package com.arkivanov.sample.app

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.TwoWayConverter
import androidx.compose.animation.core.VectorizedAnimationSpec
import androidx.compose.animation.core.animateOffsetAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import kotlin.time.Duration.Companion.milliseconds

fun Any.print() {
    println(this)
}

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
//    val spec = spec(listOf(Offset(500F, 250F) to 0.5F))
    val spec =
        spec(
            listOf(
                Offset(500F, 150F) to 0.33F,
                Offset(500F, 350F) to 0.66F,
            )
        )
    var offset by remember { mutableStateOf(Offset(0F, 0F)) }
    val anim by animateOffsetAsState(targetValue = offset, animationSpec = spec)
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().weight(1F)) {
            Box(modifier = Modifier.offset { anim.round() }.background(Color.Red).size(32.dp))
        }

        Button(onClick = { offset = Offset(0F, 500F) }) {
            Text("Click")
        }
    }

//    var cards by remember {
//        mutableStateOf(
//            listOf(
//                Card(color = Color.Green),
//                Card(color = Color.Blue),
//                Card(color = Color.Red),
//            )
//        )
//    }
//
//    Box(
//        modifier = Modifier.padding(32.dp).fillMaxSize(),
//        contentAlignment = Alignment.BottomCenter,
//    ) {
//        cards.forEachIndexed { index, card ->
//            key(card) {
//                CardContent(
//                    color = card.color,
//                    offsetY = ((cards.lastIndex - index) * -16).dp.toPx(),
//                    onMoveToBack = { cards = listOf(card) + (cards - card) },
//                )
//            }
//        }
//    }
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
    ANIM,
    CANCEL
}

fun spec(
    keyframes: List<Pair<Offset, Float>>,
): AnimationSpec<Offset> =
    object : AnimationSpec<Offset> {
        override fun <V : AnimationVector> vectorize(converter: TwoWayConverter<Offset, V>): VectorizedAnimationSpec<V> =
            VectorizedAnimationSpecImpl(keyframes, converter)
    }

private class VectorizedAnimationSpecImpl<V : AnimationVector>(
    private val keyframes: List<Pair<Offset, Float>>,
    private val converter: TwoWayConverter<Offset, V>,
) : VectorizedAnimationSpec<V> {
    override val isInfinite: Boolean = false
    private val durationNanos = 1000.milliseconds.inWholeNanoseconds

    override fun getDurationNanos(initialValue: V, targetValue: V, initialVelocity: V): Long = durationNanos

    override fun getValueFromNanos(playTimeNanos: Long, initialValue: V, targetValue: V, initialVelocity: V): V {
        val durationFraction = (playTimeNanos.toDouble() / durationNanos.toDouble()).toFloat().coerceIn(0F, 1F)
        val initialOffset = converter.convertFromVector(initialValue)
        val targetOffset = converter.convertFromVector(targetValue)
        val allKeyframes = (keyframes + (initialOffset to 0F) + (targetOffset to 1F)).sortedBy { it.second }

        val (offset1, durationFraction1) = allKeyframes.last { it.second <= durationFraction }
        val (offset2, durationFraction2) = allKeyframes.first { it.second >= durationFraction }

        if (durationFraction1 == durationFraction2) {
            return converter.convertToVector(offset1)
        }



        val (x1, y1) = offset1
        val (x2, y2) = offset2
        val f = (durationFraction - durationFraction1) / (durationFraction2 - durationFraction1)
        val x = f * (x2 - x1) + x1
        val y = f * (y2 - y1) + y1

        return converter.convertToVector(Offset(x = x, y = y))
    }

    override fun getVelocityFromNanos(playTimeNanos: Long, initialValue: V, targetValue: V, initialVelocity: V): V =
        initialVelocity
}


@Composable
fun CardContent(color: Color, offsetY: Float, onMoveToBack: () -> Unit) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var mode by remember { mutableStateOf(Mode.IDLE) }
    var startOffset: Offset by remember { mutableStateOf(Offset.Zero) }
    var dragOffset: Offset by remember { mutableStateOf(Offset.Zero) }
    val upperOffsetY = -350.dp.toPx()

    val targetOffset: Offset = remember(mode, offsetY, dragOffset) {
        when (mode) {
            Mode.IDLE -> Offset(x = 0F, y = offsetY)
            Mode.DRAG -> dragOffset
            Mode.ANIM,
            Mode.CANCEL -> Offset(x = 0F, y = offsetY)
        }
    }

    val animationSpec: AnimationSpec<Offset> = remember(mode) {
        when (mode) {
            Mode.IDLE -> tween()
            Mode.DRAG -> snap()

            Mode.ANIM ->
                keyframes {
                    durationMillis = 5000

                    Offset(x = size.width.toFloat(), y = upperOffsetY / 2F) atFraction 0.25F
                    Offset(x = 0F, y = upperOffsetY) atFraction 0.5F
                }

            Mode.CANCEL -> tween()
        }
    }

    val animatedOffset by animateOffsetAsState(targetValue = targetOffset, animationSpec = animationSpec)

//    DisposableEffect(animatedOffset, mode, offsetY) {
//        if (mode == Mode.UP && animatedOffset.y == upperOffsetY) {
//            onMoveToBack()
//            mode = Mode.DOWN
//        }
//
//        if (mode == Mode.DOWN && animatedOffset.y == offsetY) {
//            mode = Mode.IDLE
//        }
//
//        onDispose {}
//    }

    Box(
        Modifier
            .offset { animatedOffset.round() }
            .onPlaced { size = it.size }
            .pointerInput(Unit) {
                var velocity = 0F

                detectDragGestures(
                    onDragStart = {
                        velocity = 0F
                        startOffset = it
                        mode = Mode.DRAG
                    },
                    onDragEnd = {
                        mode = if (velocity > 0.7) Mode.ANIM else Mode.CANCEL
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

                val distanceFactor = (animatedOffset.y - offsetY) / (upperOffsetY - offsetY)
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
