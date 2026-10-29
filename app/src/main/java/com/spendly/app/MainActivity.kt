package com.spendly.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
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
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.spendly.app.core.theme.SpendlyTheme
import com.spendly.app.core.theme.ThemeMode
import com.spendly.app.navigation.SpendlyNavGraph

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as SpendlyApp
        val container = app.container

        setContent {
            val currentTheme by container.userPreferences.themeModeFlow.collectAsState(initial = ThemeMode.DARK)
            val navController = rememberNavController()

            // Native Android default permission prompt on fresh app open
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { /* runtime permissions updated */ }

            LaunchedEffect(Unit) {
                val hasSms = ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.READ_SMS
                ) == PackageManager.PERMISSION_GRANTED

                if (!hasSms) {
                    val perms = buildList {
                        add(Manifest.permission.READ_SMS)
                        add(Manifest.permission.RECEIVE_SMS)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }.toTypedArray()
                    permissionLauncher.launch(perms)
                }
            }

            SpendlyTheme(themeMode = currentTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = SpendlyTheme.colors.background
                ) {
                    SpendlyNavGraph(
                        navController = navController,
                        userPreferences = container.userPreferences,
                        repository = container.repository,
                        currentTheme = currentTheme
                    )
                }
            }
        }
    }
}
