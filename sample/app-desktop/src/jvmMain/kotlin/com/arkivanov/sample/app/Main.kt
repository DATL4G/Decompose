package com.arkivanov.sample.app

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
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
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
                    DraggableCards()
                }
            }
        }
    }
}

@Composable
fun DraggableCards() {
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
                DraggableCard(
                    layoutSize = size,
                    offsetY = ((cards.lastIndex - index) * -16).dp.toPx(),
                    scale = 1F - (cards.lastIndex - index).toFloat() / 20F,
                    onMoveToBack = { cards = listOf(card) + (cards - card) },
                    content = { isFront -> CardContent(isFront = isFront, color = card.color) },
                )
            }
        }
    }
}

@Composable
fun DraggableCard(
    layoutSize: IntSize,
    offsetY: Float,
    scale: Float,
    onMoveToBack: () -> Unit,
    content: @Composable (isFront: Boolean) -> Unit,
) {
    var cardPosition: Offset by remember { mutableStateOf(Offset.Zero) }
    var cardSize: IntSize by remember { mutableStateOf(IntSize.Zero) }
    val minOffsetX: Float = -cardPosition.x - cardSize.width
    val maxOffsetX: Float = layoutSize.width - cardPosition.x
    val maxOffsetY: Float = -cardPosition.y

    var mode by remember { mutableStateOf(Mode.IDLE) }
    var startTouchPosition: Offset by remember { mutableStateOf(Offset.Zero) }
    var dragTotalOffset: Offset by remember { mutableStateOf(Offset.Zero) }
    var dragLastOffset: Offset by remember { mutableStateOf(Offset.Zero) }
    val dragDistanceThreshold = 3.dp.toPx()

    val animatedOffset by animateOffsetAsState(
        targetValue = when (mode) {
            Mode.DRAG -> dragTotalOffset + Offset(x = 0F, y = offsetY)

            Mode.UP -> {
                val (x1, y1) = dragTotalOffset
                val x2 = x1 + dragLastOffset.x
                val y2 = y1 + dragLastOffset.y
                val upperOffsetX = ((maxOffsetY - y1) * (x2 - x1) / (y2 - y1) + x1).coerceIn(minOffsetX, maxOffsetX)
                Offset(x = upperOffsetX, y = maxOffsetY)
            }

            Mode.IDLE,
            Mode.DOWN -> Offset(x = 0F, y = offsetY)
        },
        animationSpec = if (mode == Mode.DRAG) snap() else tween()
    )

    val animatedScale by animateFloatAsState(targetValue = scale, animationSpec = tween())
    var targetRotationY by remember { mutableStateOf(0F) }
    val animatedRotationY by animateFloatAsState(targetValue = targetRotationY, animationSpec = tween())

    DisposableEffect(animatedOffset, mode, offsetY) {
        if ((mode == Mode.UP) && (animatedOffset.y == maxOffsetY)) {
            onMoveToBack()
            mode = Mode.DOWN
        } else if ((mode == Mode.DOWN) && (animatedOffset.y == offsetY)) {
            mode = Mode.IDLE
        }

        onDispose {}
    }

    Box(
        modifier = Modifier
            .onPlaced {
                cardPosition = it.positionInParent()
                cardSize = it.size
            }
            .widthIn(max = 256.dp)
            .offset { animatedOffset.round() }
            .aspectRatio(ratio = 1.5882353F)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { position ->
                        startTouchPosition = position
                        dragTotalOffset = Offset.Zero
                        mode = Mode.DRAG
                    },
                    onDragEnd = { mode = if (dragLastOffset.getDistance() > dragDistanceThreshold) Mode.UP else Mode.DOWN },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        dragTotalOffset += dragAmount
                        dragLastOffset = dragAmount
                    },
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { targetRotationY += 180F }
            }
            .scale(animatedScale)
            .graphicsLayer {
                if (mode == Mode.IDLE) {
                    return@graphicsLayer
                }

                transformOrigin =
                    TransformOrigin(
                        pivotFractionX = startTouchPosition.x / size.width,
                        pivotFractionY = startTouchPosition.y / size.height,
                    )

                val verticalFactor = (animatedOffset.y - offsetY) / (maxOffsetY - offsetY)
                val horizontalFactor = transformOrigin.pivotFractionX * 2F - 1F
                rotationZ = verticalFactor * horizontalFactor * -30F
            }
            .graphicsLayer { rotationY = animatedRotationY }
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(size = 16.dp), clip = true)
    ) {
        val deg = animatedRotationY.roundToInt() % 360
        content((deg <= 90) || (deg >= 270))
    }
}

@Composable
fun CardContent(isFront: Boolean, color: Color) {
    Column(
        modifier = Modifier.fillMaxSize().background(color).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(space = 16.dp, alignment = Alignment.CenterVertically),
    ) {
        if (isFront) {
            ImagePlaceholder()
            RowPlaceholder()
        } else {
            RowPlaceholder()
            RowPlaceholder()
            RowPlaceholder()
        }
    }
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
    IDLE, DRAG, UP, DOWN
}

@Composable
private fun Dp.toPx(): Float =
    with(LocalDensity.current) { toPx() }
