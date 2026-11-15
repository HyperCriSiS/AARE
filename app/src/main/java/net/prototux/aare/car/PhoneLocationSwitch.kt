package net.prototux.aare.car

import android.content.Context
import android.location.LocationManager
import android.util.Log
import net.prototux.aare.launch.AppLauncher

/**
 * Turns the phone's location on when a car connects, as Android Auto asks the driver to, and off
 * when it goes, each if chosen: apps (Blitzer.de, Maps) need it, and the car's GPS only reaches them
 * through it. Needs Shizuku or root; without them it does nothing.
 */
class PhoneLocationSwitch(private val context: Context) {
    fun start() = switch(true)

    fun stop() = switch(false)

    private fun switch(on: Boolean) {
        val lm = context.getSystemService(LocationManager::class.java) ?: return
        if (lm.isLocationEnabled == on || !AppLauncher.available(context)) return
        Thread { set(on) }.start()
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
    }
}
