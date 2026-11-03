package edu.sdwu.teaching

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import edu.sdwu.teaching.domain.model.ThemeMode
import edu.sdwu.teaching.ui.AppViewModelProvider
import edu.sdwu.teaching.ui.MainViewModel
import edu.sdwu.teaching.ui.navigation.TeachingAppRoot
import edu.sdwu.teaching.ui.theme.TeachingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MainViewModel = viewModel(factory = AppViewModelProvider.Factory)
            val prefs = viewModel.preferences.collectAsStateWithLifecycle().value
                ?: return@setContent // Wait for DataStore (a few ms) instead of flashing the wrong theme.

            val darkTheme = when (prefs.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            // Status/navigation bar icons must follow the app theme, not only the system setting.
            LaunchedEffect(darkTheme) {
                val style = SystemBarStyle.auto(
                    lightScrim = android.graphics.Color.TRANSPARENT,
                    darkScrim = android.graphics.Color.TRANSPARENT,
                ) { darkTheme }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            TeachingTheme(darkTheme = darkTheme, dynamicColor = prefs.dynamicColor) {
                TeachingAppRoot()
            }
        }
    }
}
