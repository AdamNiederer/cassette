package com.example.cassette.presentation

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.ambient.AmbientLifecycleObserver
import com.example.cassette.data.types.Track
import com.example.cassette.data.repositories.MusicRepository
import com.example.cassette.presentation.components.TrackProgressBar
import com.example.cassette.presentation.viewmodels.PlayerViewModel
import com.example.cassette.presentation.views.PlayerView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class PlayerActivity : ComponentActivity() {

    private var isAmbient by mutableStateOf(false)
    private var ambientOffset by mutableStateOf(0 to 0)

    private val ambientCallback = object : AmbientLifecycleObserver.AmbientLifecycleCallback {
        override fun onEnterAmbient(ambientDetails: AmbientLifecycleObserver.AmbientDetails) {
            isAmbient = true
        }

        override fun onExitAmbient() {
            isAmbient = false
            ambientOffset = 0 to 0
        }

        override fun onUpdateAmbient() {
            ambientOffset = ((-8..8).random() to (-8..8).random())
        }
    }

    private val ambientObserver by lazy { AmbientLifecycleObserver(this, ambientCallback) }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)

        lifecycle.addObserver(ambientObserver)

        setContent {
            PlayerView(
                viewModel = viewModel<PlayerViewModel>(),
                isAmbient = isAmbient,
                ambientOffset = ambientOffset
            )
        }
    }
}