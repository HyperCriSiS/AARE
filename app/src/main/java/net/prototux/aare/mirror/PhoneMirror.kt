package net.prototux.aare.mirror

import android.content.Context
import android.graphics.Point
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.projection.MediaProjection
import android.util.Log
import android.view.Display
import android.view.Surface
import net.prototux.aare.launch.AppLauncher
import java.util.concurrent.Executors

/**
 * The phone's own screen, mirrored into a surface of the car screen through the screen-capture
 * consent AARE already holds for car audio, or else through the privileged helper (Shizuku or
 * root) that also hosts phone apps.
 *
 * Android 14 lets one consent create a single virtual display, so the display is created once
 * per consent and only its surface and size change afterwards. Main thread only.
 */
object PhoneMirror {
    private const val TAG = "AareMirror"
    private var display: VirtualDisplay? = null
    private var owner: MediaProjection? = null

    /** Mirrors the phone screen into [surface] ([width]×[height] pixels). False without a consent or a helper. */
    fun attach(context: Context, projection: MediaProjection?, surface: Surface, width: Int, height: Int, dpi: Int): Boolean {
        if (projection == null) return Helper.attach(context, surface, width, height, dpi)
        if (owner !== projection) release()
        val d = display
        if (d != null) {
            d.resize(width, height, dpi)
            d.surface = surface
            return true
        }
        return runCatching {
            display = projection.createVirtualDisplay("aare-phone-screen", width, height, dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, surface, null, null)
            owner = projection
        }.onFailure { Log.w(TAG, "can't mirror the phone screen", it) }.isSuccess
    }

    /** Stops drawing (the display stays, for the next [attach]). */
    fun detach() {
        display?.surface = null
        Helper.detach()
    }

    fun release() {
        runCatching { display?.release() }
        display = null
        owner = null
        Helper.release()
    }

    /** Without a consent: the privileged helper mirrors the screen like scrcpy. Its binder calls block, so they run on [worker]. */
    private object Helper {
        private val worker = Executors.newSingleThreadExecutor()
        private var launcher: AppLauncher? = null // worker only

        fun attach(context: Context, surface: Surface, width: Int, height: Int, dpi: Int): Boolean {
            if (!AppLauncher.available(context)) return false
            val app = context.applicationContext
            worker.execute {
                val l = launcher ?: AppLauncher(app, "mirror").let { if (it.connect()) it.also { launcher = it } else { it.release(); null } }
                if (l?.mirror(surface, width, height, dpi) != true) Log.w(TAG, "the helper can't mirror the phone screen")
            }
            return true
        }

        fun detach() = worker.execute { launcher?.stopMirror() }

        fun release() = worker.execute { launcher?.release(); launcher = null }
    }

    /** The phone screen's current size in pixels, rotation included. */
    fun phoneScreenSize(context: Context): Point {
        val d = context.getSystemService(DisplayManager::class.java).getDisplay(Display.DEFAULT_DISPLAY)
        @Suppress("DEPRECATION")
        return Point().also { d.getRealSize(it) }
    }
}
