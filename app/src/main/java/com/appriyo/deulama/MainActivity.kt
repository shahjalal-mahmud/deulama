package com.appriyo.deulama

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.appriyo.deulama.presentation.navigation.HangugNavGraph
import com.appriyo.deulama.ui.theme.HangugDeulamaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HangugDeulamaTheme {
                HangugNavGraph()
            }
        }
    }
}