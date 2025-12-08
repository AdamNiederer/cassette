package com.example.cassette.presentation.views

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.example.cassette.R
import com.example.cassette.presentation.viewmodels.OnboardingViewModel
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState

@OptIn(ExperimentalHorologistApi::class)
@Composable
fun OnboardingView(
    onboardingViewModel: OnboardingViewModel,
    onComplete: () -> Unit
) {
    var currentPage by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    val hasPermission = remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_MEDIA_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission.value = isGranted
        if (isGranted) {
            currentPage = 1
        }
    }

    val columnState = rememberResponsiveColumnState()

    Box(modifier = Modifier.fillMaxSize()) {
        when (currentPage) {
            0 -> {
                ScalingLazyColumn(
                    columnState = columnState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Welcome!",
                            style = MaterialTheme.typography.title3,
                            textAlign = TextAlign.Center,
                        )
                    }
                    item {
                        Text(
                            text = "Please grant permission to access your music",
                            style = MaterialTheme.typography.body2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp),
                        )
                    }
                    item {
                        Chip(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_music),
                                    contentDescription = "Music Icon",
                                    modifier = Modifier.size(ButtonDefaults.DefaultIconSize)
                                )
                            },
                            label = {
                                Text("Grant")
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = ChipDefaults.secondaryChipColors(),
                        )
                    }
                }
            }
            1 -> {
                ScalingLazyColumn(
                    columnState = columnState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    item {
                        Text(
                            text = "Ready to Scan",
                            style = MaterialTheme.typography.title3,
                            textAlign = TextAlign.Center,
                        )
                    }
                    item {
                        Text(
                            text = "Scan for music on your device. This may take a while.",
                            style = MaterialTheme.typography.body2,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    item {
                        Chip(
                            onClick = {
                                onboardingViewModel.rescan()
                                onboardingViewModel.setOnboardingCompleted(true)
                                onComplete()
                            },
                            icon = {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_refresh),
                                    contentDescription = "Refresh Icon",
                                    modifier = Modifier.size(ButtonDefaults.DefaultIconSize)
                                )
                            },
                            label = {
                                Text("Scan")
                            },
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = ChipDefaults.secondaryChipColors()
                        )
                    }
                }
            }
        }
    }

    LaunchedEffect(hasPermission.value) {
        if (hasPermission.value && currentPage == 0) {
            currentPage = 1
        }
    }
}
