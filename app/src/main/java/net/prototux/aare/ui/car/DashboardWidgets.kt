package net.prototux.aare.ui.car

import android.text.format.DateFormat
import android.text.format.DateUtils
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.Umbrella
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.car.NextEvent
import net.prototux.aare.car.Weather
import net.prototux.aare.car.startsToday

/** WMO weather codes (Open-Meteo) to an icon. */
private fun weatherIcon(w: Weather): ImageVector = when (w.code) {
    0, 1 -> if (w.day) Icons.Rounded.WbSunny else Icons.Rounded.NightsStay
    in 51..67, in 80..82 -> Icons.Rounded.Umbrella
    in 71..77, 85, 86 -> Icons.Rounded.AcUnit
    in 95..99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.Cloud
}

/** The temperature and sky, next to the clock like Android Auto's. */
@Composable
internal fun WeatherChip(weather: Weather?) {
    val w = weather ?: return
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Icon(weatherIcon(w), null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(18.dp))
        Text("${w.temperature}${w.unit}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

/** The weather as a dashboard card: the sky and the temperature, big. */
@Composable
internal fun WeatherCard(weather: Weather) {
    Row(Modifier.fillMaxSize().padding(24.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Icon(weatherIcon(weather), null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(72.dp))
        Spacer(Modifier.width(16.dp))
        Text("${weather.temperature}${weather.unit}", style = MaterialTheme.typography.displaySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

/**
 * The next calendar events: the first large, with a shortcut to navigate to its place, the ones
 * after it as lines underneath, as many as the card has room for. One event alone stays centered.
 */
@Composable
internal fun CalendarCard(events: List<NextEvent>, onNavigate: (String) -> Unit, modifier: Modifier) {
    val event = events.firstOrNull() ?: return
    BoxWithConstraints(modifier.padding(24.dp)) {
        // About 170 dp for the first event with its place, 48 dp for each line after it.
        val more = events.drop(1).take(((maxHeight - 170.dp) / 48.dp).toInt().coerceAtLeast(0))
        Column(Modifier.fillMaxSize(), verticalArrangement = if (more.isEmpty()) Arrangement.Center else Arrangement.Top) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Event, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Text(eventTime(event), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(6.dp))
            Text(event.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (event.location.isNotBlank()) {
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.secondaryContainer)
                        .clickable { onNavigate(event.location) }.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Rounded.Navigation, null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(event.location, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSecondaryContainer,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            more.forEach { e ->
                Spacer(Modifier.height(20.dp))
                Text(eventTime(e), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                Text(e.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

/** When [e] starts: the time today, "Today" for an all-day event today, and the weekday for tomorrow. */
@Composable
private fun eventTime(e: NextEvent): String {
    val context = LocalContext.current
    val today = startsToday(e)
    return when {
        e.allDay && today -> stringResource(R.string.dash_event_today)
        e.allDay -> DateUtils.formatDateTime(context, e.startMs, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_UTC)
        today -> DateFormat.getTimeFormat(context).format(e.startMs)
        else -> DateUtils.formatDateTime(context, e.startMs, DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY or DateUtils.FORMAT_SHOW_TIME)
    }
}

/** Built-in wallpapers: soft gradients that keep the cards readable by day and by night. */
private val WALLPAPERS = mapOf(
    "dusk" to listOf(Color(0xFF2B1B3D), Color(0xFF6B3A5B), Color(0xFFC2735A)),
    "forest" to listOf(Color(0xFF0F2A1D), Color(0xFF1F4D36), Color(0xFF4F7A4A)),
    "ocean" to listOf(Color(0xFF071E3D), Color(0xFF1F4E79), Color(0xFF3E8DB8)),
)
val WALLPAPER_NAMES = WALLPAPERS.keys.toList()

/** The dashboard's wallpaper, filling [modifier]; nothing for "none". */
@Composable
internal fun Wallpaper(ui: CarUiState, modifier: Modifier) {
    val image = ui.wallpaperImage
    when {
        ui.wallpaper == net.prototux.aare.Settings.WALLPAPER_CUSTOM && image != null ->
            Image(image.asImageBitmap(), null, modifier, contentScale = ContentScale.Crop)
        else -> WALLPAPERS[ui.wallpaper]?.let { colors -> Box(modifier.background(Brush.linearGradient(colors))) }
    }
}

/** The latest message, for the third card of very wide dashboards. */
@Composable
internal fun LatestNotification(n: CarNotice, driving: Boolean) {
    Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.Center) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Notifications, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.dash_latest_label), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(Modifier.height(6.dp))
        Text(n.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(if (driving) stringResource(R.string.notif_new_message_placeholder) else n.text, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}
