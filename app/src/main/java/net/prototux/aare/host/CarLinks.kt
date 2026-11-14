package net.prototux.aare.host

/**
 * Links that act on the car, for assistants that can only write text (Claude, ChatGPT): the reply
 * puts `https://hypercrisis.github.io/AARE/go/car_navigate?destination=Berlin` behind an emoji,
 * the voice reading it aloud skips the address, and a tap on the emoji hands it to AARE
 * (CarLinkActivity), which runs the car tool of the same name.
 */
object CarLinks {
    const val HOST = "hypercrisis.github.io"
    const val PATH = "/AARE/go/"

    /**
     * Only tools the driver sees and can undo. Not car_open: its loose name match reaches the phone
     * screen mirror, and anything sent on the driver's behalf needs the assistant's confirmation.
     */
    private val TOOLS = setOf("car_navigate", "car_media", "car_display")

    /**
     * The tool a link to [host] and [path] asks for, with its [query] as arguments, or null for
     * anything else. Only the car's voice assistant ([assistant], see Assistants.authorized) may send
     * one: [caller] is the app that opened the link, as Android reports it.
     */
    internal fun parse(host: String?, path: String, query: Map<String, String>, caller: String?, assistant: String?): Pair<String, Map<String, String>>? {
        if (host != HOST || !path.startsWith(PATH) || caller == null || caller != assistant) return null
        val tool = path.removePrefix(PATH).trimEnd('/')
        return if (tool in TOOLS) tool to query else null
    }
}
