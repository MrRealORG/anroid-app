package com.fbr.ntn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.theme.*

@Composable
fun ConnectScreen(prefilledUrl: String, loading: Boolean, error: String?, onBack: () -> Unit, onConnect: (String, String, String, String) -> Unit) {
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    val valid = prefilledUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank() && pin.length >= 4

    Box(Modifier.fillMaxSize().background(Paper)) {
        SoftBackground()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ScreenHeader(
                "Connect workspace",
                "Enter your credentials to link this device with your invoicing database.",
                true, onBack
            )
            Card(enterDelay = 120) {
                Text("Username", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = InkMuted)
                Spacer(Modifier.height(8.dp))
                Field(username, { username = it }, "e.g. admin", false, KeyboardType.Text)
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
                AccentButton("Connect securely", valid, loading) { onConnect(prefilledUrl, username, password, pin) }
            }
            TrustFooter()
        }
    }
}
