package com.arkivanov.sample.app

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.math.abs

fun main() {
    application {
        Window(
            onCloseRequest = { exitApplication() },
            state = rememberWindowState(
                size = DpSize(350.dp, 600.dp)
            ),
            title = "Sample"
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                MaterialTheme {
                    Cards()
                }
            }
        }
    }
}

@Composable
fun Cards() {
    var cards by remember {
        mutableStateOf(
            listOf(
                Card(color = Color.Green),
                Card(color = Color.Blue),
                Card(color = Color.Red),
            )
        )
    }

    Box(
        modifier = Modifier.padding(32.dp).fillMaxSize(),
        contentAlignment = Alignment.BottomCenter,
    ) {
        cards.forEachIndexed { index, card ->
            key(card) {
                CardContent(
                    color = card.color,
                    offsetY = ((cards.lastIndex - index) * -16).dp.toPx(),
                    onMoveToBack = { cards = listOf(card) + (cards - card) },
                )
            }
        }
    }
}

private data class Card(
    val color: Color,
)

@Composable
private fun Dp.toPx(): Float =
    with(LocalDensity.current) { toPx() }

val SlowOutFastInEasing: Easing = CubicBezierEasing(0.8f, 0.0f, 0.6f, 1.0f)

val PointerInputChange.velocity: Float
    get() = abs(position.y - previousPosition.y) / (uptimeMillis - previousUptimeMillis).toFloat()

enum class Mode {
    IDLE,
    DRAG,
    UP,
    DOWN
}

@Composable
fun CardContent(color: Color, offsetY: Float, onMoveToBack: () -> Unit) {
    var mode by remember { mutableStateOf(Mode.IDLE) }
    var startOffset: Offset by remember { mutableStateOf(Offset.Zero) }
    var dragOffset: Offset by remember { mutableStateOf(Offset.Zero) }
    val upperOffsetY = -350.dp.toPx()

    val targetOffset: Offset = remember(mode, offsetY, dragOffset) {
        when (mode) {
            Mode.IDLE -> Offset(x = 0F, y = offsetY)
            Mode.DRAG -> dragOffset
            Mode.UP -> Offset(x = 0F, y = upperOffsetY)
            Mode.DOWN -> Offset(x = 0F, y = offsetY)
        }
    }

    val animationSpec: AnimationSpec<Offset> by remember(mode) {
        mutableStateOf(
            when (mode) {
                Mode.IDLE -> tween()
                Mode.DRAG -> snap()
                Mode.UP -> tween(easing = FastOutSlowInEasing)
                Mode.DOWN -> tween(easing = SlowOutFastInEasing)
            }
        )
    }

    val animatedOffset by animateOffsetAsState(targetValue = targetOffset, animationSpec = animationSpec)

    DisposableEffect(animatedOffset, mode, offsetY) {
        if (mode == Mode.UP && animatedOffset.y == upperOffsetY) {
            onMoveToBack()
            mode = Mode.DOWN
        }

        if (mode == Mode.DOWN && animatedOffset.y == offsetY) {
            mode = Mode.IDLE
        }

        onDispose {}
    }

    Box(
        Modifier
            .offset { animatedOffset.round() }
            .pointerInput(Unit) {
                var velocity = 0F

                detectDragGestures(
                    onDragStart = {
                        velocity = 0F
                        startOffset = it
                        mode = Mode.DRAG
                    },
                    onDragEnd = {
                        mode = if (velocity > 0.7) Mode.UP else Mode.DOWN
                        dragOffset = Offset.Zero
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocity = change.velocity
                        dragOffset += dragAmount
                    },
                )
            }
            .graphicsLayer {
                val (startOffsetX, startOffsetY) = startOffset.takeUnless { mode == Mode.IDLE } ?: return@graphicsLayer

                transformOrigin =
                    TransformOrigin(
                        pivotFractionX = startOffsetX / size.width,
                        pivotFractionY = startOffsetY / size.height,
                    )

                val distanceFactor =  (animatedOffset.y - offsetY) / (upperOffsetY - offsetY)
                val horizontalFactor = transformOrigin.pivotFractionX * 2F - 1F
                rotationZ = distanceFactor * horizontalFactor * -30F
            }
            .clip(RoundedCornerShape(size = 16.dp))
            .aspectRatio(ratio = 1.5882353F)
            .background(color)
    ) {
//        Text("Minecraft")
    }
}
