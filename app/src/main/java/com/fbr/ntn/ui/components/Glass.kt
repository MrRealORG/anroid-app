package com.fbr.ntn.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fbr.ntn.R
import com.fbr.ntn.ui.sound.SoundFx
import com.fbr.ntn.ui.theme.*

private val CardShape = RoundedCornerShape(28.dp)
private val ButtonShape = RoundedCornerShape(22.dp)

/** Flat paper background used by every screen, like the invoice reference. */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Paper)) {
        SoftBackground()
        content()
    }
}

/** Clean iOS-style backdrop: subtle top-to-bottom gradient, nothing else. */
@Composable
fun SoftBackground(modifier: Modifier = Modifier) {
    Box(
        modifier.fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (ThemeMode.dark) Color(0xFF111318) else Color(0xFFF0F0F5),
                        if (ThemeMode.dark) Color(0xFF0B0B0F) else Paper
                    )
                )
            )
    )
}

/** White rounded card with hairline border, soft shadow, and staggered entrance. */
@Composable
fun Card(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(20.dp),
    enterDelay: Int = 0,
    animate: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    var shown by remember { mutableStateOf(!animate) }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(450, delayMillis = enterDelay), label = "card alpha")
    val slide by animateFloatAsState(if (shown) 0f else 26f, tween(500, delayMillis = enterDelay), label = "card slide")
    if (animate) LaunchedEffect(Unit) { shown = true }
    Column(
        modifier.graphicsLayer { translationY = slide; this.alpha = alpha }
            .shadow(12.dp, CardShape, spotColor = Color.Black.copy(alpha = .10f))
            .clip(CardShape)
            .background(CardWhite)
            .border(1.dp, Line, CardShape)
            .padding(contentPadding),
        content = content
    )
}

/** Apple-soft primary CTA: 20dp rounded rect, white semibold text, soft blue glow. */
@Composable
fun AccentButton(text: String, enabled: Boolean = true, loading: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "accent scale")
    Button(
        onClick = { SoundFx.click(); onClick() },
        enabled = enabled && !loading,
        interactionSource = interactions,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Accent,
            contentColor = Color.White,
            disabledContainerColor = Track,
            disabledContentColor = InkMuted
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 10.dp, pressedElevation = 4.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)
            .shadow(10.dp, ButtonShape, spotColor = Accent.copy(alpha = .40f))
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        if (loading) LoadingShimmer(Modifier.width(110.dp).height(18.dp), dark = true)
        else Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}

/** Apple-soft secondary button: 20dp rounded rect on ink. */
@Composable
fun PrimaryButton(text: String, enabled: Boolean = true, loading: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .97f else 1f, spring(stiffness = Spring.StiffnessMediumLow), label = "primary scale")
    Button(
        onClick = { SoundFx.click(); onClick() },
        enabled = enabled && !loading,
        interactionSource = interactions,
        shape = ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = Ink,
            contentColor = if (ThemeMode.dark) Color(0xFF111318) else Color.White,
            disabledContainerColor = Track,
            disabledContentColor = InkMuted
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp)
            .shadow(6.dp, ButtonShape, spotColor = Color.Black.copy(alpha = .25f))
            .graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        if (loading) LoadingShimmer(Modifier.width(110.dp).height(18.dp))
        else Text(text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
fun LoadingShimmer(modifier: Modifier = Modifier, dark: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val x by transition.animateFloat(-300f, 700f, infiniteRepeatable(tween(900), RepeatMode.Restart), label = "shimmer x")
    val base = if (dark) Color.Black.copy(.10f) else Color.White.copy(.35f)
    val peak = if (dark) Color.Black.copy(.22f) else Color.White.copy(.85f)
    Box(modifier.clip(CircleShape).background(Brush.linearGradient(listOf(base, peak, base), Offset(x, 0f), Offset(x + 300, 100f))))
}

/** Large bold title with a circular back button, invoice-app style. */
@Composable
fun ScreenHeader(title: String, body: String, canGoBack: Boolean = false, onBack: () -> Unit = {}) {
    var shown by remember { mutableStateOf(false) }
    val alpha by animateFloatAsState(if (shown) 1f else 0f, tween(400), label = "header alpha")
    val slide by animateFloatAsState(if (shown) 0f else 22f, tween(450), label = "header slide")
    LaunchedEffect(Unit) { shown = true }
    Column(Modifier.graphicsLayer { translationY = slide; this.alpha = alpha }, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (canGoBack) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(44.dp).clip(CircleShape).background(CardWhite).border(1.dp, Line, CircleShape)
            ) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Go back", tint = Ink)
            }
        } else Spacer(Modifier.height(28.dp))
        Text(title, style = MaterialTheme.typography.headlineMedium, color = Ink)
        Text(body, style = MaterialTheme.typography.bodyLarge, color = InkMuted)
    }
}

/** White text field with hairline border. */
@Composable
fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    isError: Boolean,
    keyboardType: KeyboardType
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
        cursorBrush = SolidColor(Ink),
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .border(
                width = if (focused && !isError) 2.dp else 1.dp,
                color = when {
                    isError -> ErrorRed
                    focused -> Ink
                    else -> Line
                },
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 20.dp, vertical = 17.dp),
        decorationBox = { inner ->
            Box {
                if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = InkMuted.copy(.7f))
                inner()
            }
        }
    )
}

/** Segmented control like All / Open / Paid / Templates in the reference. */
@Composable
fun SegmentedTabs(options: List<String>, selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Track)
            .padding(4.dp)
    ) {
        options.forEachIndexed { index, label ->
            val isSelected = index == selected
            Box(
                Modifier.weight(1f)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (isSelected) CardWhite else Color.Transparent)
                    .clickable { SoundFx.tab(); onSelect(index) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    ),
                    color = if (isSelected) Ink else InkMuted
                )
            }
        }
    }
}

/** Accent hint pill, like "Template was created" in the reference. */
@Composable
fun AccentPill(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(Accent).padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = Color.White)
    }
}

@Composable
fun InlineMessage(message: String?) {
    if (message != null) {
        Text(
            message,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Medium),
            color = ErrorInk,
            modifier = Modifier.padding(top = 10.dp, start = 4.dp)
        )
    }
}

@Composable
fun TrustFooter() {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Lock, null, tint = InkMuted, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text("Your information is encrypted and secure", style = MaterialTheme.typography.labelMedium, color = InkMuted)
    }
}

/** Subtle dot-grid pattern backdrop used to add texture to headers and splash. */
@Composable
fun DotPattern(modifier: Modifier = Modifier, strength: Float = 1f) {
    Canvas(modifier.fillMaxSize().alpha(strength)) {
        val step = 30.dp.toPx()
        val radius = 1.8.dp.toPx()
        var y = step / 2
        while (y < size.height) {
            var x = step / 2
            while (x < size.width) {
                drawCircle(Color.Black.copy(alpha = .055f), radius, Offset(x, y))
                x += step
            }
            y += step
        }
    }
}

/** Zeeno brand mark: vivid #0700FF tile with a bold white Z and soft blue glow. */
@Composable
fun ZeenoMark(size: Dp = 54.dp, fontSize: TextUnit = 28.sp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size)
            .shadow(14.dp, RoundedCornerShape(26.dp), spotColor = Accent.copy(alpha = .45f))
            .clip(RoundedCornerShape(26.dp))
            .background(Accent),
        contentAlignment = Alignment.Center
    ) {
        Text("Z", color = Color.White, fontSize = fontSize, fontWeight = FontWeight.Black)
    }
}

/** Official FBR logo (asset in drawable/fbr_logo.jpg). */
@Composable
fun FbrLogo(size: Dp = 54.dp) {
    Box(
        Modifier.size(size)
            .clip(RoundedCornerShape(17.dp))
            .background(CardWhite)
            .border(1.dp, Line, RoundedCornerShape(17.dp))
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.fbr_logo),
            contentDescription = "FBR logo",
            contentScale = ContentScale.Fit
        )
    }
}

/** Password field with show/hide toggle. */
@Composable
fun PasswordField(value: String, onChange: (String) -> Unit, placeholder: String = "••••••••") {
    var visible by rememberSaveable { mutableStateOf(false) }
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        interactionSource = interaction,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (visible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
        cursorBrush = SolidColor(Ink),
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .border(
                width = if (focused) 2.dp else 1.dp,
                color = if (focused) Ink else Line,
                shape = RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 20.dp, vertical = 10.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.weight(1f).padding(vertical = 7.dp)) {
                    if (value.isEmpty()) Text(placeholder, style = MaterialTheme.typography.bodyLarge, color = InkMuted.copy(.7f))
                    inner()
                }
                IconButton(onClick = { SoundFx.click(); visible = !visible }, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = if (visible) "Hide password" else "Show password",
                        tint = InkMuted,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    )
}

/** Rounded white search field with a magnifier icon. */
@Composable
fun SearchField(value: String, onChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
        cursorBrush = SolidColor(Ink),
        modifier = Modifier.fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .border(1.dp, Line, RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        decorationBox = { inner ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Search, contentDescription = null, tint = InkMuted, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f)) {
                    if (value.isEmpty()) Text("Search invoices…", style = MaterialTheme.typography.bodyLarge, color = InkMuted.copy(.7f))
                    inner()
                }
            }
        }
    )
}
