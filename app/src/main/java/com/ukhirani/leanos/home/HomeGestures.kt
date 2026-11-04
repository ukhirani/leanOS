package com.ukhirani.leanos.home

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.ukhirani.leanos.platform.FocusService
import kotlin.math.abs

enum class GestureOutcome { Done, EnableServicePrompt }

/** Home gestures that go through the Focus service; without it they ask the user to enable it. */
class HomeGestures(private val service: FocusService) {
    fun onSwipeDown(): GestureOutcome = outcome(service.openNotifications())
    fun onDoubleTap(): GestureOutcome = outcome(service.lockScreen())

    private fun outcome(performed: Boolean) =
        if (performed) GestureOutcome.Done else GestureOutcome.EnableServicePrompt
}

enum class SwipeDirection { Up, Down }

/** A drag counts as a swipe only when it is mostly vertical and longer than [threshold]. */
fun swipeDirection(totalDx: Float, totalDy: Float, threshold: Float): SwipeDirection? = when {
    abs(totalDy) < threshold || abs(totalDy) <= abs(totalDx) -> null
    totalDy > 0 -> SwipeDirection.Down
    else -> SwipeDirection.Up
}

/**
 * All empty-space home gestures in one place, so vertical swipes and taps never fight over
 * the same touch. Children that handle their own touches (clock, chips, apps) take precedence.
 * Callers should pass stable (remembered) lambdas: they key the gesture detectors.
 */
fun Modifier.homeGestures(
    onSwipeDown: () -> Unit,
    onDoubleTap: () -> Unit,
    onSwipeUp: () -> Unit = {},
    onLongPress: (() -> Unit)? = null,
): Modifier = this
    .pointerInput(onSwipeUp, onSwipeDown) {
        val threshold = 64.dp.toPx()
        var total = Offset.Zero
        detectDragGestures(
            onDragStart = { total = Offset.Zero },
            onDragEnd = {
                when (swipeDirection(total.x, total.y, threshold)) {
                    SwipeDirection.Up -> onSwipeUp()
                    SwipeDirection.Down -> onSwipeDown()
                    null -> Unit
                }
            },
        ) { change, amount ->
            change.consume()
            total += amount
        }
    }
    .pointerInput(onDoubleTap, onLongPress) {
        detectTapGestures(
            onDoubleTap = { onDoubleTap() },
            onLongPress = onLongPress?.let { { _ -> it() } },
        )
    }
