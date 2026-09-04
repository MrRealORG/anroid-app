package com.fbr.ntn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.fbr.ntn.ui.FbrNtnApp
import com.fbr.ntn.ui.theme.FbrNtnTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as FbrNtnApplication).container
        setContent { FbrNtnTheme { FbrNtnApp(container) } }
    }
}
