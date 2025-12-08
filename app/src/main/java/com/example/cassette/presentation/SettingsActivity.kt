package com.example.cassette.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cassette.presentation.viewmodels.SettingsViewModel
import com.example.cassette.presentation.views.SettingsView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(android.R.style.Theme_DeviceDefault)
        
        setContent {
            val viewModel = viewModel<SettingsViewModel>()
            SettingsView(viewModel)
        }
    }
}
