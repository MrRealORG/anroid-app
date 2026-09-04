package com.fbr.ntn.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.fbr.ntn.ui.theme.*

@Composable
fun SplashScreen() {
    var entered by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(if (entered) 1f else .72f, spring(dampingRatio = .62f, stiffness = 260f), label = "logo settle")
    val alpha by animateFloatAsState(if (entered) 1f else 0f, tween(350), label = "logo fade")
    LaunchedEffect(Unit) { entered = true }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.graphicsLayer { scaleX = scale; scaleY = scale; this.alpha = alpha }.size(92.dp).shadow(22.dp, RoundedCornerShape(28.dp)).clip(RoundedCornerShape(28.dp)).background(AccentBlue), contentAlignment = Alignment.Center) {
            Text("F", color = Color.White, style = MaterialTheme.typography.headlineMedium)
        }
        Spacer(Modifier.height(22.dp)); Text("FBR NTN", style = MaterialTheme.typography.headlineMedium, color = Ink)
        Text("Secure verification", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
    }
}
