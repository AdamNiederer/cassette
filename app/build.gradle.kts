plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp.plugin)
    alias(libs.plugins.hilt.plugin)
}

android {
    namespace = "com.example.cassette"
    compileSdk {
        version = release(36)
    }
    defaultConfig {
        applicationId = "com.example.cassette"
        minSdk = 34
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }
    buildTypes {
        debug {
            isMinifyEnabled = false
            isShrinkResources = false
            isDebuggable = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
    useLibrary("wear-sdk")
    buildFeatures {
        compose = true
    }
    // splits {
    //     abi {
    //         isEnable = true
    //         reset()
    //         include("armeabi-v7a")
    //         isUniversalApk = false
    //     }
    // }
}

dependencies {
    implementation(libs.play.services.wearable)
    implementation(libs.androidx.core)
    implementation(libs.androidx.wear)
    implementation(libs.wear.ongoing)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.compose.material)
    implementation(libs.compose.material3)
    implementation(libs.compose.foundation)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.wear.tooling.preview)
    implementation(libs.activity.compose)
    implementation(libs.core.splashscreen)
    implementation(libs.tiles)
    implementation(libs.tiles.material)
    implementation(libs.tiles.tooling.preview)
    implementation(libs.horologist.compose.tools)
    implementation(libs.horologist.compose.layout)
    implementation(libs.horologist.tiles)
    implementation(libs.horologist.media.ui)
    implementation(libs.horologist.media.data)
    implementation(libs.horologist.audio.ui)
    implementation(libs.protolayout.material3)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)
    implementation(libs.media3.extractor)
    implementation(libs.watchface.complications.data.source.ktx)
    implementation(libs.dagger.hilt.android)
    implementation(libs.work.runtime.ktx)
    implementation(libs.hilt.work)
    implementation(libs.room.runtime)
    implementation(libs.room.paging)
    implementation(libs.paging.runtime)
    implementation(libs.paging.compose)
    implementation(libs.palette)
    implementation(libs.jaudiotagger)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    debugImplementation(libs.tiles.tooling)
    ksp(libs.dagger.hilt.compiler)
    ksp(libs.hilt.compiler)
    ksp(libs.room.compiler)
    annotationProcessor(libs.room.compiler)
}
