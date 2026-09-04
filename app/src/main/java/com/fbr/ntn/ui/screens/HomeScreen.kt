package com.fbr.ntn.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.fbr.ntn.model.*
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(account: AccountContext?, items: List<PendingItem>, loading: Boolean, refreshing: Boolean, error: String?, onRefresh: () -> Unit, onRetry: () -> Unit) {
    val listState = rememberLazyListState()
    val topGlass by animateFloatAsState(if (listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 40) 1f else 0f, label = "top glass")
    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            indicator = {
                if (refreshing) LoadingShimmer(Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 78.dp).width(90.dp).height(7.dp))
            }
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 116.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Text("Good morning", style = MaterialTheme.typography.bodyLarge, color = InkMuted)
                    Text(account?.displayName ?: "Your account", style = MaterialTheme.typography.headlineMedium, color = Ink)
                    Text(account?.let { "NTN ${it.ntn}" } ?: "Secure FBR session", style = MaterialTheme.typography.labelMedium, color = InkMuted)
                    Spacer(Modifier.height(24.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Pending items", style = MaterialTheme.typography.titleLarge, color = Ink)
                        Surface(shape = CircleShape, color = AccentBlue.copy(.1f)) { Text(items.size.toString(), color = AccentBlue, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium) }
                    }
                }
                if (loading) items(3) { PendingSkeleton() }
                else if (error != null) item { ErrorState(error, onRetry) }
                else if (items.isEmpty()) item { EmptyState() }
                else items(items, key = { it.id }) { PendingRow(it) }
            }
        }
        Box(Modifier.fillMaxWidth().height(94.dp).alpha(topGlass).glass(RoundedCornerShape(0.dp)))
        Row(Modifier.fillMaxWidth().statusBarsPadding().height(68.dp).padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(38.dp).clip(RoundedCornerShape(13.dp)).background(AccentBlue), contentAlignment = Alignment.Center) { Text("F", color = Color.White, style = MaterialTheme.typography.labelLarge) }
            Spacer(Modifier.width(12.dp)); Text("FBR NTN", style = MaterialTheme.typography.titleLarge, color = Ink)
            Spacer(Modifier.weight(1f)); Icon(Icons.Rounded.VerifiedUser, "Verified account", tint = AccentTeal)
        }
    }
}

@Composable
private fun PendingRow(item: PendingItem) {
    GlassCard(Modifier.fillMaxWidth(), PaddingValues(18.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(AccentBlue.copy(.1f)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Description, null, tint = AccentBlue) }
            Spacer(Modifier.width(14.dp)); Column(Modifier.weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.labelLarge, color = Ink)
                Text("${item.period} · ${item.dueLabel}", style = MaterialTheme.typography.labelMedium, color = InkMuted)
            }
            Icon(Icons.Rounded.ChevronRight, "Open item", tint = InkMuted)
        }
        Spacer(Modifier.height(14.dp))
        val color = when (item.status) { PendingStatus.DUE_SOON -> ErrorRed; PendingStatus.REVIEW -> AccentBlue; PendingStatus.PENDING -> Color(0xFFB36B00) }
        Surface(color = color.copy(.1f), shape = CircleShape) { Text(item.status.label, color = color, style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp)) }
    }
}

@Composable private fun PendingSkeleton() { GlassCard(Modifier.fillMaxWidth()) { LoadingShimmer(Modifier.fillMaxWidth(.65f).height(18.dp)); Spacer(Modifier.height(12.dp)); LoadingShimmer(Modifier.fillMaxWidth(.4f).height(12.dp)) } }
@Composable private fun EmptyState() { GlassCard(Modifier.fillMaxWidth()) { Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) { Icon(Icons.Rounded.TaskAlt, null, tint = AccentTeal, modifier = Modifier.size(48.dp)); Spacer(Modifier.height(12.dp)); Text("Nothing pending right now", style = MaterialTheme.typography.titleLarge); Text("You're all caught up.", color = InkMuted) } } }
@Composable private fun ErrorState(message: String, retry: () -> Unit) { GlassCard(Modifier.fillMaxWidth()) { Icon(Icons.Rounded.CloudOff, null, tint = ErrorRed); Spacer(Modifier.height(10.dp)); Text(message, color = Ink); Spacer(Modifier.height(16.dp)); PrimaryButton("Try again", onClick = retry) } }
