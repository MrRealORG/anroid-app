package com.fbr.ntn.ui.screens

import android.content.Context
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fbr.ntn.model.PendingItem
import com.fbr.ntn.model.PendingStatus
import com.fbr.ntn.model.money
import com.fbr.ntn.model.qtyFmt
import com.fbr.ntn.ui.components.*
import com.fbr.ntn.ui.sound.SoundFx
import com.fbr.ntn.ui.theme.*

fun shareInvoice(context: Context, inv: PendingItem) = com.fbr.ntn.data.PdfInvoice.share(context, inv)
fun previewPdf(context: Context, inv: PendingItem) = com.fbr.ntn.data.PdfInvoice.view(context, inv)

@Composable
fun InvoiceDetailScreen(
    invoice: PendingItem,
    detailLoading: Boolean,
    onBack: () -> Unit,
    onValidate: () -> Unit,
    onPost: () -> Unit
) {
    val context = LocalContext.current
    var celebrated by remember { mutableStateOf(false) }
    val posted = invoice.status == PendingStatus.POSTED
    LaunchedEffect(posted) { if (posted && !celebrated) { celebrated = true; SoundFx.success() } }

    var showValidateConfirm by remember { mutableStateOf(false) }
    var showPostConfirm by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Paper)) {
        SoftBackground()
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ScreenHeader("Invoice ${invoice.number}", invoice.title, true, onBack)
            AnimatedContent(posted, label = "posted banner") { isPosted ->
                if (isPosted) AccentPill("Posted — thank you")
                else StatusPill(invoice.status)
            }

            if (detailLoading) {
                Card(enterDelay = 0) {
                    Box(Modifier.fillMaxWidth().padding(vertical = 40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Accent, strokeWidth = 3.dp)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Loading invoice details from FBR...", style = MaterialTheme.typography.bodyMedium, color = InkMuted, modifier = Modifier.align(Alignment.CenterHorizontally))
                }
            } else {
                Card(enterDelay = 100) {
                    Text("BILL TO", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = InkMuted)
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(48.dp).clip(CircleShape).background(Accent), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Person, null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(invoice.client.ifBlank { "Walk-in customer" }, style = MaterialTheme.typography.titleLarge, color = Ink)
                            Text("${invoice.date} • ${invoice.period}", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                        }
                    }
                }

                if (invoice.sellerStrn.isNotBlank() || invoice.buyerNtn.isNotBlank()) {
                    Card(enterDelay = 140) {
                        Text("TAX DETAILS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = InkMuted)
                        Spacer(Modifier.height(10.dp))
                        if (invoice.sellerStrn.isNotBlank()) {
                            Text("Seller STRN: ${invoice.sellerStrn}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                        if (invoice.sellerNtn.isNotBlank()) {
                            Text("Seller NTN: ${invoice.sellerNtn}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                        if (invoice.buyerNtn.isNotBlank()) {
                            Text("Buyer NTN: ${invoice.buyerNtn}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                    }
                }

                if (invoice.fbrInvoiceNo.isNotBlank() || invoice.validationCode.isNotBlank()) {
                    Card(enterDelay = 160) {
                        Text("FBR VERIFICATION", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = InkMuted)
                        Spacer(Modifier.height(10.dp))
                        if (invoice.fbrInvoiceNo.isNotBlank()) {
                            Text("FBR Invoice Token: ${invoice.fbrInvoiceNo}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                        if (invoice.validationCode.isNotBlank()) {
                            Text("Validation Code: ${invoice.validationCode}", style = MaterialTheme.typography.bodyMedium, color = Ink)
                        }
                    }
                }

                Card(enterDelay = 170) {
                    Text("ITEMS", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = InkMuted)
                    Spacer(Modifier.height(6.dp))
                    if (invoice.items.isEmpty()) {
                        Text("No items loaded", style = MaterialTheme.typography.bodyMedium, color = InkMuted)
                    } else {
                        invoice.items.forEachIndexed { index, item ->
                            if (index > 0) HorizontalDivider(color = Line, modifier = Modifier.padding(vertical = 12.dp))
                            else Spacer(Modifier.height(6.dp))
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Column(Modifier.weight(1f)) {
                                    Text(item.description, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                                    Text(
                                        "HS ${item.hsCode} • ${qtyFmt(item.quantity)} ${item.uom} x ${money(item.rate)} • ST ${qtyFmt(item.taxRate)}%",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = InkMuted
                                    )
                                }
                                Text(money(item.total), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = Line)
                    Spacer(Modifier.height(12.dp))
                    TotalRow("Subtotal", money(invoice.subtotal), false)
                    Spacer(Modifier.height(8.dp))
                    TotalRow("Sales Tax", money(invoice.tax), false)
                    Spacer(Modifier.height(8.dp))
                    TotalRow("Further Tax / FED / Discount", money(invoice.furtherTax), false)
                    Spacer(Modifier.height(8.dp))
                    TotalRow("Grand Total", money(invoice.amount), true)
                }

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { SoundFx.click(); shareInvoice(context, invoice) },
                        shape = CircleShape,
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Ink),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink),
                        contentPadding = PaddingValues(horizontal = 22.dp, vertical = 15.dp),
                        modifier = Modifier.heightIn(min = 56.dp)
                    ) {
                        Icon(Icons.Rounded.Share, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold))
                    }
                    if (posted) {
                        AccentButton("Posted", enabled = false, modifier = Modifier.weight(1f)) {}
                    } else {
                        if (invoice.status == PendingStatus.VALIDATE) {
                            AccentButton("Validate", modifier = Modifier.weight(1f)) { showValidateConfirm = true }
                        }
                        if (invoice.status == PendingStatus.POSTED) {
                            AccentButton("Post", modifier = Modifier.weight(1f)) { showPostConfirm = true }
                        }
                    }
                }
                TextButton(onClick = { previewPdf(context, invoice) }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Icon(Icons.Rounded.PictureAsPdf, null, tint = AccentDeep, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Preview PDF", color = AccentDeep, fontWeight = FontWeight.Bold)
                }
            }
        }
        if (showValidateConfirm) {
            ConfirmDialog(
                title = "Validate invoice?",
                message = "This will send the invoice to FBR for validation. Continue?",
                onConfirm = { SoundFx.click(); onValidate(); showValidateConfirm = false },
                onDismiss = { SoundFx.click(); showValidateConfirm = false }
            )
        }
        if (showPostConfirm) {
            ConfirmDialog(
                title = "Post invoice?",
                message = "This will finalize the invoice in your workspace. Continue?",
                onConfirm = { SoundFx.click(); onPost(); showPostConfirm = false },
                onDismiss = { SoundFx.click(); showPostConfirm = false }
            )
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.4f)).clickable { onDismiss() }
    ) {
        Box(
            Modifier.align(Alignment.Center).width(320.dp)
                .clip(RoundedCornerShape(22.dp)).background(CardWhite).padding(24.dp)
        ) {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleLarge, color = Ink)
                Text(message, style = MaterialTheme.typography.bodyLarge, color = InkMuted, textAlign = TextAlign.Center)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    OutlinedButton(onClick = onDismiss, shape = CircleShape, border = androidx.compose.foundation.BorderStroke(1.5.dp, Line), colors = ButtonDefaults.outlinedButtonColors(contentColor = InkMuted), contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp)) { Text("Cancel", style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)) }
                    AccentButton("Confirm", modifier = Modifier.heightIn(min = 56.dp)) { onConfirm() }
                }
            }
        }
    }
}

@Composable
private fun TotalRow(label: String, value: String, bold: Boolean) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge, color = Ink)
        Text(value, style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Ink)
    }
}
