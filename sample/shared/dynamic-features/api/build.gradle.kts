import com.arkivanov.gradle.iosCompat
import com.arkivanov.gradle.setupMultiplatform

plugins {
    id("kotlin-multiplatform")
    id("com.android.library")
    id("com.arkivanov.gradle.setup")
}

setupMultiplatform {
    android()
    jvm()
    js(IR) { browser() }
    iosCompat()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation(project(":decompose"))
            }
        }
    }
}
