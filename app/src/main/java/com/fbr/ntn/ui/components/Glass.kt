package com.fbr.ntn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import com.fbr.ntn.ui.theme.*

val LocalHazeState = staticCompositionLocalOf<HazeState> { error("No HazeState") }

@Composable
fun GlassBackground(content: @Composable BoxScope.() -> Unit) {
    val hazeState = rememberHazeState()
    CompositionLocalProvider(LocalHazeState provides hazeState) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.linearGradient(
                    listOf(BackgroundBlue, BackgroundLilac, Color(0xFFE8FAF7)),
                    start = Offset.Zero,
                    end = Offset(1100f, 1900f)
                )
            ).hazeSource(hazeState)
        ) {
            Box(Modifier.size(280.dp).offset(x = (-90).dp, y = 120.dp).background(Color(0x336DA2FF), CircleShape))
            Box(Modifier.size(220.dp).align(Alignment.BottomEnd).offset(x = 70.dp, y = (-80).dp).background(Color(0x3300C2A8), CircleShape))
            content()
        }
    }
}

@Composable
fun Modifier.glass(shape: Shape = RoundedCornerShape(24.dp)): Modifier = this
    .shadow(18.dp, shape, ambientColor = Color(0x220A2A66), spotColor = Color(0x260A2A66))
    .clip(shape)
    .hazeEffect(LocalHazeState.current) {
        blurRadius = 24.dp
        backgroundColor = Color.White.copy(alpha = .56f)
        tints = listOf(HazeTint(Color.White.copy(alpha = .16f)))
        noiseFactor = .04f
    }
    .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(.82f), Color.White.copy(.12f))), shape)

@Composable
fun GlassCard(modifier: Modifier = Modifier, contentPadding: PaddingValues = PaddingValues(22.dp), content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.glass().padding(contentPadding), content = content)
}

@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, loading: Boolean = false, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "button scale")
    Button(
        onClick = onClick,
        enabled = enabled && !loading,
        interactionSource = interactions,
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(containerColor = AccentBlue, disabledContainerColor = AccentBlue.copy(.32f)),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        if (loading) LoadingShimmer(Modifier.width(110.dp).height(18.dp)) else Text(text, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
fun LoadingShimmer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(-300f, 700f, infiniteRepeatable(tween(900), RepeatMode.Restart), label = "shimmer x")
    Box(modifier.clip(CircleShape).background(Brush.linearGradient(listOf(Color.White.copy(.18f), Color.White.copy(.8f), Color.White.copy(.18f)), Offset(x, 0f), Offset(x + 300, 100f))))
}

@Composable
fun ScreenHeader(title: String, body: String, canGoBack: Boolean = false, onBack: () -> Unit = {}) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (canGoBack) {
            IconButton(onClick = onBack, modifier = Modifier.offset(x = (-12).dp)) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Go back", tint = Ink)
            }
        } else Spacer(Modifier.height(36.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = InkMuted)
    }
}

@Composable
fun InlineMessage(message: String?, error: Boolean = true) {
    if (message != null) Text(message, style = MaterialTheme.typography.labelMedium, color = if (error) ErrorRed else AccentTeal, modifier = Modifier.padding(top = 8.dp, start = 4.dp))
}
