package net.prototux.aare.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface

/**
 * A video codec the phone can feed a head unit, mapped to its MediaCodec MIME type and the
 * profile/level we ask for. We only ever use codecs the phone can encode in *hardware* (see
 * [VideoEncoder.hardwareEncodes]); the level table is tuned for H.264, where it is well tested,
 * and left to the encoder for the others (the config fallback drops it if it is rejected anyway).
 */
enum class VideoCodecKind(val label: String, val mime: String, val profile: Int?) {
    H264("H.264", MediaFormat.MIMETYPE_VIDEO_AVC, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline),
    H265("H.265", MediaFormat.MIMETYPE_VIDEO_HEVC, MediaCodecInfo.CodecProfileLevel.HEVCProfileMain),
    VP9("VP9", MediaFormat.MIMETYPE_VIDEO_VP9, null),
    AV1("AV1", MediaFormat.MIMETYPE_VIDEO_AV1, null);

    /** H.264 pins the level to the macroblock rate; the others let the encoder choose. */
    fun levelFor(w: Int, h: Int, fps: Int): Int? = if (this == H264) h264LevelFor(w, h, fps) else null
}

/**
 * Hardware video encoder fed by a Surface (a virtual display renders into it); H.264 by default,
 * or another [VideoCodecKind] the head unit asked for and the phone can encode in hardware.
 *
 * Head units decode Annex-B access units, the same thing the official app sends; H.264 uses
 * Baseline/Constrained Baseline. Output buffers are copied into pooled arrays with [headroom] free
 * bytes in front, so the session can prepend its message header without another copy.
 */
class VideoEncoder(
    val width: Int,
    val height: Int,
    val fps: Int,
    private val kind: VideoCodecKind = VideoCodecKind.H264,
    /** The starting bitrate; [setBitrate] changes it while running. */
    val bitrate: Int = defaultBitrate(width, height, fps),
    private val headroom: Int = 10,
    /**
     * Constant bitrate instead of variable, if the encoder supports it: every frame stays near the
     * bitrate, where variable bitrate sends very large frames when the whole picture changes (a
     * mirrored phone screen, a video), which some head units drop the link over. Costs sharpness on
     * busy pictures.
     */
    private val constantBitrate: Boolean = false,
    /** Encoder changes for a head unit that takes the stream but shows nothing (see [EncoderTweaks]). */
    private val tweaks: EncoderTweaks = EncoderTweaks(),
    private val sink: Sink,
) {
    interface Sink {
        /** SPS/PPS. Sent once before the first frame, and again whenever the encoder restarts. */
        fun onCodecConfig(data: ByteArray, offset: Int, length: Int)

        /** One access unit. [data] has [offset] bytes of headroom; give it back with [recycle]. */
        fun onFrame(data: ByteArray, offset: Int, length: Int, ptsUs: Long, keyFrame: Boolean)

        fun onError(e: Exception)
    }

    private var codec: MediaCodec? = null
    private var thread: HandlerThread? = null
    lateinit var inputSurface: Surface
        private set

    private val pool = ArrayDeque<ByteArray>()

    fun start() {
        val t = HandlerThread("aare-video-enc", android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY).also { it.start() }
        thread = t
        var bitrateMode = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_VBR
        // Built afresh for each attempt (MediaFormat.removeKey is Android 10+).
        fun format(dropped: Set<String>) = MediaFormat.createVideoFormat(kind.mime, width, height).apply {
            fun int(key: String, value: Int) { if (key !in dropped) setInteger(key, value) }
            setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            // Long GOP: the transport is reliable, and we ask for key frames when they're needed.
            setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, if (tweaks.shortGop) 1 else 2)
            val h264 = kind == VideoCodecKind.H264
            (if (h264 && tweaks.constrainedBaseline) MediaCodecInfo.CodecProfileLevel.AVCProfileConstrainedBaseline else kind.profile)
                ?.let { int(MediaFormat.KEY_PROFILE, it) }
            (if (h264 && tweaks.level30) MediaCodecInfo.CodecProfileLevel.AVCLevel3 else kind.levelFor(width, height, fps))
                ?.let { int(MediaFormat.KEY_LEVEL, it) }
            int(MediaFormat.KEY_BITRATE_MODE, bitrateMode)
            // Keep frames flowing on a static screen so the head unit's decoder never stalls.
            if (MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER !in dropped) setLong(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER, 1_000_000L / 5)
            int(MediaFormat.KEY_PRIORITY, 0) // realtime
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) int(MediaFormat.KEY_LATENCY, 1)
            int(MediaFormat.KEY_MAX_B_FRAMES, 0)
            // SPS/PPS before every key frame, so a head unit that resets its decoder can resync.
            if (!tweaks.noRepeatedHeaders) int(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES, 1)
        }
        // Until the codec runs, a failure must release what was made so far: the caller never
        // gets an encoder to stop, and a leaked codec holds one of the few hardware instances.
        var c: MediaCodec? = null
        var surface: Surface? = null
        try {
            var format = format(emptySet())
            val name = MediaCodecList(MediaCodecList.REGULAR_CODECS).findEncoderForFormat(format)
            val software = if (tweaks.softwareEncoder) softwareEncoder(kind.mime) else null
            c = (software ?: name)?.let { MediaCodec.createByCodecName(it) } ?: MediaCodec.createEncoderByType(kind.mime)
            if (constantBitrate) {
                val cbr = MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR
                val supported = runCatching { c.codecInfo.getCapabilitiesForType(kind.mime).encoderCapabilities?.isBitrateModeSupported(cbr) == true }.getOrDefault(false)
                if (supported) bitrateMode = cbr
                Log.i(TAG, "constant bitrate ${if (supported) "on" else "not supported by ${c.name}, variable kept"}")
            }
            // Encoders differ in which tuning keys they accept: drop them step by step until one works.
            val fallbacks = listOf(
                emptyList(),
                listOf(MediaFormat.KEY_PREPEND_HEADER_TO_SYNC_FRAMES),
                listOf(MediaFormat.KEY_LATENCY, MediaFormat.KEY_PRIORITY, MediaFormat.KEY_MAX_B_FRAMES),
                listOf(MediaFormat.KEY_PROFILE, MediaFormat.KEY_LEVEL, MediaFormat.KEY_BITRATE_MODE),
                listOf(MediaFormat.KEY_REPEAT_PREVIOUS_FRAME_AFTER),
            )
            var configured = false
            var lastError: Exception? = null
            val dropped = HashSet<String>()
            for (drop in fallbacks) {
                dropped += drop
                format = format(dropped)
                try {
                    // Set before every configure(): on Android 8.x the reset() after a rejected format
                    // also drops the callback, the codec then runs in sync mode, no output ever reaches
                    // us and the encoder stalls once its output buffers are full (black car screen).
                    c.setCallback(Callback(), Handler(t.looper))
                    c.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                    configured = true
                    break
                } catch (e: Exception) {
                    lastError = e
                    Log.w(TAG, "encoder ${c.name} rejected $format: ${e.message}")
                    runCatching { c.reset() }
                }
            }
            if (!configured) throw IllegalStateException("no usable ${kind.label} encoder configuration for ${width}x$height@$fps", lastError)
            surface = c.createInputSurface()
            c.start()
            inputSurface = surface
            codec = c
            Log.i(TAG, "encoder ${c.name} ${width}x$height@$fps ${bitrate / 1000} kb/s")
        } catch (e: Throwable) {
            surface?.let { runCatching { it.release() } }
            c?.let { runCatching { it.release() } }
            t.quitSafely()
            thread = null
            throw e
        }
    }

    /** Asks for an IDR frame now (after focus changes, or when the head unit asks). */
    fun requestKeyFrame() {
        runCatching { codec?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0) }) }
    }

    /** Changes the target bitrate of the running stream (adaptive bitrate, see [BitrateController]). */
    fun setBitrate(bps: Int) {
        runCatching { codec?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bps) }) }
            .onFailure { Log.w(TAG, "can't change the bitrate to $bps: ${it.message}") }
    }

    /** Pauses or resumes output (no frames while the head unit shows its own UI). */
    fun setSuspended(suspended: Boolean) {
        runCatching { codec?.setParameters(Bundle().apply { putInt(MediaCodec.PARAMETER_KEY_SUSPEND, if (suspended) 1 else 0) }) }
        if (!suspended) requestKeyFrame()
    }

    fun recycle(buffer: ByteArray) {
        synchronized(pool) { if (pool.size < 8) pool.addLast(buffer) }
    }

    private fun obtain(size: Int): ByteArray {
        synchronized(pool) {
            val it = pool.iterator()
            while (it.hasNext()) {
                val b = it.next()
                if (b.size >= size) { it.remove(); return b }
            }
        }
        // round up so buffers get reused across frames of similar size
        return ByteArray(((size + 16383) / 16384) * 16384)
    }

    fun stop() {
        val c = codec ?: return
        codec = null
        runCatching { c.stop() }
        runCatching { c.release() }
        runCatching { inputSurface.release() }
        thread?.quitSafely()
        thread = null
    }

    private inner class Callback : MediaCodec.Callback() {
        override fun onInputBufferAvailable(codec: MediaCodec, index: Int) = Unit // Surface input

        override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
            try {
                val buf = codec.getOutputBuffer(index)
                if (buf != null && info.size > 0) {
                    buf.position(info.offset).limit(info.offset + info.size)
                    if (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                        val cfg = ByteArray(info.size)
                        buf.get(cfg)
                        sink.onCodecConfig(cfg, 0, cfg.size)
                    } else {
                        val out = obtain(headroom + info.size)
                        buf.get(out, headroom, info.size)
                        sink.onFrame(out, headroom, info.size, info.presentationTimeUs,
                            info.flags and MediaCodec.BUFFER_FLAG_KEY_FRAME != 0)
                    }
                }
                codec.releaseOutputBuffer(index, false)
            } catch (e: IllegalStateException) {
                // released while a callback was in flight
            }
        }

        override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
            Log.e(TAG, "encoder error", e)
            sink.onError(e)
        }

        override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
            Log.i(TAG, "encoder output format $format")
        }
    }

    companion object {
        private const val TAG = "AareVideoEnc"

        fun defaultBitrate(w: Int, h: Int, fps: Int): Int {
            // ~0.12 bits per pixel per frame: crisp UI text without saturating USB 2 / Wi-Fi,
            // capped at the bitrates head units are specified for (more makes some decoders stall).
            val bpp = 0.12
            val short = minOf(w, h)
            val max = when {
                short <= 480 -> 4_000_000
                short <= 720 -> 6_000_000
                else -> 8_000_000
            }
            return (w * h * fps * bpp).toInt().coerceIn(2_000_000, max)
        }

        /** Whether the phone has a hardware encoder for [kind] (we never software-encode video). */
        fun hardwareEncodes(kind: VideoCodecKind): Boolean {
            val probe = MediaFormat.createVideoFormat(kind.mime, 1280, 720).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
            }
            val list = MediaCodecList(MediaCodecList.REGULAR_CODECS)
            val name = runCatching { list.findEncoderForFormat(probe) }.getOrNull() ?: return false
            // isHardwareAccelerated is API 29+; before that, trust only H.264 (universally hardware).
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return kind == VideoCodecKind.H264
            return list.codecInfos.firstOrNull { it.name == name }?.isHardwareAccelerated == true
        }
    }
}

/**
 * Encoder changes to try, one variant at a time, on a head unit that acks every frame but never
 * shows the picture: a lower level, Constrained Baseline, no SPS/PPS repeated before key frames
 * (only the codec config), Android's software encoder instead of the phone's, or all of them.
 */
data class EncoderTweaks(
    val level30: Boolean = false,
    val constrainedBaseline: Boolean = false,
    val noRepeatedHeaders: Boolean = false,
    val softwareEncoder: Boolean = false,
    val shortGop: Boolean = false,
) {
    companion object {
        val VARIANTS = listOf("", "level30", "constrained", "headers", "software", "all")

        fun of(variant: String) = when (variant) {
            "level30" -> EncoderTweaks(level30 = true)
            "constrained" -> EncoderTweaks(constrainedBaseline = true)
            "headers" -> EncoderTweaks(noRepeatedHeaders = true)
            "software" -> EncoderTweaks(softwareEncoder = true)
            "all" -> EncoderTweaks(level30 = true, constrainedBaseline = true, softwareEncoder = true, shortGop = true)
            else -> EncoderTweaks()
        }
    }
}

/** Android's own software encoder for [mime], or null if the phone has none that takes a Surface. */
private fun softwareEncoder(mime: String): String? = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos
    .filter { it.isEncoder && it.supportedTypes.any { t -> t.equals(mime, ignoreCase = true) } }
    .filter { (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && it.isSoftwareOnly) || it.name.startsWith("c2.android.") || it.name.startsWith("OMX.google.") }
    .firstOrNull { info ->
        runCatching { MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface in info.getCapabilitiesForType(mime).colorFormats }.getOrDefault(false)
    }?.name

/** The lowest H.264 level whose macroblock rate (and frame size) covers the stream. */
private fun h264LevelFor(w: Int, h: Int, fps: Int): Int {
    val frameMbs = ((w + 15) / 16) * ((h + 15) / 16)
    val mbps = frameMbs * fps
    return when {
        frameMbs > 8_704 -> MediaCodecInfo.CodecProfileLevel.AVCLevel51 // beyond 1080p
        mbps <= 108_000 -> MediaCodecInfo.CodecProfileLevel.AVCLevel31 // 800x480@60, 720p30
        mbps <= 216_000 -> MediaCodecInfo.CodecProfileLevel.AVCLevel32 // 720p60
        mbps <= 245_760 -> MediaCodecInfo.CodecProfileLevel.AVCLevel4 // 1080p30
        else -> MediaCodecInfo.CodecProfileLevel.AVCLevel42 // 1080p60
    }
}
