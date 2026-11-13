package net.prototux.aare.mirror

import net.prototux.aare.ui.car.HostedAppSurface
import net.prototux.aare.launch.AppLauncher
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.rounded.FullscreenExit
import androidx.compose.material.icons.rounded.Fullscreen
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import net.prototux.aare.R
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.media.Placeholder
import net.prototux.aare.ui.car.LocalFullScreen

/**
 * A media app's own picture (its video), parked: [content] draws it, [shown] says whether it takes
 * the art's place, [toggle] switches between the two.
 */
class PhoneVideoSlot(val shown: Boolean, val toggle: () -> Unit, val content: @Composable (Modifier) -> Unit)

/** The [PhoneVideoSlot] for media apps; null while driving or where the phone screen can't show. */
val LocalPhoneVideo = compositionLocalOf<PhoneVideoSlot?> { null }

/**
 * The media app [packageName] itself, running on a display of exactly this size through the
 * privileged helper (Shizuku or root), whatever the phone shows: a video fills it, and touches go
 * to the app. The corner button shows it over the whole car screen, rail included, and back. While
 * it runs here the app isn't on the phone's screen; it goes back there when this closes.
 */
@Composable
internal fun PhoneVideo(runtime: CarRuntime, packageName: String, modifier: Modifier) {
    val context = LocalContext.current
    val full = LocalFullScreen.current
    val available = remember { AppLauncher.available(context) }
    // Its own helper process, apart from the phone apps': releasing one leaves the other running.
    val launcher = remember(packageName) { AppLauncher(context, "video") }
    // Null while starting, then whether the app runs here.
    var started by remember(packageName) { mutableStateOf<Boolean?>(null) }
    // Gone (driving, another screen): so are its full-screen copy and its display.
    DisposableEffect(launcher) { onDispose { full?.value = null; launcher.release() } }
    Box(modifier.background(Color.Black)) {
        if (!available) {
            Placeholder(stringResource(R.string.standalone_need_root), loading = false, icon = Icons.Rounded.PhoneAndroid)
            return@Box
        }
        // Full screen, the one display moves onto the copy over everything.
        if (full?.value == null) HostedAppSurface(launcher, packageName, runtime.densityDpi, Modifier.fillMaxSize(), tablet = true) { started = it }
        when (started) {
            null -> Placeholder(stringResource(R.string.standalone_starting), loading = true)
            false -> Placeholder(stringResource(R.string.standalone_failed), loading = false, icon = Icons.Rounded.PhoneAndroid)
            true -> Unit
        }
        if (full != null) {
            CornerButton(Icons.Rounded.Fullscreen, stringResource(R.string.video_full_screen), Modifier.align(Alignment.TopEnd)) {
                full.value = {
                    Box(Modifier.fillMaxSize()) {
                        HostedAppSurface(launcher, packageName, runtime.densityDpi, Modifier.fillMaxSize(), tablet = true)
                        CornerButton(Icons.Rounded.FullscreenExit, stringResource(R.string.video_exit_full_screen), Modifier.align(Alignment.TopEnd)) { full.value = null }
                    }
                }
            }
        }
    }
}

/** A small round button over a video's corner. */
@Composable
private fun CornerButton(icon: ImageVector, label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.padding(10.dp).size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.5f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, label, tint = Color.White) }
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
