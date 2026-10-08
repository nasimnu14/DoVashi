package com.example.dovashiapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.example.dovashiapp.navigation.AppNavHost

@Composable
@Preview
fun App() {
    MaterialTheme {
        AppNavHost()
    }
}
