@file:OptIn(ExperimentalDecomposeApi::class)

package com.arkivanov.decompose.extensions.compose.jetbrains.stack.animation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import com.arkivanov.decompose.Child
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.router.stack.ChildStack

@ExperimentalDecomposeApi
internal class DefaultStackAnimation<C : Any, T : Any>(
    private val selector: (child: Child.Created<C, T>, direction: Direction) -> StackAnimator
) : StackAnimation<C, T> {

    @Composable
    override operator fun invoke(stack: ChildStack<C, T>, modifier: Modifier, content: @Composable (child: Child.Created<C, T>) -> Unit) {
        var activePage by remember { mutableStateOf(stack.activePage()) }
        var items by remember { mutableStateOf(getAnimationItems(newPage = activePage, oldPage = null)) }

        if (stack.active.configuration != activePage.child.configuration) {
            val oldPage = activePage
            activePage = stack.activePage()
            items = getAnimationItems(newPage = activePage, oldPage = oldPage)
        }

        Box(modifier = modifier) {
            items.forEach { (configuration, item) ->
                val (child, direction) = item

                key(configuration) {
                    val animator = remember(direction) { selector(child, direction) }

                    animator(
                        direction = direction,
                        onFinished = {
                            when (direction) {
                                Direction.EXIT_FRONT,
                                Direction.EXIT_BACK -> items = items - configuration
                                Direction.ENTER_FRONT,
                                Direction.ENTER_BACK -> items = items + (configuration to item.copy(direction = Direction.IDLE))
                                Direction.IDLE -> Unit
                            }
                        }
                    ) { modifier ->
                        Box(modifier = modifier) {
                            content(child)
                        }
                    }
                }
            }

            // A workaround until https://issuetracker.google.com/issues/214231672.
            // Normally only the exiting child be disabled.
            if (items.size > 1) {
                Overlay(modifier = Modifier.matchParentSize())
            }
        }
    }

    @Composable
    private fun Overlay(modifier: Modifier) {
        Box(
            modifier = modifier.pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consumeAllChanges() }
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
                listOf(AnimationItem(newPage.child, Direction.IDLE))

            newPage.index >= oldPage.index ->
                listOf(
                    AnimationItem(oldPage.child, Direction.EXIT_BACK),
                    AnimationItem(newPage.child, Direction.ENTER_FRONT),
                )

            else ->
                listOf(
                    AnimationItem(newPage.child, Direction.ENTER_BACK),
                    AnimationItem(oldPage.child, Direction.EXIT_FRONT),
                )
        }.associateBy { it.child.configuration }

    private data class AnimationItem<out C : Any, out T : Any>(
        val child: Child.Created<C, T>,
        val direction: Direction,
    )

    private class Page<out C : Any, out T : Any>(
        val child: Child.Created<C, T>,
        val index: Int,
    )
}
