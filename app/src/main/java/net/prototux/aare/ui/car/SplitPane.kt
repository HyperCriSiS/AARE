package net.prototux.aare.ui.car

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import net.prototux.aare.R
import net.prototux.aare.ui.theme.LocalCarDimens
import kotlin.math.roundToInt

/**
 * Two apps side by side. Dragging the divider resizes them; let go within [SPLIT_SNAP] of an edge,
 * the pane on that side closes and the other app takes the whole screen. A pane about to close
 * dims while it is dragged there. Resizing gives the apps a new surface size as the divider moves.
 */
@Composable
internal fun SplitPane(
    split: CarScreen.Split,
    state: CarUiState,
    actions: CarActions,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    modifier: Modifier,
) {
    BoxWithConstraints(modifier) {
        val width = constraints.maxWidth.toFloat()
        // Never quite zero while dragging: a weight must stay above 0, and the pane must not vanish
        // before the finger is lifted.
        val f = state.splitFraction.coerceIn(0.02f, 0.98f)
        Row(Modifier.fillMaxSize()) {
            Pane(split.left, true, f < SPLIT_SNAP, state, appContent, Modifier.weight(f))
            Divider(Modifier.fillMaxHeight().pointerInput(width) {
                detectHorizontalDragGestures(
                    onDragEnd = { actions.releaseDivider(state.splitFraction) },
                    onDragCancel = { actions.releaseDivider(state.splitFraction) },
                ) { change, dx ->
                    change.consume()
                    state.splitFraction = (state.splitFraction + dx / width).coerceIn(0.02f, 0.98f)
                }
            })
            Pane(split.right, false, f > 1 - SPLIT_SNAP, state, appContent, Modifier.weight(1 - f))
        }
    }
}

@Composable
private fun Pane(
    appId: String,
    left: Boolean,
    closing: Boolean,
    state: CarUiState,
    appContent: @Composable (CarAppEntry, Modifier) -> Unit,
    modifier: Modifier,
) {
    Box(
        modifier.fillMaxHeight().clipToBounds()
            // A rotary knob or D-pad moving into the pane counts as touching it.
            .onFocusChanged { if (it.hasFocus) state.splitActiveLeft = left }
            // Watches touches without taking them: the car's Back key goes to the pane touched last.
            .pointerInput(left) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        state.splitActiveLeft = left
                    }
                }
            },
    ) {
        state.app(appId)?.let { appContent(it, Modifier.fillMaxSize()) }
        // A scrim rather than alpha: alpha doesn't dim a navigation app's map surface.
        if (closing) Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.6f)))
    }
}

/** The handle between the panes: wide enough to grab with a finger, drawn as a short bar. */
@Composable
private fun Divider(modifier: Modifier) {
    val label = stringResource(R.string.split_divider)
    Box(modifier.width(24.dp).semantics { contentDescription = label }, contentAlignment = Alignment.Center) {
        Box(Modifier.size(width = 4.dp, height = 56.dp).clip(CircleShape).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)))
    }
}

/**
 * Asked for by a long press on a rail app: which pane to open [app] in. [anchor] is the centre of
 * the pressed icon in the shell's coordinates; [horizontal] tells a bottom bar from a side rail.
 */
internal class PaneChoice(val app: CarAppEntry, val anchor: Offset, val horizontal: Boolean, val railOnRight: Boolean)

/**
 * The two pane icons next to the pressed rail app, over everything else. Drawn in the car screen's
 * own window, not a popup: the head unit's touches only reach that window. A tap elsewhere closes it.
 */
@Composable
internal fun PaneChooser(choice: PaneChoice, onPick: (left: Boolean) -> Unit, onDismiss: () -> Unit) {
    val density = LocalDensity.current
    val gap = with(density) { (LocalCarDimens.current.railButton / 2 + 8.dp).toPx() }
    // Two 64 dp options with 4 dp padding around and between them.
    val w = with(density) { 140.dp.toPx() }
    val h = with(density) { 72.dp.toPx() }
    val (x, y) = when {
        choice.horizontal -> choice.anchor.x - w / 2 to choice.anchor.y - gap - h
        choice.railOnRight -> choice.anchor.x - gap - w to choice.anchor.y - h / 2
        else -> choice.anchor.x + gap to choice.anchor.y - h / 2
    }
    BoxWithConstraints(Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
        val maxX = constraints.maxWidth - w
        val maxY = constraints.maxHeight - h
        Row(
            Modifier.offset { IntOffset(x.coerceIn(0f, maxOf(0f, maxX)).roundToInt(), y.coerceIn(0f, maxOf(0f, maxY)).roundToInt()) }
                .clip(RoundedCornerShape(20.dp)).background(MaterialTheme.colorScheme.surfaceContainerHigh).padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            PaneOption(left = true, stringResource(R.string.split_open_left)) { onPick(true) }
            PaneOption(left = false, stringResource(R.string.split_open_right)) { onPick(false) }
        }
    }
}

@Composable
private fun PaneOption(left: Boolean, label: String, onClick: () -> Unit) {
    val color = MaterialTheme.colorScheme.primary
    Box(
        Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).clickable(onClick = onClick).semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        PaneIcon(left, color)
    }
}

/** A screen split in two, the chosen half filled. */
@Composable
private fun PaneIcon(left: Boolean, color: Color) {
    Canvas(Modifier.size(width = 40.dp, height = 28.dp)) {
        val stroke = 2.dp.toPx()
        val radius = CornerRadius(4.dp.toPx())
        val half = size.width / 2
        drawRoundRect(color, topLeft = Offset(if (left) 0f else half, 0f), size = Size(half, size.height), cornerRadius = radius)
        drawRoundRect(color, cornerRadius = radius, style = Stroke(stroke))
        drawLine(color, Offset(half, 0f), Offset(half, size.height), stroke)
    }
}
