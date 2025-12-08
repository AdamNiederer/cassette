package com.example.cassette.presentation

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.wear.tooling.preview.devices.WearDevices
import android.content.Intent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.cassette.presentation.viewmodels.OnboardingViewModel
import com.example.cassette.presentation.viewmodels.PlayerViewModel
import com.example.cassette.presentation.viewmodels.TrackListViewModel
import com.example.cassette.presentation.views.LibraryView
import com.example.cassette.presentation.views.OnboardingView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        setContent {
            MainView()
        }
    }

    @Composable
    fun MainView() {
        val playerViewModel = viewModel<PlayerViewModel>()
        val trackListViewModel = viewModel<TrackListViewModel>()
        val onboardingViewModel = viewModel<OnboardingViewModel>()
        val isOnboardingCompleted by onboardingViewModel.isOnboardingCompleted.collectAsState()

        when (isOnboardingCompleted) {
            null -> {
                // Wait for the state to be loaded from the database
                Box(modifier = Modifier.fillMaxSize())
            }
            false -> {
                OnboardingView(
                    onboardingViewModel = onboardingViewModel,
                    onComplete = {
                        // onboarding state is updated in the view model
                    }
                )
            }
            true -> {
                LibraryView(
                    viewModel = trackListViewModel,
                    onTrackSelected = { track, source ->
                        playerViewModel.play(track, source)
                        startActivity(Intent(this, PlayerActivity::class.java))
                    },
                    onNavigateToPlayer = {
                        startActivity(Intent(this, PlayerActivity::class.java))
                    }
                )
            }
        }
    }

    @Preview(device = WearDevices.SMALL_ROUND)
    @Composable
    fun DefaultPreview() {
    }
}
