package com.arkivanov.sample.app

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.math.abs
import kotlin.math.roundToInt

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
                Card(color = Color(0xFF2980B9)),
                Card(color = Color(0xFFE74C3C)),
                Card(color = Color(0xFF27AE60)),
                Card(color = Color(0xFFF39C12)),
            )
        )
    }

    var size by remember { mutableStateOf(IntSize.Zero) }

    Box(
        modifier = Modifier.padding(32.dp).fillMaxSize().onPlaced { size = it.size },
        contentAlignment = Alignment.BottomCenter,
    ) {
        cards.forEachIndexed { index, card ->
            key(card) {
                CardContent(
                    color = card.color,
                    offsetY = ((cards.lastIndex - index) * -16).dp.toPx(),
                    layoutWidth = size.width.toFloat(),
                    onMoveToBack = { cards = listOf(card) + (cards - card) },
                )
            }
        }
    }
}

@Composable
fun CardContent(color: Color, offsetY: Float, layoutWidth: Float, onMoveToBack: () -> Unit) {
    var cardSize by remember { mutableStateOf(IntSize.Zero) }
    var mode by remember { mutableStateOf(Mode.IDLE) }
    var startOffset: Offset by remember { mutableStateOf(Offset.Zero) }
    var dragOffset: Offset by remember { mutableStateOf(Offset.Zero) }
    var dragChange: PointerInputChange? by remember { mutableStateOf(null) }
    val upperOffsetY = -350.dp.toPx()

    val targetOffset: Offset =
        remember(mode, offsetY, dragOffset) {
            when (mode) {
                Mode.DRAG -> dragOffset + Offset(x = 0F, y = offsetY)

                Mode.UP -> {
                    val (x1, y1) = dragOffset
                    val x2 = x1 + requireNotNull(dragChange).dx
                    val y2 = y1 + requireNotNull(dragChange).dy
                    val upperOffsetX = ((upperOffsetY - y1) * (x2 - x1) / (y2 - y1) + x1).coerceIn(-cardSize.width.toFloat(), layoutWidth)
                    Offset(x = upperOffsetX, y = upperOffsetY)
                }

                Mode.IDLE,
                Mode.DOWN -> Offset(x = 0F, y = offsetY)
            }
        }

    val animationSpec: AnimationSpec<Offset> = remember(mode) { if (mode == Mode.DRAG) snap() else tween() }
    val animatedOffset by animateOffsetAsState(targetValue = targetOffset, animationSpec = animationSpec)

    DisposableEffect(animatedOffset, mode, offsetY) {
        if ((mode == Mode.UP) && (animatedOffset.y == upperOffsetY)) {
            onMoveToBack()
            mode = Mode.DOWN
        } else if ((mode == Mode.DOWN) && (animatedOffset.y == offsetY)) {
            mode = Mode.IDLE
        }

        onDispose {}
    }

    var targetRotationY by remember { mutableStateOf(0F) }
    val animatedRotationY by animateFloatAsState(targetValue = targetRotationY, animationSpec = tween())

    Column(
        modifier = Modifier
            .onPlaced { cardSize = it.size }
            .offset { animatedOffset.round() }
            .pointerInput(Unit) {
                var velocity = 0F

                detectDragGestures(
                    onDragStart = {
                        velocity = 0F
                        startOffset = it
                        dragOffset = Offset.Zero
                        dragChange = null
                        mode = Mode.DRAG
                    },
                    onDragEnd = { mode = if (velocity > 0.7) Mode.UP else Mode.DOWN },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        velocity = change.velocity
                        dragOffset += dragAmount
                        dragChange = change
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { targetRotationY += 180F }
            }
            .graphicsLayer {
                if (mode == Mode.IDLE) {
                    return@graphicsLayer
                }

                transformOrigin =
                    TransformOrigin(
                        pivotFractionX = startOffset.x / size.width,
                        pivotFractionY = startOffset.y / size.height,
                    )

                val distanceFactor = (animatedOffset.y - offsetY) / (upperOffsetY - offsetY)
                val horizontalFactor = transformOrigin.pivotFractionX * 2F - 1F
                rotationZ = distanceFactor * horizontalFactor * -30F
            }
            .graphicsLayer { rotationY = animatedRotationY }
            .clip(RoundedCornerShape(size = 16.dp))
            .aspectRatio(ratio = 1.5882353F)
            .background(color)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp, alignment = Alignment.CenterVertically),
    ) {
        val deg = animatedRotationY.roundToInt() % 360
        if ((deg <= 90) || (deg >= 270)) {
            ImagePlaceholder()
            RowPlaceholder()
        } else {
            repeat(3) {
                RowPlaceholder()
            }
        }
    }
}

private fun Modifier.offsetCard(startOffset: Offset, offsetY: Float, ): Modifier =
    graphicsLayer {
        transformOrigin =
            TransformOrigin(
                pivotFractionX = startOffset.x / size.width,
                pivotFractionY = startOffset.y / size.height,
            )

        val distanceFactor = (animatedOffset.y - offsetY) / (upperOffsetY - offsetY)
        val horizontalFactor = transformOrigin.pivotFractionX * 2F - 1F
        rotationZ = distanceFactor * horizontalFactor * -30F
    }

@Composable
private fun RowPlaceholder() {
    Box(modifier = Modifier.fillMaxWidth().height(18.dp).background(Color.White.copy(alpha = 0.5F)))
}

@Composable
private fun ImagePlaceholder() {
    Box(modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.5F)))
}

private data class Card(
    val color: Color,
)

enum class Mode {
    IDLE,
    DRAG,
    UP,
    DOWN
}

@Composable
private fun Dp.toPx(): Float =
    with(LocalDensity.current) { toPx() }

val PointerInputChange.velocity: Float
    get() = abs(position.y - previousPosition.y) / (uptimeMillis - previousUptimeMillis).toFloat()

val PointerInputChange.dx: Float
    get() = position.x - previousPosition.x

val PointerInputChange.dy: Float
    get() = position.y - previousPosition.y
