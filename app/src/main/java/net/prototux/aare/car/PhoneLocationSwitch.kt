package net.prototux.aare.car

import android.content.Context
import android.location.LocationManager
import android.util.Log
import net.prototux.aare.launch.AppLauncher

/**
 * Turns the phone's location on while a car is connected, as Android Auto asks the driver to, and
 * back off at the end if it was off before: apps (Blitzer.de, Maps) need it, and the car's GPS only
 * reaches them through it. Needs Shizuku or root; without them it does nothing. Main thread.
 */
class PhoneLocationSwitch(private val context: Context) {
    /** It was off and this turned it on: put back off at [stop]. */
    @Volatile private var turnedOn = false

    fun start() {
        val lm = context.getSystemService(LocationManager::class.java) ?: return
        if (lm.isLocationEnabled || !AppLauncher.available(context)) return
        Thread {
            if (set(true)) turnedOn = true
        }.start()
    }

    fun stop() {
        if (!turnedOn) return
        turnedOn = false
        Thread { set(false) }.start()
    }

    private fun set(on: Boolean): Boolean {
        val launcher = AppLauncher(context, "location")
        return try {
            launcher.connect() && launcher.setLocationEnabled(on)
        } finally {
            launcher.release()
        }.also { Log.i(TAG, "phone location ${if (on) "on" else "back off"}: $it") }
    }

    private companion object {
        const val TAG = "AareLocation"
    }
}
