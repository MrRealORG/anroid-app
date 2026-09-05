package com.fbr.ntn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.fbr.ntn.BuildConfig
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.theme.*

@Composable
fun ConnectScreen(prefilledUrl: String, loading: Boolean, error: String?, onBack: () -> Unit, onConnect: (String, String, String, String) -> Unit) {
    var apiUrl by rememberSaveable { mutableStateOf(prefilledUrl.ifBlank { BuildConfig.API_BASE_URL }) }
    var username by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var pin by rememberSaveable { mutableStateOf("") }
    val valid = apiUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank() && pin.length >= 4

    Box(Modifier.fillMaxSize().background(Paper)) {
        SoftBackground()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ScreenHeader(
                "Connect workspace",
                "Enter the API details and the owner PIN to link this device with your invoicing database.",
                true, onBack
            )
            Card(enterDelay = 120) {
                LabelledField("API URL", apiUrl, { apiUrl = it }, "https://api.example.com/", KeyboardType.Uri, false)
                Spacer(Modifier.height(16.dp))
                LabelledField("Username", username, { username = it }, "e.g. ayesha.trading", KeyboardType.Text, false)
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
                AccentButton("Connect securely", valid, loading) { onConnect(apiUrl, username, password, pin) }
            }
            TrustFooter()
        }
    }
}

@Composable
private fun LabelledField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    keyboardType: KeyboardType,
    isError: Boolean
) {
    Text(label, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), color = InkMuted)
    Spacer(Modifier.height(8.dp))
    Field(value, onChange, placeholder, isError, keyboardType)
}
