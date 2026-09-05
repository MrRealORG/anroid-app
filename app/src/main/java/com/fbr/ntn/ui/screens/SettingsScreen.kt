package com.fbr.ntn.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fbr.ntn.BuildConfig
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.sound.SoundFx
import com.fbr.ntn.ui.theme.*

private val ThemeOptions = listOf("System", "Light", "Dark")

/** iOS-style settings: grouped rows, segmented appearance control, switches. */
@Composable
fun SettingsScreen(
    themeMode: String,
    soundsEnabled: Boolean,
    accountName: String?,
    onThemeMode: (String) -> Unit,
    onSounds: (Boolean) -> Unit,
    onBack: () -> Unit,
    onSwitchAccount: () -> Unit
) {
    Box(Modifier.fillMaxSize().background(Paper)) {
        SoftBackground()
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding(),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item {
                ScreenHeader("Settings", "Appearance, sounds and account.", true, onBack)
                Spacer(Modifier.height(8.dp))
            }
            item {
                SectionLabel("APPEARANCE")
                Spacer(Modifier.height(8.dp))
            }
            item {
                GroupedRow {
                    SegmentedTabs(
                        ThemeOptions,
                        selected = when (themeMode) { "light" -> 1; "dark" -> 2; else -> 0 },
                        onSelect = { onThemeMode(listOf("system", "light", "dark")[it]) }
                    )
                }
                Spacer(Modifier.height(4.dp))
                Box(Modifier.padding(horizontal = 28.dp)) {
                    Text(
                        "Dark saves battery on OLED and is easier on the eyes.",
                        style = MaterialTheme.typography.labelMedium, color = InkMuted
                    )
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                SectionLabel("SOUNDS")
                Spacer(Modifier.height(8.dp))
            }
            item {
                GroupedRow {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(34.dp).clip(CircleShape).background(AccentSoft), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.VolumeUp, null, tint = AccentDeep, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Interface sounds", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = Ink)
                            Text("Clicks and confirmations", style = MaterialTheme.typography.labelMedium, color = InkMuted)
                        }
                        Switch(
                            checked = soundsEnabled,
                            onCheckedChange = { SoundFx.click(); onSounds(it) },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = Accent,
                                checkedThumbColor = Color.White,
                                uncheckedTrackColor = Track
                            )
                        )
                    }
                }
            }
            item {
                Spacer(Modifier.height(20.dp))
                SectionLabel("ACCOUNT")
                Spacer(Modifier.height(8.dp))
            }
            if (accountName != null) {
                item {
                    GroupedRow {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(34.dp).clip(CircleShape).background(Tile), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Person, null, tint = Ink, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(accountName, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = Ink)
                                Text("Signed in on this device", style = MaterialTheme.typography.labelMedium, color = InkMuted)
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = InkMuted, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            item {
                GroupedRow {
                    Row(
                        Modifier.fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ErrorTint)
                            .clickable { SoundFx.click(); onSwitchAccount() }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Logout, null, tint = ErrorInk, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text("Switch account", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = ErrorInk)
                    }
                }
            }
            item {
                Spacer(Modifier.height(28.dp))
                Text(
                    "Zeeno SmartDi v${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.labelMedium, color = InkMuted,
                    modifier = Modifier.padding(horizontal = 28.dp)
                )
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
        color = InkMuted,
        modifier = Modifier.padding(horizontal = 28.dp)
    )
}

@Composable
private fun GroupedRow(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.padding(horizontal = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CardWhite)
            .border(0.5.dp, Line, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        content = content
    )
}
