package net.prototux.aare.host

import android.app.Activity
import android.os.Bundle
import android.util.Log
import net.prototux.aare.Settings
import org.json.JSONObject

/** Runs a car link (see CarLinks) tapped in the assistant's reply, without showing anything itself. */
class CarLinkActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = intent?.data
        // The app that opened the link; Android fills it in for links opened from another app.
        val caller = referrer?.takeIf { it.scheme == "android-app" }?.host
        val link = uri?.let { u ->
            CarLinks.parse(u.host, u.path.orEmpty(), u.queryParameterNames.associateWith { u.getQueryParameter(it).orEmpty() },
                caller, Assistants.authorized(this, Settings(this).assistantPackage))
        }
        val rt = CarRuntime.active
        when {
            link == null -> Log.w(TAG, "not a car link from the assistant ($caller): $uri")
            rt == null -> Log.w(TAG, "the car isn't connected: $uri")
            // Anyone may send a link: a failing tool must not take the car screen down with it.
            else -> Log.i(TAG, "${link.first}: ${runCatching { CarTools.run(rt, link.first, JSONObject(link.second)) }.getOrElse { it.toString() }}")
        }
        finish()
    }

    private companion object {
        const val TAG = "AareCarLink"
    }
}
