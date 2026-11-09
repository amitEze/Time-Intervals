package com.amiteze.intervaltimer.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

/** Root composable: picks the theme and the current screen. */
@Composable
fun App(vm: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val dark = isSystemInDarkTheme()
    val colors = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme()
        else -> lightColorScheme()
    }

    val screen by vm.screen.collectAsStateWithLifecycle()

    MaterialTheme(colorScheme = colors) {
        when (screen) {
            Screen.LIST -> WorkoutListScreen(vm)
            Screen.EDIT -> {
                BackHandler { vm.cancelEdit() }
                EditorScreen(vm)
            }
            Screen.RUN -> {
                BackHandler { vm.go(Screen.LIST) } // the timer keeps running
                RunScreen(vm)
            }
            Screen.SETTINGS -> {
                BackHandler { vm.go(Screen.LIST) }
                SettingsScreen(vm)
            }
        }
    }
}
