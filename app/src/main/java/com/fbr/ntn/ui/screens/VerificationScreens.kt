package com.fbr.ntn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fbr.ntn.model.AccountContext
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.sound.SoundFx
import com.fbr.ntn.ui.theme.*
import kotlin.math.roundToInt

@Composable
private fun ScreenFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
        content = content
    )
}

@Composable
fun NtnScreen(loading: Boolean, error: String?, onSubmit: (String) -> Unit, onConnect: () -> Unit = {}) {
    var ntn by rememberSaveable { mutableStateOf("") }
    val valid = ntn.length in 7..13 && ntn.all(Char::isDigit)
    LaunchedEffect(error) { if (error != null) SoundFx.error() }
    ScreenFrame {
        ScreenHeader("Verify your NTN", "Enter your National Tax Number to find the account linked with FBR.")
        Card(enterDelay = 120) {
            Text("National Tax Number", style = MaterialTheme.typography.labelLarge, color = InkMuted)
            Spacer(Modifier.height(10.dp))
            Field(ntn, { ntn = it.filter(Char::isDigit).take(13) }, "e.g. 1234567", error != null, KeyboardType.Number)
            InlineMessage(error)
            Spacer(Modifier.height(22.dp))
            AccentButton("Check NTN", valid, loading) { onSubmit(ntn) }
        }
        TextButton(onClick = onConnect, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text("Connect workspace", color = AccentDeep, fontWeight = FontWeight.Bold)
        }
        TrustFooter()
    }
}

@Composable
fun PinScreen(ntn: String, loading: Boolean, error: String?, onBack: () -> Unit, onVerify: (String) -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    val valid = pin.length >= 4 && pin.all(Char::isDigit)
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) { focusRequester.requestFocus(); keyboard?.show(); SoundFx.send() }
    LaunchedEffect(error) {
        if (error != null) {
            SoundFx.error()
            repeat(4) { shake.animateTo(if (it % 2 == 0) 9f else -9f, tween(55)) }
            shake.animateTo(0f, spring()); pin = ""; focusRequester.requestFocus()
        }
    }
    LaunchedEffect(pin) { if (pin.length == 4 && valid) { keyboard?.hide(); onVerify(pin) } }
    ScreenFrame {
        ScreenHeader("Enter PIN", "Confirm your identity with your 4-digit PIN.", true, onBack)
        Card(enterDelay = 120) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Lock, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("NTN", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = AccentDeep)
                    Text(ntn, style = MaterialTheme.typography.titleLarge, color = Ink)
                }
            }
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(color = Line)
            Spacer(Modifier.height(20.dp))
            Text("PIN Code", style = MaterialTheme.typography.labelLarge, color = InkMuted)
            PinInput(pin, { if (!loading) pin = it.filter(Char::isDigit).take(4) }, focusRequester, Modifier.offset { IntOffset(shake.value.roundToInt(), 0) })
            InlineMessage(error)
            Spacer(Modifier.height(22.dp))
            AccentButton("Verify & Login", valid, loading) { onVerify(pin) }
        }
        TrustFooter()
    }
}

@Composable
private fun PinInput(pin: String, onChange: (String) -> Unit, requester: FocusRequester, modifier: Modifier = Modifier) {
    BasicTextField(
        value = pin,
        onValueChange = onChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = TextStyle(color = Color.Transparent),
        modifier = modifier.fillMaxWidth().focusRequester(requester),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(4) { index ->
                    val active = index == pin.length
                    Box(
                        Modifier.weight(1f).aspectRatio(.78f).clip(RoundedCornerShape(16.dp))
                            .background(CardWhite)
                            .border(
                                width = if (active) 2.dp else 1.dp,
                                color = if (active) Ink else Line,
                                shape = RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) { Text(pin.getOrNull(index)?.toString() ?: "", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Ink) }
                }
            }
        }
    )
}
