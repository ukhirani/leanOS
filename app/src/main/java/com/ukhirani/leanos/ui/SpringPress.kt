package com.ukhirani.leanos.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.ripple
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role

/**
 * Tap (and optional long-press) with spring press feedback and a light haptic tick. Put it first in a
 * modifier chain so the whole element scales; it replaces `clickable`, don't combine the two.
 */
@OptIn(ExperimentalFoundationApi::class)
fun Modifier.springPress(
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null,
    onClickLabel: String? = null,
    onLongClickLabel: String? = null,
    role: Role = Role.Button,
): Modifier = composed {
    val haptics = LocalHapticFeedback.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press",
    )
    graphicsLayer { scaleX = scale; scaleY = scale }
        .combinedClickable(
            interactionSource = interaction,
            indication = ripple(),
            role = role,
            onClickLabel = onClickLabel,
            onLongClickLabel = onLongClickLabel,
            onLongClick = onLongPress?.let { long ->
                {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    long()
                }
            },
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.VirtualKey)
                onClick()
            },
        )
}
