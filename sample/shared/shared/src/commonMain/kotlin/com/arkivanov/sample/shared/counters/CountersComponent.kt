package com.arkivanov.sample.shared.counters

import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import com.arkivanov.sample.shared.counters.counter.CounterComponent

interface CountersComponent : BackHandlerOwner {

    val childStack: Value<ChildStack<*, CounterComponent>>

    fun onBackPressed()
}
