package com.fbr.ntn.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fbr.ntn.R
import com.fbr.ntn.ui.components.SoftBackground
import com.fbr.ntn.ui.components.LoadingShimmer
import com.fbr.ntn.ui.theme.*

@Composable
fun SplashScreen() {
    var entered by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(if (entered) 1f else .8f, spring(dampingRatio = .65f, stiffness = 200f), label = "logo scale")
    val logoAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(500), label = "logo alpha")
    val textAlpha by animateFloatAsState(if (entered) 1f else 0f, tween(450, delayMillis = 150), label = "text alpha")
    val textSlide by animateFloatAsState(if (entered) 0f else 24f, spring(dampingRatio = .75f, stiffness = 180f), label = "text slide")
    LaunchedEffect(Unit) { entered = true }
    Box(Modifier.fillMaxSize().background(Paper)) {
        SoftBackground()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.graphicsLayer { scaleX = logoScale; scaleY = logoScale; alpha = logoAlpha },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    Modifier.shadow(8.dp, RoundedCornerShape(18.dp), spotColor = Color.Black.copy(alpha = .06f))
                        .clip(RoundedCornerShape(18.dp))
                        .background(CardWhite)
                        .border(1.dp, Line, RoundedCornerShape(18.dp))
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.fbr_logo),
                        contentDescription = "FBR logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(48.dp)
                    )
                }
                Spacer(Modifier.width(16.dp))
                Box(Modifier.width(1.5.dp).height(48.dp).background(Line))
                Spacer(Modifier.width(16.dp))
                Box(
                    Modifier.shadow(12.dp, RoundedCornerShape(18.dp), spotColor = Accent.copy(alpha = .4f))
                        .clip(RoundedCornerShape(18.dp))
                        .background(Accent)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Z", color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.graphicsLayer { translationY = textSlide; alpha = textAlpha },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Zeeno SmartDi",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Ink,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "ZEENO SOFT × FBR",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = AccentDeep,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Federal Board of Revenue • Digital Invoicing",
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkMuted,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(28.dp))
                LoadingShimmer(Modifier.width(90.dp).height(8.dp), dark = true)
            }
        }
    }
}
