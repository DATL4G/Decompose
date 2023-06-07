package com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation

import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.arkivanov.essenty.backhandler.BackEvent
import com.arkivanov.essenty.backhandler.BackHandler

class PredictiveBackGestureConfig(
    val backHandler: BackHandler,
    val exitModifier: (progress: Float, edge: BackEvent.SwipeEdge) -> Modifier = ::getPredictiveBackGestureAnimationModifier,
    val enterModifier: (progress: Float, edge: BackEvent.SwipeEdge) -> Modifier = { _, _ -> Modifier },
    val onBack: () -> Unit,
) {
    private companion object {
        private fun getPredictiveBackGestureAnimationModifier(
            progress: Float,
            edge: BackEvent.SwipeEdge,
        ): Modifier =
            Modifier
                .scale(1F - progress * 0.25F)
                .absoluteOffset(
                    x = when (edge) {
                        BackEvent.SwipeEdge.LEFT -> 32.dp * progress
                        BackEvent.SwipeEdge.RIGHT -> (-32).dp * progress
                        BackEvent.SwipeEdge.UNKNOWN -> 0.dp
                    },
                )
                .alpha(((1F - progress) * 2F).coerceAtMost(1F))
                .clip(RoundedCornerShape(percent = 10))
    }
}
