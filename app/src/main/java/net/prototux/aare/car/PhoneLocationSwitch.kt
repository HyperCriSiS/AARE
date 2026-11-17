package net.prototux.aare.car

import android.content.Context
import android.location.LocationManager
import android.util.Log
import java.util.concurrent.Executors
import net.prototux.aare.launch.AppLauncher

/**
 * Turns the phone's location on when a car connects, as Android Auto asks the driver to, and off
 * when it goes, each if chosen: apps (Blitzer.de, Maps) need it, and the car's GPS only reaches them
 * through it. Needs Shizuku or root; without them it does nothing.
 */
class PhoneLocationSwitch(private val context: Context) {
    private var started = false

    /** The session runs; [on]: turn the location on. */
    fun start(on: Boolean) { started = true; if (on) switch(true) }

    /** Only after [start]: a session that ended before it ran (a failed connect) leaves the location as it is. */
    fun stop() { if (started) switch(false) }

    /** One after the other, each checking when it runs: an off, then an on right after (a quick reconnect) ends on. */
    private fun switch(on: Boolean) {
        if (!AppLauncher.available(context)) return
        worker.execute {
            val lm = context.getSystemService(LocationManager::class.java) ?: return@execute
            if (lm.isLocationEnabled != on) set(on)
        }
    }

    private fun set(on: Boolean): Boolean {
        val launcher = AppLauncher(context, "location")
        return try {
            launcher.connect() && launcher.setLocationEnabled(on)
        } finally {
            launcher.release()
        }.also { Log.i(TAG, "phone location ${if (on) "on" else "off"}: $it") }
    }

    private companion object {
        const val TAG = "AareLocation"

        /** Shared by every session's switch, so the last session's off and the next one's on keep their order. */
        val worker = Executors.newSingleThreadExecutor()
    }
}
