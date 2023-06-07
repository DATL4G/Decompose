package com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.essenty.backhandler.BackCallback
import com.arkivanov.essenty.backhandler.BackEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

internal abstract class AbstractStackAnimation<C : Any, T : Any>(
    private val disableInputDuringAnimation: Boolean,
    private val backGestureConfig: PredictiveBackGestureConfig?,
) : StackAnimation<C, T> {

    @Composable
    protected abstract fun Child(
        item: AnimationItem<C, T>,
        onFinished: () -> Unit,
        content: @Composable (child: Child.Created<C, T>) -> Unit,
    )

    @Composable
    override operator fun invoke(stack: ChildStack<C, T>, modifier: Modifier, content: @Composable (child: Child.Created<C, T>) -> Unit) {
        var backItems: Map<C, AnimationItem<C, T>>? by remember(stack) { mutableStateOf(null) }
        var activePage by remember(backItems == null) { mutableStateOf(stack.activePage()) }
        var items by remember(backItems == null) { mutableStateOf(getAnimationItems(newPage = activePage, oldPage = null)) }

        if (stack.active.configuration != activePage.child.configuration) {
            val oldPage = activePage
            activePage = stack.activePage()
            items = getAnimationItems(newPage = activePage, oldPage = oldPage)
        }

        Box(modifier = modifier) {
            (backItems ?: items).forEach { (configuration, item) ->
                key(configuration) {
                    Box(modifier = item.backModifier ?: Modifier) {
                        Child(
                            item = item,
                            onFinished = {
                                if (item.backModifier == null) {
                                    if (item.direction.isExit) {
                                        items -= configuration
                                    } else {
                                        items += (configuration to item.copy(otherChild = null))
                                    }
                                }
                            },
                            content = content,
                        )
                    }
                }
            }

            // A workaround until https://issuetracker.google.com/issues/214231672.
            // Normally only the exiting child should be disabled.
            if (disableInputDuringAnimation && (items.size > 1)) {
                Overlay(modifier = Modifier.matchParentSize())
            }
        }

        if ((backGestureConfig != null) && stack.backStack.isNotEmpty()) {
            DisposableEffect(stack) {
                val callback =
                    BackCallbackImpl(
                        backGestureConfig = backGestureConfig,
                        exitChild = stack.active,
                        enterChild = stack.backStack.last(),
                        setItems = { backItems = it },
                    )

                callback.init()
                onDispose { callback.dispose() }
            }
        }
    }

    private class BackCallbackImpl<C : Any, T : Any>(
        private val backGestureConfig: PredictiveBackGestureConfig,
        private val exitChild: Child.Created<C, T>,
        private val enterChild: Child.Created<C, T>,
        private val setItems: (Map<C, AnimationItem<C, T>>?) -> Unit,
    ) : BackCallback() {
        private val scope = CoroutineScope(Dispatchers.Main.immediate)
        private var progress = 0F
        private var edge = BackEvent.SwipeEdge.UNKNOWN

        fun init() {
            backGestureConfig.backHandler.register(this)
        }

        fun dispose() {
            backGestureConfig.backHandler.unregister(this)
            scope.cancel()
        }

        override fun onBackStarted(backEvent: BackEvent) {
            progress = backEvent.progress
            edge = backEvent.swipeEdge
            updateItems()
        }

        override fun onBackProgressed(backEvent: BackEvent) {
            progress = backEvent.progress
            edge = backEvent.swipeEdge
            updateItems()
        }

        override fun onBackCancelled() {
            setItems(null)
        }

        override fun onBack() {
            scope.launch { continueGesture() }
        }

        private suspend fun CoroutineScope.continueGesture() {
            while ((progress <= 1F) && isActive) {
                delay(16.milliseconds)
                progress += 0.075F
                updateItems()
            }

            if (isActive) {
                setItems(null)
                backGestureConfig.onBack()
            }
        }

        private fun updateItems() {
            setItems(
                listOf(
                    AnimationItem(
                        child = enterChild,
                        direction = Direction.ENTER_BACK,
                        isInitial = true,
                        otherChild = exitChild,
                        backModifier = backGestureConfig.enterModifier(progress, edge),
                    ),
                    AnimationItem(
                        child = exitChild,
                        direction = Direction.ENTER_FRONT,
                        isInitial = true,
                        otherChild = enterChild,
                        backModifier = backGestureConfig.exitModifier(progress, edge),
                    ),
                ).associateBy { it.child.configuration }
            )
        }
    }

    @Composable
    private fun Overlay(modifier: Modifier) {
        Box(
            modifier = modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
        )
    }

    private fun ChildStack<C, T>.activePage(): Page<C, T> =
        Page(child = active, index = items.lastIndex)

    private fun getAnimationItems(newPage: Page<C, T>, oldPage: Page<C, T>?): Map<C, AnimationItem<C, T>> =
        when {
            oldPage == null ->
                listOf(AnimationItem(child = newPage.child, direction = Direction.ENTER_FRONT, isInitial = true))

            newPage.index >= oldPage.index ->
                listOf(
                    AnimationItem(child = oldPage.child, direction = Direction.EXIT_BACK, otherChild = newPage.child),
                    AnimationItem(child = newPage.child, direction = Direction.ENTER_FRONT, otherChild = oldPage.child),
                )

            else ->
                listOf(
                    AnimationItem(child = newPage.child, direction = Direction.ENTER_BACK, otherChild = oldPage.child),
                    AnimationItem(child = oldPage.child, direction = Direction.EXIT_FRONT, otherChild = newPage.child),
                )
        }.associateBy { it.child.configuration }

    protected data class AnimationItem<out C : Any, out T : Any>(
        val child: Child.Created<C, T>,
        val direction: Direction,
        val isInitial: Boolean = false,
        val otherChild: Child.Created<C, T>? = null,
        val backModifier: Modifier? = null,
    )

    private class Page<out C : Any, out T : Any>(
        val child: Child.Created<C, T>,
        val index: Int,
    )
}
