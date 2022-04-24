plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.compose")
    id("kotlin-parcelize")
    id("com.arkivanov.gradle.setup")
}

setupAndroidApp {
    androidApp(
        applicationId = "com.arkivanov.counter.app",
        versionCode = 1,
        versionName = "1.0",
    )
}

android {
    packagingOptions {
        exclude("META-INF/*")
    }
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":decompose"))
    implementation(project(":extensions-compose-jetbrains"))
    implementation(project(":extensions-android"))
    implementation(project(":sample:counter:shared"))
    implementation(project(":sample:counter:ui-android"))
    implementation(project(":sample:counter:ui-compose"))
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(deps.androidx.core.coreKtx)
    implementation(deps.androidx.appcompat.appcompat)
    implementation(deps.androidx.lifecycle.lifecycleCommonJava8)
    implementation(deps.androidx.activity.activityCompose)
    implementation(deps.android.material.material)
}
