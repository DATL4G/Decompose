package com.arkivanov.decompose.router.stack

import com.arkivanov.decompose.isUnique
import com.arkivanov.essenty.lifecycle.destroy
import com.arkivanov.essenty.lifecycle.pause
import com.arkivanov.essenty.lifecycle.resume
import com.arkivanov.essenty.lifecycle.stop

internal class StackControllerImpl<C : Any, T : Any>(
    private val routerEntryFactory: RouterEntryFactory<C, T>
) : StackController<C, T> {

    override fun navigate(
        oldStack: RouterStack<C, T>,
        transformer: (stack: List<C>) -> List<C>,
    ): RouterStack<C, T> {
        val oldConfigurationStack = oldStack.configurationStack
        val newConfigurationStack = transformer(oldConfigurationStack)

        if (newConfigurationStack === oldConfigurationStack) {
            return oldStack
        }

        check(newConfigurationStack.isNotEmpty()) { "Configuration stack can not be empty" }
        check(newConfigurationStack.isUnique()) { "Configurations in the stack must be unique" }

        val newActiveConfiguration = newConfigurationStack.last()
        val newActiveEntry: RouterEntry.Created<C, *>
        if (newActiveConfiguration === oldStack.active.configuration) {
            newActiveEntry = oldStack.active
        } else {
            newActiveEntry =
                when (val entry = oldStack.backStack.find { it.configuration === newActiveConfiguration }) {
                    is RouterEntry.Created -> entry.copy(savedState = null)
                    is RouterEntry.Destroyed -> routerEntryFactory(entry.configuration, entry.savedState)
                    null -> routerEntryFactory(newActiveConfiguration)
                }

            oldStack.active.backHandler.stop()
            newActiveEntry.backHandler.start()
            oldStack.active.lifecycleRegistry.pause()
            newActiveEntry.lifecycleRegistry.resume()
            oldStack.active.lifecycleRegistry.stop()
            if (newConfigurationStack.none { it === oldStack.active.configuration }) {
                oldStack.active.instanceKeeperDispatcher.destroy()
                oldStack.active.lifecycleRegistry.destroy()
            }
        }

        val newBackStackEntries =
            newConfigurationStack.dropLast(1).map { configuration ->
                if (oldStack.active.configuration === configuration) {
                    oldStack.active.copy(savedState = oldStack.active.stateKeeperDispatcher.save())
                } else {
                    oldStack
                        .backStack
                        .find { it.configuration === configuration }
                        ?: RouterEntry.Destroyed(configuration = configuration)
                }
            }

        oldStack
            .backStack
            .filter { entry -> newConfigurationStack.none { it === entry.configuration } }
            .destroy()

        return RouterStack(active = newActiveEntry, backStack = newBackStackEntries)
    }
}
