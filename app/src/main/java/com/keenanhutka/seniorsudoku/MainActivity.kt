package com.keenanhutka.seniorsudoku

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.keenanhutka.seniorsudoku.ui.HomeScreen
import com.keenanhutka.seniorsudoku.ui.SeniorSudokuTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SeniorSudokuTheme {
                HomeScreen()
            }
        }
    }
}
