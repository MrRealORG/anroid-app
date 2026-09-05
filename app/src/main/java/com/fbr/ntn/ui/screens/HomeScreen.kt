package com.fbr.ntn.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fbr.ntn.R
import com.fbr.ntn.model.*
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.sound.SoundFx
import com.fbr.ntn.ui.theme.*
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import kotlinx.coroutines.delay
private val Tabs = listOf("All", "Validate", "Post")
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    account: AccountContext?,
    items: List<PendingItem>,
    loading: Boolean,
    refreshing: Boolean,
    error: String?,
    onRefresh: () -> Unit,
    onRetry: () -> Unit,
    onOpen: (String) -> Unit,
    onLock: () -> Unit,
    onValidate: (String) -> Unit,
    onPost: (String) -> Unit,
    onSettings: () -> Unit
) {
    val context = LocalContext.current
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var settledQuery by remember { mutableStateOf("") }
    LaunchedEffect(query) { delay(300); settledQuery = query }
    var startDate by rememberSaveable { mutableStateOf("") }
    var endDate by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var datePickerMode by rememberSaveable { mutableStateOf("start") }
    var confirmTarget by remember { mutableStateOf<PendingItem?>(null) }
    val today = remember { LocalDate.now(ZoneId.systemDefault()) }
    val visible = remember(tab, settledQuery, items, startDate, endDate) {
        val q = settledQuery.trim().lowercase()
        items
            .filter {
                when (tab) {
                    1 -> it.status == PendingStatus.VALIDATE
                    2 -> it.status == PendingStatus.POSTED
                    else -> true
                }
            }
            .filter {
                if (startDate.isBlank() && endDate.isBlank()) return@filter true
                val itemDate = runCatching { LocalDate.parse(it.date, DateTimeFormatter.ofPattern("dd MMM yyyy")) }.getOrNull()
                if (itemDate == null) return@filter true
                if (startDate.isNotBlank()) { val sd = runCatching { LocalDate.parse(startDate) }.getOrNull(); if (sd != null && sd.isAfter(itemDate)) return@filter false }
                if (endDate.isNotBlank()) { val ed = runCatching { LocalDate.parse(endDate) }.getOrNull(); if (ed != null && ed.isBefore(itemDate)) return@filter false }
                true
            }
            .filter {
                if (q.isBlank()) true
                else (it.title + " " + it.number + " " + it.client).lowercase().contains(q)
            }
    }
    val listState = rememberLazyListState()
    val showTopBar by remember { derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40 } }
    val topBarAlpha by animateFloatAsState(if (showTopBar) 1f else 0f, label = "top bar")
    val greeting = remember {
        when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good morning"
            in 12..16 -> "Good afternoon"
            in 17..20 -> "Good evening"
            else -> "Welcome back"
        }
    }
    Box(Modifier.fillMaxSize().background(Paper)) {
        SoftBackground()
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            indicator = {
                if (refreshing) LoadingShimmer(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 78.dp).width(90.dp).height(10.dp), dark = true)
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 116.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text(greeting, style = MaterialTheme.typography.bodyLarge, color = InkMuted)
                    Text(account?.displayName ?: "Your account", style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Text("Digital invoicing", style = MaterialTheme.typography.labelMedium, color = InkMuted)
                    Spacer(Modifier.height(20.dp))
                    SearchField(query) { query = it }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedButton(
                            onClick = { datePickerMode = "start"; showDatePicker = true },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, null, tint = InkMuted, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(startDate.ifBlank { "Start" }, style = MaterialTheme.typography.labelMedium, color = InkMuted)
                        }
                        Text("→", style = MaterialTheme.typography.labelLarge, color = InkMuted)
                        OutlinedButton(
                            onClick = { datePickerMode = "end"; showDatePicker = true },
                            shape = CircleShape,
                            modifier = Modifier.weight(1f).heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Rounded.CalendarMonth, null, tint = InkMuted, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(endDate.ifBlank { "End" }, style = MaterialTheme.typography.labelMedium, color = InkMuted)
                        }
                    }
                    if (startDate.isNotBlank() || endDate.isNotBlank()) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Filtered: ${if (startDate.isNotBlank()) startDate else "—"} to ${if (endDate.isNotBlank()) endDate else "—"}", style = MaterialTheme.typography.labelMedium, color = AccentDeep)
                            TextButton(onClick = { startDate = ""; endDate = "" }) { Text("Clear", color = AccentDeep, fontWeight = FontWeight.Bold) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Invoices", style = MaterialTheme.typography.titleLarge, color = Ink)
                        Surface(shape = CircleShape, color = Ink) {
                            Text(
                                items.size.toString(),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 13.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    SegmentedTabs(Tabs, tab) { tab = it }
                }
                if (loading) items(3) { PendingSkeleton(Modifier.animateItem()) }
                else if (error != null) item { ErrorState(error, onRetry) }
                else if (visible.isEmpty()) item {
                    EmptyState(
                        if (query.isBlank()) "Nothing here right now" else "No invoices match \"$query\"",
                        if (query.isBlank()) "You're all caught up." else "Try a different search."
                    )
                }
                else itemsIndexed(visible, key = { _, it -> it.id }, contentType = { _, _ -> "invoice" }) { index, item ->
                    InvoiceRow(item, Modifier.animateItem(), index < 10, onPreview = { onOpen(item.id) }, onShare = { shareInvoice(context, item) }, onValidate = { confirmTarget = item }, onPost = { confirmTarget = item })
                }
            }
        }
        if (topBarAlpha > 0.01f) {
            Box(Modifier.fillMaxWidth().height(94.dp).background(CardWhite.copy(alpha = topBarAlpha)))
            HorizontalDivider(Modifier.align(Alignment.TopCenter).padding(top = 93.dp), color = Line.copy(alpha = topBarAlpha))
        }
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(68.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            ZeenoMark(size = 44.dp, fontSize = 23.sp)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { SoundFx.click(); onSettings() }, modifier = Modifier.size(42.dp).clip(CircleShape).background(CardWhite).border(1.dp, Line, CircleShape)) {
                Icon(Icons.Rounded.Settings, "Settings", tint = Ink, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { SoundFx.click(); onLock() }, modifier = Modifier.size(42.dp).clip(CircleShape).background(CardWhite).border(1.dp, Line, CircleShape)) {
                Icon(Icons.Rounded.Lock, "Lock app", tint = Ink, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Rounded.VerifiedUser, "Verified account", tint = AccentDeep)
        }
    }
    if (showDatePicker) {
        SimpleDatePicker(
            selectedDate = if (datePickerMode == "start") startDate else endDate,
            onDateSelected = { date ->
                if (datePickerMode == "start") startDate = date else endDate = date
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false }
        )
    }
    confirmTarget?.let { target ->
        val action = if (target.status == PendingStatus.VALIDATE) "validate" else "post"
        ConfirmActionDialog(
            action = action,
            invoiceNumber = target.number,
            amount = money(target.amount),
            onConfirm = {
                if (target.status == PendingStatus.VALIDATE) onValidate(target.id) else onPost(target.id)
                confirmTarget = null
            },
            onDismiss = { SoundFx.click(); confirmTarget = null }
        )
    }
}

@Composable
private fun SimpleDatePicker(selectedDate: String, onDateSelected: (String) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now(ZoneId.systemDefault())
    val initial = runCatching { LocalDate.parse(selectedDate) }.getOrNull() ?: today
    var year by remember { mutableStateOf(initial.year) }
    var month by remember { mutableStateOf(initial.monthValue) }
    var day by remember { mutableStateOf(initial.dayOfMonth) }
    val maxDays = when (month) { 1,3,5,7,8,10,12 -> 31; 4,6,9,11 -> 30; else -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28 }
    val safeDay = day.coerceAtMost(maxDays)
    val first = runCatching { LocalDate.of(year, month, 1) }.getOrNull() ?: today
    val leadingBlanks = first.dayOfWeek.value % 7
    val cells = List(leadingBlanks) { null } + (1..maxDays).map { it }
    val weeks = cells.chunked(7)
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                onDateSelected("$year-${month.toString().padStart(2, '0')}-${safeDay.toString().padStart(2, '0')}")
            }) { Text("OK", color = AccentDeep, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = InkMuted) } },
        title = { Text("Select date", style = MaterialTheme.typography.titleLarge, color = Ink) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = {
                        if (month > 1) month-- else { month = 12; year-- }
                        day = day.coerceAtMost(when (month) { 1,3,5,7,8,10,12 -> 31; 4,6,9,11 -> 30; else -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28 })
                    }) { Text("‹", style = MaterialTheme.typography.headlineSmall, color = Ink) }
                    Text(
                        "${Month.of(month).name.lowercase().replaceFirstChar { it.uppercase() }} $year",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = Ink
                    )
                    TextButton(onClick = {
                        if (month < 12) month++ else { month = 1; year++ }
                        day = day.coerceAtMost(when (month) { 1,3,5,7,8,10,12 -> 31; 4,6,9,11 -> 30; else -> if (year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)) 29 else 28 })
                    }) { Text("›", style = MaterialTheme.typography.headlineSmall, color = Ink) }
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                        Text(
                            it, modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = InkMuted, textAlign = TextAlign.Center
                        )
                    }
                }
                weeks.forEach { week ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        week.forEach { d ->
                            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                                if (d == null) Spacer(Modifier.size(38.dp))
                                else {
                                    val isSelected = d == safeDay
                                    val isToday = year == today.year && month == today.monthValue && d == today.dayOfMonth
                                    Box(
                                        Modifier.size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Accent else Color.Transparent)
                                            .then(if (isToday && !isSelected) Modifier.border(1.5.dp, Accent, CircleShape) else Modifier)
                                            .clickable { SoundFx.click(); day = d },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            d.toString(),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                            ),
                                            color = if (isSelected) Color.White else Ink
                                        )
                                    }
                                }
                            }
                        }
                        repeat(7 - week.size) { Box(Modifier.weight(1f)) { Spacer(Modifier.size(38.dp)) } }
                    }
                }
            }
        }
    )
}

@Composable
private fun ConfirmActionDialog(
    action: String,
    invoiceNumber: String,
    amount: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        Modifier.fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f))
            .clickable { onDismiss() }
    ) {
        Box(
            Modifier.align(Alignment.Center).width(320.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(CardWhite)
                .padding(24.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(56.dp).clip(CircleShape).background(AccentSoft), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.VerifiedUser, null, tint = AccentDeep, modifier = Modifier.size(28.dp))
                }
                Text("Are you sure?", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                Text(
                    "Do you want to $action invoice $invoiceNumber for $amount? This action cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium, color = InkMuted, textAlign = TextAlign.Center
                )
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Line),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = InkMuted),
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 52.dp)
                    ) { Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
                    AccentButton(action.replaceFirstChar { it.uppercase() }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { onConfirm() }
                }
            }
        }
    }
}

@Composable
private fun InvoiceRow(item: PendingItem, modifier: Modifier = Modifier, animate: Boolean = true, onPreview: () -> Unit, onShare: () -> Unit, onValidate: () -> Unit, onPost: () -> Unit) {
    Card(modifier.fillMaxWidth(), PaddingValues(18.dp), animate = animate) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { SoundFx.click(); onPreview() }) {
            Box(Modifier.size(46.dp).clip(RoundedCornerShape(15.dp)).background(Tile), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Description, null, tint = Ink)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                Text("${item.number} • ${item.date}", style = MaterialTheme.typography.labelMedium, color = InkMuted)
            }
            Icon(Icons.Rounded.ChevronRight, "Open invoice", tint = InkMuted)
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(money(item.amount), style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(item.dueLabel, style = MaterialTheme.typography.labelMedium, color = InkMuted)
            }
            StatusPill(item.status)
        }
        Spacer(Modifier.height(14.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            when (item.status) {
                PendingStatus.VALIDATE -> AccentButton("Validate", modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { SoundFx.click(); onValidate() }
                PendingStatus.POSTED -> AccentButton("Post", modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { SoundFx.click(); onPost() }
            }
            OutlinedIconButton(
                onClick = { SoundFx.click(); onShare() },
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Line),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.Share, "Share invoice", tint = Ink, modifier = Modifier.size(22.dp))
            }
            OutlinedIconButton(
                onClick = { SoundFx.click(); onPreview() },
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Line),
                modifier = Modifier.size(48.dp)
            ) {
                Icon(Icons.Rounded.PictureAsPdf, "Preview PDF", tint = Ink, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
fun StatusPill(status: PendingStatus) {
    val (bg, fg) = when (status) {
        PendingStatus.VALIDATE -> ErrorTint to ErrorInk
        PendingStatus.POSTED -> Accent to Color.White
    }
    Surface(color = bg, shape = CircleShape) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)) {
            if (status == PendingStatus.POSTED) {
                Icon(Icons.Rounded.Check, null, tint = fg, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(status.label, color = fg, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@Composable
private fun PendingSkeleton(modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth()) {
        LoadingShimmer(Modifier.fillMaxWidth(.65f).height(18.dp), dark = true)
        Spacer(Modifier.height(12.dp))
        LoadingShimmer(Modifier.fillMaxWidth(.4f).height(12.dp), dark = true)
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(60.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.TaskAlt, null, tint = Color.White, modifier = Modifier.size(30.dp))
            }
            Spacer(Modifier.height(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
            Text(subtitle, color = InkMuted)
        }
    }
}

@Composable
private fun ErrorState(message: String, retry: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Icon(Icons.Rounded.CloudOff, null, tint = ErrorInk, modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(10.dp))
        Text(message, color = Ink)
        Spacer(Modifier.height(16.dp))
        AccentButton("Try again", onClick = retry)
    }
}
