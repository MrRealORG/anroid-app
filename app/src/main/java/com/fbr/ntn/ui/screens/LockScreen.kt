package com.fbr.ntn.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.fbr.ntn.R
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.theme.*

@Composable
fun LockScreen(
    savedUsername: String?,
    displayName: String?,
    loading: Boolean,
    error: String?,
    onUnlock: (String, String, String) -> Unit,
    onSwitchAccount: () -> Unit
) {
    var username by rememberSaveable(savedUsername) { mutableStateOf(savedUsername.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    val valid = username.isNotBlank() && password.isNotBlank() && pin.length >= 4

    Box(Modifier.fillMaxSize().background(Paper)) {
        SoftBackground()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.shadow(12.dp, RoundedCornerShape(22.dp), ambientColor = Color(0x14000000), spotColor = Color(0x1A000000))
                    .clip(RoundedCornerShape(22.dp))
                    .background(CardWhite)
                    .border(1.dp, Line, RoundedCornerShape(22.dp))
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.fbr_mark),
                    contentDescription = "FBR Pakistan logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(44.dp).aspectRatio(173f / 68f)
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Welcome back", style = MaterialTheme.typography.headlineMedium, color = Ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (displayName != null) "$displayName, enter your login to unlock" else "Enter your login to unlock your invoices",
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkMuted,
                    textAlign = TextAlign.Center
                )
            }
            Card(enterDelay = 120) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, null, tint = AccentDeep, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Device locked", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                }
                Spacer(Modifier.height(16.dp))
                Text("Username", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = InkMuted)
                Spacer(Modifier.height(8.dp))
                Field(username, { username = it }, "e.g. ayesha.trading", false, KeyboardType.Text)
                Spacer(Modifier.height(16.dp))
                Text("Password", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = InkMuted)
                Spacer(Modifier.height(8.dp))
                PasswordField(password, { password = it })
                Spacer(Modifier.height(16.dp))
                Text("Owner PIN", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = InkMuted)
                Spacer(Modifier.height(8.dp))
                Field(pin, { pin = it.filter(Char::isDigit).take(6) }, "4-digit PIN", error != null, KeyboardType.NumberPassword)
                InlineMessage(error)
                Spacer(Modifier.height(22.dp))
                AccentButton("Unlock", valid, loading) { onUnlock(username, password, pin) }
            }
            TextButton(onClick = onSwitchAccount) {
                Text("Use a different account", color = InkMuted, fontWeight = FontWeight.Bold)
            }
        }
    }
}
