package com.fbr.ntn.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.fbr.ntn.BuildConfig
import com.fbr.ntn.model.AccountContext
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.theme.*
import kotlin.math.roundToInt

@Composable
private fun ScreenFrame(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
        content = content
    )
}

@Composable
fun NtnScreen(loading: Boolean, error: String?, onSubmit: (String) -> Unit) {
    var ntn by rememberSaveable { mutableStateOf("") }
    val valid = ntn.length in 7..13 && ntn.all(Char::isDigit)
    ScreenFrame {
        ScreenHeader("Verify your NTN", "Enter your National Tax Number to find the account linked with FBR.")
        GlassCard {
            Text("National Tax Number", style = MaterialTheme.typography.labelMedium, color = InkMuted)
            Spacer(Modifier.height(10.dp))
            GlassTextField(ntn, { ntn = it.filter(Char::isDigit).take(13) }, "e.g. 1234567", error != null, KeyboardType.Number)
            InlineMessage(error)
            if (BuildConfig.DEMO_MODE) Text("Demo mode: enter any 7–13 digit NTN", style = MaterialTheme.typography.labelMedium, color = InkMuted, modifier = Modifier.padding(top = 10.dp, start = 4.dp))
            Spacer(Modifier.height(22.dp))
            PrimaryButton("Check NTN", valid, loading) { onSubmit(ntn) }
        }
        TrustFooter()
    }
}

@Composable
fun MobileScreen(account: AccountContext, loading: Boolean, error: String?, onBack: () -> Unit, onSend: () -> Unit) {
    ScreenFrame {
        ScreenHeader("Confirm your mobile", "We'll send a one-time code to the registered number.", true, onBack)
        GlassCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(AccentTeal.copy(.12f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, null, tint = AccentTeal)
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text("NTN found", style = MaterialTheme.typography.labelMedium, color = AccentTeal)
                    Text(account.displayName, style = MaterialTheme.typography.titleLarge, color = Ink)
                    Text("NTN ${account.ntn}", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                }
            }
            Spacer(Modifier.height(26.dp))
            Text("Registered mobile", style = MaterialTheme.typography.labelMedium, color = InkMuted)
            Text(account.maskedMobile, style = MaterialTheme.typography.headlineMedium, color = Ink, modifier = Modifier.padding(top = 6.dp))
            InlineMessage(error)
            Spacer(Modifier.height(24.dp))
            PrimaryButton("Send code", true, loading, onSend)
        }
    }
}

@Composable
fun OtpScreen(account: AccountContext, verifying: Boolean, verified: Boolean, error: String?, seconds: Int, onBack: () -> Unit, onVerify: (String) -> Unit, onResend: () -> Unit) {
    var code by rememberSaveable { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val shake = remember { Animatable(0f) }
    LaunchedEffect(Unit) { focusRequester.requestFocus(); keyboard?.show() }
    LaunchedEffect(error) {
        if (error != null) {
            repeat(4) { shake.animateTo(if (it % 2 == 0) 9f else -9f, tween(55)) }
            shake.animateTo(0f, spring()); code = ""; focusRequester.requestFocus()
        }
    }
    LaunchedEffect(code) { if (code.length == 6) { keyboard?.hide(); onVerify(code) } }
    ScreenFrame {
        ScreenHeader("Enter your code", "We sent a 6-digit verification code to ${account.maskedMobile}.", true, onBack)
        GlassCard {
            AnimatedContent(verified, label = "verified") { success ->
                if (success) SuccessMark()
                else Column {
                    OtpInput(code, { if (!verifying) code = it.filter(Char::isDigit).take(6) }, focusRequester, Modifier.offset { IntOffset(shake.value.roundToInt(), 0) })
                    InlineMessage(error)
                    if (verifying) {
                        Spacer(Modifier.height(18.dp)); LoadingShimmer(Modifier.fillMaxWidth().height(7.dp))
                    }
                    if (BuildConfig.DEMO_MODE) Text("Demo code: 123456", style = MaterialTheme.typography.labelMedium, color = InkMuted, modifier = Modifier.padding(top = 12.dp))
                }
            }
            Spacer(Modifier.height(22.dp))
            TextButton(onClick = onResend, enabled = seconds == 0, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(if (seconds > 0) "Resend in 0:${seconds.toString().padStart(2, '0')}" else "Resend code", color = if (seconds > 0) InkMuted else AccentBlue)
            }
        }
    }
}

@Composable
private fun OtpInput(code: String, onChange: (String) -> Unit, requester: FocusRequester, modifier: Modifier = Modifier) {
    BasicTextField(
        value = code,
        onValueChange = onChange,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        cursorBrush = SolidColor(Color.Transparent),
        textStyle = TextStyle(color = Color.Transparent),
        modifier = modifier.fillMaxWidth().focusRequester(requester),
        decorationBox = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                repeat(6) { index ->
                    val active = index == code.length
                    Box(
                        Modifier.weight(1f).aspectRatio(.78f).clip(RoundedCornerShape(16.dp))
                            .background(Color.White.copy(.48f))
                            .border(1.dp, if (active) AccentBlue else Color.White.copy(.8f), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) { Text(code.getOrNull(index)?.toString() ?: "", fontSize = 22.sp, fontWeight = FontWeight.SemiBold, color = Ink) }
                }
            }
        }
    )
}

@Composable
private fun SuccessMark() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(76.dp).clip(CircleShape).background(AccentTeal), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Check, "Verified", tint = Color.White, modifier = Modifier.size(42.dp))
        }
        Spacer(Modifier.height(14.dp)); Text("Code verified", style = MaterialTheme.typography.titleLarge, color = Ink)
    }
}

@Composable
fun GlassTextField(value: String, onChange: (String) -> Unit, placeholder: String, isError: Boolean, keyboardType: KeyboardType) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value, onValueChange = onChange, singleLine = true,
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink), cursorBrush = SolidColor(AccentBlue),
        modifier = Modifier.fillMaxWidth().clip(CircleShape).background(Color.White.copy(.48f))
            .border(1.dp, when { isError -> ErrorRed; focused -> AccentBlue; else -> Color.White.copy(.8f) }, CircleShape).padding(horizontal = 20.dp, vertical = 17.dp),
        decorationBox = { inner -> Box { if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = InkMuted.copy(.7f)); inner() } }
    )
}

@Composable
private fun TrustFooter() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Lock, null, tint = InkMuted, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(6.dp))
        Text("Your information is encrypted and secure", style = MaterialTheme.typography.labelMedium, color = InkMuted)
    }
}
