package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.NayraMainScreen
import com.example.ui.NayraViewModel
import com.example.ui.theme.CyberVoid
import com.example.ui.theme.NayraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NayraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = CyberVoid
                ) {
                    val viewModel: NayraViewModel = viewModel()
                    NayraMainScreen(viewModel = viewModel)
                }
            }
        }
    }
}
