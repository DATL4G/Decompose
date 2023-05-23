package com.arkivanov.decompose.extensions.compose.jetbrains

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.jetbrains.utils.IconCompat
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackDispatcher
import com.arkivanov.essenty.backhandler.BackDispatcher.PredictiveBackDispatcher
import com.arkivanov.essenty.backhandler.BackEvent

/**
 * Handles back gestures on both edges of the screen and drives the provided [backDispatcher] accordingly,
 * while showing the provided [backIcon]. If [onClose] callback is supplied, then the back gesture is also handled
 * when there are no other enabled back callbacks registered in [backDispatcher].
 */
@ExperimentalDecomposeApi
@Composable
fun PredictiveBackGestureOverlay(
    backDispatcher: BackDispatcher,
    backIcon: ImageVector,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    PredictiveBackGestureOverlay(
        backDispatcher = backDispatcher,
        modifier = modifier,
        backIcon = { progress, _ ->
            PredictiveBackGestureIcon(
                imageVector = backIcon,
                progress = progress,
            )
        },
        onClose = onClose,
        content = content,
    )
}

/**
 * Handles back gestures on both edges of the screen and drives the provided [backDispatcher] accordingly,
 * while showing the provided [backIcon]. If [onClose] callback is supplied, then the back gesture is also handled
 * when there are no other enabled back callbacks registered in [backDispatcher].
 */
@ExperimentalDecomposeApi
@Composable
fun PredictiveBackGestureOverlay(
    backDispatcher: BackDispatcher,
    backIcon: (@Composable (progress: Float, edge: BackEvent.SwipeEdge) -> Unit)?,
    modifier: Modifier = Modifier,
    onClose: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val iconState: MutableState<IconState> = remember { mutableStateOf(IconState()) }

    Box(
        modifier = modifier.handleBackGestures(
            backDispatcher = backDispatcher,
            onIconMoved = { position, progress, edge ->
                iconState.value =
                    IconState(
                        position = position,
                        progress = progress,
                        edge = edge,
                        isVisible = true,
                    )
            },
            onIconHidden = { iconState.value = iconState.value.copy(isVisible = false) }
        ),
    ) {
        content()

        if (backIcon != null) {
            val (position, progress, edge, isVisible) = iconState.value

            AnimatedVisibility(
                visible = isVisible,
                modifier = Modifier.backIconOffset(position = position),
                enter = fadeIn(),
                exit = fadeOut(),
                content = { backIcon(progress, edge.toSwipeEdge()) },
            )
        }
    }

    if (onClose != null) {
        DisposableEffect(backDispatcher, onClose) {
            val callback = BackCallback(priority = BackCallback.PRIORITY_MIN, onBack = onClose)
            backDispatcher.register(callback)
            onDispose { backDispatcher.unregister(callback) }
        }
    }
}

@Composable
private fun PredictiveBackGestureIcon(
    imageVector: ImageVector,
    progress: Float,
    iconColor: Color = Color.Unspecified,
    backgroundColor: Color = Color.LightGray,
) {
    IconCompat(
        painter = rememberVectorPainter(imageVector),
        modifier = Modifier
            .alpha((progress * 6F).coerceAtMost(1F))
            .background(color = backgroundColor, shape = CircleShape)
            .padding(4.dp),
        tint = iconColor,
    )
}

private fun Modifier.handleBackGestures(
    backDispatcher: BackDispatcher,
    onIconMoved: (position: Offset, progress: Float, BackGestureHandler.Edge) -> Unit,
    onIconHidden: () -> Unit,
): Modifier =
    pointerInput(backDispatcher) {
        awaitEachGesture {
            onIconHidden()

            val down = awaitFirstDown(pass = PointerEventPass.Initial)
            val startPosition = down.position

            if ((startPosition.x > 16.dp.toPx()) && (startPosition.x < size.width - 16.dp.toPx())) {
                return@awaitEachGesture
            }

            val handler =
                BackGestureHandler(
                    startPosition = startPosition,
                    size = size,
                    cancelYOffset = 16.dp.toPx(),
                    startXOffset = 16.dp.toPx(),
                    backDispatcher = backDispatcher,
                    onIconMoved = onIconMoved,
                )

            with(handler) { handleGesture() }
        }
    }

private fun Modifier.backIconOffset(position: Offset): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
        layout(constraints.maxWidth, constraints.minHeight) {
            placeable.placeRelative(
                x = (position.x.toInt() - placeable.width / 2),
                y = (position.y.toInt() - 48.dp.roundToPx()).coerceAtLeast(64.dp.roundToPx()),
            )
        }
    }

private fun BackGestureHandler.Edge.toSwipeEdge(): BackEvent.SwipeEdge =
    when (this) {
        BackGestureHandler.Edge.LEFT -> BackEvent.SwipeEdge.LEFT
        BackGestureHandler.Edge.RIGHT -> BackEvent.SwipeEdge.RIGHT
    }

private data class IconState(
    val position: Offset = Offset.Zero,
    val progress: Float = 0F,
    val edge: BackGestureHandler.Edge = BackGestureHandler.Edge.RIGHT,
    val isVisible: Boolean = false,
)

private class BackGestureHandler(
    private val startPosition: Offset,
    private val size: IntSize,
    private val cancelYOffset: Float,
    private val startXOffset: Float,
    private val backDispatcher: BackDispatcher,
    private val onIconMoved: (position: Offset, progress: Float, Edge) -> Unit,
) {

    private val edge = if (startPosition.x < size.width / 2F) Edge.LEFT else Edge.RIGHT
    private var changesIterator: Iterator<PointerInputChange>? = null

    private suspend fun AwaitPointerEventScope.awaitChange(): PointerInputChange {
        var iterator = changesIterator

        if ((iterator == null) || !iterator.hasNext()) {
            iterator = awaitPointerEvent(pass = PointerEventPass.Initial).changes.iterator()
            changesIterator = iterator
        }

        return iterator.next()
    }

    suspend fun AwaitPointerEventScope.handleGesture() {
        val dispatcher: PredictiveBackDispatcher = awaitStart() ?: return
        processGesture(dispatcher)
    }

    private suspend fun AwaitPointerEventScope.awaitStartChange(): PointerInputChange? {
        while (true) {
            val change = awaitChange()
            val position = change.position

            if ((position.y < startPosition.y - cancelYOffset) || (position.y > startPosition.y + cancelYOffset)) {
                return null
            }

            when (edge) {
                Edge.LEFT ->
                    if (position.x > startPosition.x + startXOffset) {
                        return change
                    }

                Edge.RIGHT ->
                    if (position.x < startPosition.x - startXOffset) {
                        return change
                    }
            }
        }
    }

    private suspend fun AwaitPointerEventScope.awaitStart(): PredictiveBackDispatcher? {
        val change = awaitStartChange() ?: return null
        val position = change.position

        val dispatcher =
            backDispatcher.startPredictiveBack(
                BackEvent(
                    progress = getProgress(position = position),
                    swipeEdge = edge.toSwipeEdge(),
                    touchX = position.x,
                    touchY = position.y,
                )
            ) ?: return null

        change.consume()

        return dispatcher
    }

    private suspend fun AwaitPointerEventScope.processGesture(dispatcher: PredictiveBackDispatcher) {
        while (true) {
            val change = awaitChange()
            val position = change.position
            change.consume()

            val progress = getProgress(position = position)

            dispatcher.progress(
                BackEvent(
                    progress = progress,
                    swipeEdge = edge.toSwipeEdge(),
                    touchX = position.x,
                    touchY = position.y,
                )
            )

            if (!change.pressed) {
                if (progress > 0.2F) {
                    dispatcher.finish()
                } else {
                    dispatcher.cancel()
                }

                return
            }

            onIconMoved(getIconOffset(progress = progress, position = position), progress, edge)
        }
    }

    private fun getProgress(position: Offset): Float =
        when (edge) {
            Edge.LEFT -> {
                val startX = startPosition.x + startXOffset
                (position.x - startX) / (size.width - startX)
            }

            Edge.RIGHT -> {
                val startX = startPosition.x - startXOffset
                (startX - position.x) / startX
            }
        }.coerceIn(0F, 1F)

    private fun getIconOffset(progress: Float, position: Offset): Offset =
        Offset(
            x = getChevronPositionX(progress = progress, position = position),
            y = getChevronPositionY(position = position),
        )

    private fun getChevronPositionX(progress: Float, position: Offset): Float {
        val factor = 2F + 3F * progress

        return when (edge) {
            Edge.LEFT -> position.x / factor
            Edge.RIGHT -> startPosition.x + (position.x - startPosition.x) / factor
        }
    }

    private fun getChevronPositionY(position: Offset): Float =
        startPosition.y + (position.y - startPosition.y) / 4F

    private fun Edge.toSwipeEdge(): BackEvent.SwipeEdge =
        when (this) {
            Edge.LEFT -> BackEvent.SwipeEdge.LEFT
            Edge.RIGHT -> BackEvent.SwipeEdge.RIGHT
        }

    enum class Edge {
        LEFT,
        RIGHT,
    }
}
