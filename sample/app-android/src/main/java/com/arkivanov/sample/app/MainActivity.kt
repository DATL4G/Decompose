package com.arkivanov.sample.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.defaultComponentContext
import com.arkivanov.decompose.extensions.android.DefaultViewContext
import com.arkivanov.essenty.lifecycle.essentyLifecycle
import com.arkivanov.sample.shared.dynamicfeatures.dynamicfeature.DefaultFeatureInstaller
import com.arkivanov.sample.shared.root.RootComponent
import com.arkivanov.sample.shared.root.DefaultRootComponent
import com.arkivanov.sample.shared.root.RootContent
import com.arkivanov.sample.shared.root.RootView
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                Surface(color = MaterialTheme.colors.background) {
                    Box(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        CardContent()
                    }
                }
            }
        }
    }
}


@Composable
fun CardContent() {
    var offsetY by remember { mutableStateOf(0F) }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(size = 16.dp))
            .aspectRatio(ratio = 1.5882353F)
            .background(Color.Red)
            .offset { IntOffset(x = 0, y = offsetY.roundToInt()) }
            .draggable(state = rememberDraggableState {

                offsetY = it }, orientation = Orientation.Horizontal)
    ) {

    }
}

