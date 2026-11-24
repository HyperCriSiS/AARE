package net.prototux.aare.car

import androidx.annotation.StringRes
import net.prototux.aare.R

/**
 * What a remapped steering-wheel or dash button does (`CarSession.onKey`), one of AARE's own
 * actions. Stored by [id] in `Settings.carKeyMap`; a key with no entry there keeps its normal
 * behaviour, so an action here is never mandatory to pick.
 */
enum class CarKeyAction(val id: String, @StringRes val labelRes: Int) {
    HOME("home", R.string.setup_car_action_home),
    BACK("back", R.string.setup_car_action_back),
    ASSISTANT("assistant", R.string.setup_car_action_assistant),
    PLAY_PAUSE("play_pause", R.string.setup_car_action_play_pause),
    NEXT("next", R.string.setup_car_action_next),
    PREVIOUS("previous", R.string.setup_car_action_previous),
    OPEN_NAVIGATION("open_navigation", R.string.setup_car_action_open_navigation),
    OPEN_MEDIA("open_media", R.string.setup_car_action_open_media),
    OPEN_PHONE("open_phone", R.string.setup_car_action_open_phone),
    NOTHING("nothing", R.string.setup_car_action_nothing),
    ;

    companion object {
        fun byId(id: String): CarKeyAction? = entries.firstOrNull { it.id == id }
    }
}

/**
 * A car key's name for the "Car buttons" chooser: the car's dedicated buttons (CarSession's own
 * codes, which `KeyEvent.keyCodeToString` only gives as a number) by what they are, the rest from
 * [name] without its "KEYCODE_" prefix ("KEYCODE_MEDIA_NEXT" is "Media next").
 */
fun carKeyLabel(code: Int, name: String): String = when (code) {
    CarSession.KEYCODE_MEDIA -> "Media"
    CarSession.KEYCODE_NAVIGATION -> "Navigation"
    CarSession.KEYCODE_RADIO -> "Radio"
    CarSession.KEYCODE_TEL -> "Phone"
    CarSession.KEYCODE_TURN_CARD -> "Turn card"
    else -> name.removePrefix("KEYCODE_").replace('_', ' ').lowercase().replaceFirstChar(Char::uppercase)
}

/** The action a remapped key dispatches to, or null to keep the key's normal behaviour. */
fun resolveCarKeyAction(map: Map<Int, String>, code: Int): CarKeyAction? = map[code]?.let(CarKeyAction::byId)

/**
 * The encoding behind `Settings.carKeyMap` and `Settings.carKeysSeen` (both a code to a string:
 * an action id, or a readable key name): one `SharedPreferences` string set entry per key, as
 * "code\tvalue". Pure, so it's tested without a `SharedPreferences` of its own.
 */
object CarKeyMapCodec {
    fun encode(map: Map<Int, String>): Set<String> = map.map { (code, value) -> "$code\t$value" }.toSet()

    fun decode(entries: Set<String>): Map<Int, String> = entries.mapNotNull { e ->
        val i = e.indexOf('\t')
        if (i <= 0) null else e.substring(0, i).toIntOrNull()?.let { it to e.substring(i + 1) }
    }.toMap()
}
