package com.arkivanov.sample.shared.counters

import android.content.res.Configuration
import android.view.View
import android.widget.LinearLayout
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.android.stack.StackRouterView
import com.arkivanov.decompose.extensions.android.ViewContext
import com.arkivanov.decompose.extensions.android.layoutInflater
import com.arkivanov.decompose.extensions.android.resources
import com.arkivanov.sample.shared.R
import com.arkivanov.sample.shared.counters.counter.CounterView

@ExperimentalDecomposeApi
@Suppress("FunctionName") // Factory function
internal fun ViewContext.CountersView(counters: Counters): View {
    val layout = layoutInflater.inflate(R.layout.counters, parent, false) as LinearLayout
    val firstRouter: StackRouterView = layout.findViewById(R.id.router_first)
    val secondRouter: StackRouterView = layout.findViewById(R.id.router_second)

    layout.orientation =
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            LinearLayout.HORIZONTAL
        } else {
            LinearLayout.VERTICAL
        }

    firstRouter.children(counters.firstChildStack, lifecycle) { parent, child, _ ->
        parent.removeAllViews()
        parent.addView(CounterView(child))
    }

    secondRouter.children(counters.secondChildStack, lifecycle) { parent, child, _ ->
        parent.removeAllViews()
        parent.addView(CounterView(child))
    }

    return layout
}
