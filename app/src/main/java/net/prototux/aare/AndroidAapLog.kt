package net.prototux.aare

import android.util.Log
import net.prototux.aare.aap.AapLog

/** Protocol logs go to logcat under "AareAap". */
object AndroidAapLog : AapLog {
    override fun d(msg: String) { Log.d("AareAap", msg) }
    override fun i(msg: String) { Log.i("AareAap", msg) }
    override fun w(msg: String, t: Throwable?) { Log.w("AareAap", msg, t) }
    override fun e(msg: String, t: Throwable?) { Log.e("AareAap", msg, t) }
}

/**
 * The car session's protocol log. Always to logcat (tag "AareAap"); when [verbose] is on, every
 * line (the protocol trace included) also goes to the in-app [DiagLog], so a reporter can
 * capture the protocol trace and export it without adb. Off by default (Settings.verboseProtocolLog).
 */
object SessionAapLog : AapLog {
    @Volatile var verbose = false
    private fun diag(msg: String) { if (verbose) DiagLog.add(msg) }
    private fun with(msg: String, t: Throwable?) = if (t != null) "$msg: ${t.message ?: t}" else msg
    /** A failure with where it came from: the first lines of its stack trace. */
    private fun withStack(msg: String, t: Throwable?) =
        if (t != null) "$msg: ${t.stackTraceToString().lineSequence().take(8).joinToString("\n    ")}" else msg
    override fun d(msg: String) { Log.d("AareAap", msg); diag(msg) }
    override fun i(msg: String) { Log.i("AareAap", msg); diag(msg) }
    // The normal log stays short; with the verbose log on, warnings come rate limited, errors with their stack.
    override fun w(msg: String, t: Throwable?) { Log.w("AareAap", msg, t); if (verbose) limited(msg) { with(msg, t) } }
    override fun e(msg: String, t: Throwable?) { Log.e("AareAap", msg, t); if (verbose) limited(msg) { withStack(msg, t) } }

    /** Times each warning was seen: one per message would flood the log and push out the session start. */
    private val seen = HashMap<String, Int>()

    /** The first [WarningLimit.FIRST] of a warning, then one in [WarningLimit.EVERY] with its count. */
    private fun limited(msg: String, line: () -> String) {
        val n = synchronized(seen) {
            if (seen.size > 500) seen.clear()
            (seen[msg] ?: 0).plus(1).also { seen[msg] = it }
        }
        if (WarningLimit.shows(n)) DiagLog.add(if (n > WarningLimit.FIRST) "${line()} ($n so far)" else line())
    }
}

/** How often a repeated warning reaches the connection log. */
object WarningLimit {
    const val FIRST = 3
    const val EVERY = 100
    fun shows(n: Int) = n <= FIRST || n % EVERY == 0
}

/** Also writes to the [DiagLog] the user sees in the app (not the debug lines). */
object DiagAapLog : AapLog {
    override fun d(msg: String) { Log.d("AareAap", msg) }
    override fun i(msg: String) { DiagLog.add(msg) }
    override fun w(msg: String, t: Throwable?) { Log.w("AareAap", msg, t); DiagLog.add(if (t != null) "$msg: ${t.message ?: t}" else msg) }
    override fun e(msg: String, t: Throwable?) { Log.e("AareAap", msg, t); DiagLog.add(if (t != null) "$msg: ${t.message ?: t}" else msg) }
}
