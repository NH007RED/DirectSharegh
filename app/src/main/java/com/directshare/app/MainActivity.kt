package com.directshare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.directshare.app.permissions.PermissionHelper
import com.directshare.app.ui.navigation.DirectShareNavHost
import com.directshare.app.ui.screens.DirectShareViewModel
import com.directshare.app.ui.theme.DirectShareTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val viewModel: DirectShareViewModel = hiltViewModel()
            val settings by viewModel.settings.collectAsState()

            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { _ -> }

            LaunchedEffect(Unit) {
                val needed = PermissionHelper.requiredRuntimePermissions()
                if (needed.isNotEmpty() && !PermissionHelper.hasNotificationPermission(this@MainActivity)) {
                    permissionLauncher.launch(needed.toTypedArray())
                }
            }

            DirectShareTheme(themePreference = settings.themePreference) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DirectShareNavHost(viewModel = viewModel)
                }
            }
        }
    }
}
