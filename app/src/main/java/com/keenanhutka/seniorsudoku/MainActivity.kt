package com.keenanhutka.seniorsudoku

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.keenanhutka.seniorsudoku.ui.HomeScreen
import com.keenanhutka.seniorsudoku.ui.SeniorSudokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The paper stays light even when the phone is in dark mode.
        // Android 7 cannot draw dark navigation icons, so that bar is dark instead.
        val lightBars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        val navigation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            lightBars
        } else {
            SystemBarStyle.dark(Color.parseColor("#1C1812"))
        }
        enableEdgeToEdge(statusBarStyle = lightBars, navigationBarStyle = navigation)
        setContent {
            SeniorSudokuTheme {
                HomeScreen()
            }
        }
    }
}