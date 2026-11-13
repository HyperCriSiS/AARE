package net.prototux.aare.mirror

import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import net.prototux.aare.R
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.media.Placeholder
import net.prototux.aare.ui.car.LocalFullScreen

/**
 * The phone's screen as a media app's picture, parked: [content] draws it, [shown] says whether it
 * takes the art's place, [toggle] switches between the two.
 */
class PhoneVideoSlot(val shown: Boolean, val toggle: () -> Unit, val content: @Composable (Modifier) -> Unit)

/** The [PhoneVideoSlot] for media apps; null while driving or where the phone screen can't show. */
val LocalPhoneVideo = compositionLocalOf<PhoneVideoSlot?> { null }

/**
 * The phone screen, live, for a video playing there. A double tap shows it over the whole car
 * screen, rail included; a tap there brings the rail back.
 */
@Composable
internal fun PhoneVideo(runtime: CarRuntime, modifier: Modifier) {
    val full = LocalFullScreen.current
    val locked = rememberKeyguardLocked()
    var mirroring by remember { mutableStateOf(true) }
    // Gone (driving, another screen): so is its full-screen copy.
    DisposableEffect(full) { onDispose { full?.value = null } }
    Box(modifier.background(Color.Black)) {
        when {
            locked -> Placeholder(stringResource(R.string.mirror_locked_placeholder), loading = false, icon = Icons.Rounded.Lock)
            !mirroring -> Placeholder(stringResource(R.string.mirror_need_car_audio), loading = false, icon = Icons.Rounded.PhoneAndroid)
            // Shown full screen: one mirror at a time.
            full?.value == null -> MirrorSurface(runtime, Modifier.fillMaxSize()) { mirroring = it }
        }
        Box(Modifier.matchParentSize().pointerInput(full) {
            detectTapGestures(onDoubleTap = {
                full?.value = {
                    Box(Modifier.fillMaxSize()) {
                        MirrorSurface(runtime, Modifier.fillMaxSize())
                        Box(Modifier.matchParentSize().pointerInput(Unit) { detectTapGestures { full.value = null } })
                    }
                }
            })
        })
    }
}

/** The phone screen drawn into a texture. [onMirroring] says whether it could start (a consent or a helper). */
@Composable
internal fun MirrorSurface(runtime: CarRuntime, modifier: Modifier, onMirroring: (Boolean) -> Unit = {}) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            var surface: Surface? = null
            TextureView(ctx).apply {
                surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                        surface = Surface(st)
                        onMirroring(PhoneMirror.attach(ctx, runtime.projection(), surface!!, w, h, runtime.densityDpi))
                    }
                    override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) {
                        surface?.let { onMirroring(PhoneMirror.attach(ctx, runtime.projection(), it, w, h, runtime.densityDpi)) }
                    }
                    override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                        PhoneMirror.detach()
                        surface?.release(); surface = null
                        return true
                    }
                    override fun onSurfaceTextureUpdated(st: SurfaceTexture) = Unit
                }
            }
        },
    )
}
