package net.prototux.aare.mirror

import android.app.PendingIntent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.runtime.LaunchedEffect
import net.prototux.aare.ui.car.HostedAppSurface
import net.prototux.aare.launch.AppLauncher
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import net.prototux.aare.R
import net.prototux.aare.host.CarRuntime
import net.prototux.aare.media.Placeholder

/**
 * A media app's own picture (its video), parked: [content] draws it with the app's player activity
 * (its media session's, or null), [shown] says whether it takes the art's place, [toggle] switches
 * between the two.
 */
class PhoneVideoSlot(val shown: Boolean, val toggle: () -> Unit, val content: @Composable (Modifier, PendingIntent?) -> Unit)

/** The [PhoneVideoSlot] for media apps; null while driving or where the phone screen can't show. */
val LocalPhoneVideo = compositionLocalOf<PhoneVideoSlot?> { null }

/**
 * The media app [packageName] itself, running on a display of exactly this size through the
 * privileged helper (Shizuku or root), whatever the phone shows: a video fills it, and every touch
 * goes to the app, its own controls included. The car's Back key goes back to the cover
 * ([onCover]) of the media app [appId]. Once it runs, its [player] is opened on the same display: started from the car, an
 * app otherwise shows its start screen rather than the video. While it runs here the app isn't on
 * the phone's screen; it goes back there when this closes.
 */
@Composable
internal fun PhoneVideo(runtime: CarRuntime, appId: String, packageName: String, modifier: Modifier, onCover: () -> Unit, player: PendingIntent? = null) {
    val context = LocalContext.current
    val available = remember { AppLauncher.available(context) }
    // Its own helper, apart from the phone apps' and other videos': releasing one leaves the others running.
    val launcher = remember(packageName) { AppLauncher(context, "video-$packageName") }
    // Null while starting, then whether the app runs here.
    var started by remember(packageName) { mutableStateOf<Boolean?>(null) }
    // Once per display: the media connection may only come after the app is up.
    var playerShown by remember(packageName) { mutableStateOf(false) }
    LaunchedEffect(started, player) {
        if (started == true && player != null && !playerShown) {
            playerShown = true
            withContext(Dispatchers.IO) { launcher.openPending(player) }
        }
    }
    DisposableEffect(launcher) { onDispose { launcher.release() } }
    // Back from the car (or the car screen preview) leaves the video for the cover.
    val cover by rememberUpdatedState(onCover)
    DisposableEffect(appId) {
        val back: () -> Boolean = { cover(); true }
        runtime.setScreenBack(appId, back)
        onDispose { runtime.clearScreenBack(appId, back) }
    }
    if (!available) {
        Placeholder(stringResource(R.string.standalone_need_root), loading = false, icon = Icons.Rounded.PhoneAndroid, modifier = modifier.background(Color.Black))
        return
    }
    Box(modifier.background(Color.Black)) {
        HostedAppSurface(launcher, packageName, runtime.densityDpi, Modifier.fillMaxSize(), tablet = true) { started = it }
        when (started) {
            null -> Placeholder(stringResource(R.string.standalone_starting), loading = true)
            false -> Placeholder(stringResource(R.string.standalone_failed), loading = false, icon = Icons.Rounded.PhoneAndroid)
            true -> Unit
        }
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
