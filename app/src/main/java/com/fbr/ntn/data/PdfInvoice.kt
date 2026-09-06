package com.fbr.ntn.data

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.fbr.ntn.model.PendingItem
import com.fbr.ntn.model.amountInWordsPKR
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File
import java.io.FileOutputStream
import kotlin.math.round

object PdfInvoice {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val M = 40f

    private val navy = Color.rgb(0x11, 0x13, 0x18)
    private val accent = Color.rgb(0x07, 0x00, 0xFF)
    private val muted = Color.rgb(0x6B, 0x72, 0x80)
    private val line = Color.rgb(0xE7, 0xE2, 0xD8)
    private val lightFill = Color.rgb(0xF3, 0xF0, 0xE9)

    private fun paint(color: Int, size: Float, bold: Boolean = false): Paint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color
            textSize = size
            typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            else Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        }

    private fun plain(value: Double): String {
        val rounded = round(value * 100) / 100.0
        val parts = rounded.toString().split(".")
        val intPart = parts[0].reversed().chunked(3).joinToString(",").reversed()
        return "$intPart.${(parts.getOrElse(1) { "0" } + "00").take(2)}"
    }

    private fun right(c: android.graphics.Canvas, text: String, x: Float, y: Float, p: Paint) {
        c.drawText(text, x - p.measureText(text), y, p)
    }

    private fun wrap(text: String, p: Paint, maxW: Float): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var cur = ""
        for (w in words) {
            val t = if (cur.isEmpty()) w else "$cur $w"
            if (p.measureText(t) <= maxW) cur = t
            else {
                if (cur.isNotEmpty()) lines += cur
                cur = w
            }
        }
        if (cur.isNotEmpty()) lines += cur
        return lines
    }

    private fun generateQrBitmap(content: String, size: Int = 120): Bitmap {
        val hints = mapOf(EncodeHintType.MARGIN to 1, EncodeHintType.ERROR_CORRECTION to com.google.zxing.qrcode.decoder.ErrorCorrectionLevel.L)
        val writer = QRCodeWriter()
        val bitMatrix: BitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) {
            for (y in 0 until size) {
                bmp.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
            }
        }
        return bmp
    }

    fun generate(context: Context, inv: PendingItem): File {
        val doc = PdfDocument()
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        var c = page.canvas

        val title = paint(navy, 20f, true)
        val h = paint(navy, 11f, true)
        val body = paint(navy, 9.5f)
        val bodyBold = paint(navy, 9.5f, true)
        val small = paint(muted, 8.5f)
        val smallBold = paint(muted, 8.5f, true)
        val tiny = paint(muted, 7.5f)
        val thin: (Int) -> Paint = { col -> Paint().apply { color = col; strokeWidth = 1f } }

        var y = 40f

        c.drawText("SALES TAX INVOICE", M, y + 18f, title)
        right(c, "FBR DIGITAL INVOICE", PAGE_W - M, y + 18f, paint(accent, 10f, true))
        y += 36f
        c.drawLine(M, y, PAGE_W - M, y, thin(line)); y += 16f

        fun meta(label: String, value: String, x: Float) {
            c.drawText(label, x, y, tiny)
            c.drawText(value, x, y + 13f, bodyBold)
        }
        meta("INVOICE NUMBER", inv.number, M)
        meta("INVOICE DATE", inv.date.ifBlank { "-" }, 220f)
        y += 28f
        c.drawLine(M, y, PAGE_W - M, y, thin(line)); y += 16f

        val midX = PAGE_W / 2f + 10f
        c.drawText("FROM / SELLER", M, y, smallBold)
        c.drawText("BILL TO / BUYER", midX, y, smallBold); y += 14f
        c.drawText(inv.sellerName, M, y, bodyBold)
        c.drawText(inv.client.ifBlank { "Walk-in customer" }, midX, y, bodyBold); y += 13f
        c.drawText("NTN: ${inv.sellerNtn}", M, y, small)
        if (inv.sellerStrn.isNotBlank()) c.drawText("STRN: ${inv.sellerStrn}", M, y + 12f, small)
        if (inv.buyerNtn.isNotBlank()) c.drawText("NTN: ${inv.buyerNtn}", midX, y, small)
        if (inv.buyerStrn.isNotBlank()) c.drawText("STRN: ${inv.buyerStrn}", midX, y + 12f, small)
        y += if (inv.sellerStrn.isNotBlank() || inv.buyerStrn.isNotBlank()) 24f else 12f
        wrap(inv.sellerAddr, small, midX - M - 16f).take(2).forEach { c.drawText(it, M, y, small); y += 12f }
        if (inv.buyerAddr.isNotBlank()) wrap(inv.buyerAddr, small, PAGE_W - midX - M).take(2).forEach { c.drawText(it, midX, y, small); y += 12f }
        c.drawText(inv.sellerContact, M, y, small)
        y += 24f
        c.drawLine(M, y, PAGE_W - M, y, thin(line)); y += 16f

        val defaultCols = listOf("#", "HS CODE", "DESCRIPTION", "UOM", "QTY", "RATE", "AMOUNT", "TAX", "TOTAL")
        val cols = if (inv.columns.isNotEmpty()) inv.columns.map { it.uppercase() } else defaultCols
        val totalTableW = PAGE_W - 2 * M
        val colCount = cols.size
        val colWidths = cols.mapIndexed { i, col ->
            when {
                col in listOf("#") -> 24f
                col in listOf("HS CODE", "HS", "HS-CODE") -> 56f
                col in listOf("UOM") -> 36f
                col in listOf("QTY", "QTY.", "QUANTITY") -> 40f
                col in listOf("RATE", "RETAIL PRICE", "RETAIL PRICE (RS)") -> 52f
                col in listOf("AMOUNT", "AMOUNT (RS)") -> 56f
                col in listOf("GST RATE", "GST RATE (%)", "TAX RATE") -> 44f
                col in listOf("GST AMOUNT", "GST AMOUNT (RS)", "TAX") -> 52f
                col in listOf("TOTAL INCL. GST", "TOTAL INCL. GST (RS)", "TOTAL") -> 56f
                else -> totalTableW / colCount
            }
        }
        val colX = mutableListOf<Float>()
        var cx = M
        colWidths.forEach { w -> colX.add(cx); cx += w }
        val rightAlignedCols = setOf("#", "QTY", "QTY.", "QUANTITY", "RATE", "RETAIL PRICE", "RETAIL PRICE (RS)",
            "AMOUNT", "AMOUNT (RS)", "GST RATE", "GST RATE (%)", "TAX RATE",
            "GST AMOUNT", "GST AMOUNT (RS)", "TAX", "TOTAL INCL. GST", "TOTAL INCL. GST (RS)", "TOTAL")

        fun colValue(item: com.fbr.ntn.model.LineItem, colName: String): String = when (colName) {
            "#", "SR", "SR." -> ""
            "HS CODE", "HS", "HS-CODE" -> item.hsCode
            "DESCRIPTION", "ITEM NAME", "ITEM", "DESC" -> item.description
            "UOM" -> item.uom
            "QTY", "QTY.", "QUANTITY" -> com.fbr.ntn.model.qtyFmt(item.quantity)
            "RATE", "RETAIL PRICE", "RETAIL PRICE (RS)" -> plain(item.rate)
            "AMOUNT", "AMOUNT (RS)" -> plain(item.valueExcl)
            "GST RATE", "GST RATE (%)", "TAX RATE" -> "${com.fbr.ntn.model.qtyFmt(item.taxRate)}%"
            "GST AMOUNT", "GST AMOUNT (RS)", "TAX" -> plain(item.tax)
            "TOTAL INCL. GST", "TOTAL INCL. GST (RS)", "TOTAL" -> plain(item.total)
            else -> ""
        }

        fun tableHeader(yy: Float) {
            c.drawRect(M, yy, PAGE_W - M, yy + 20f, Paint().apply { color = lightFill })
            val hp = paint(navy, 8f, true)
            cols.forEachIndexed { i, col ->
                val x = colX[i]
                if (col in rightAlignedCols) right(c, col, x + colWidths[i] - 4f, yy + 14f, hp)
                else c.drawText(col, x + 4f, yy + 14f, hp)
            }
        }
        tableHeader(y); y += 20f
        val rowP = paint(navy, 8.5f)
        inv.items.forEachIndexed { index, item ->
            if (y > 660f) {
                doc.finishPage(page)
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create())
                c = page.canvas; y = 50f
                tableHeader(y); y += 20f
            }
            y += 16f
            cols.forEachIndexed { i, col ->
                val x = colX[i]
                val value = if (col == "#") "%02d".format(index + 1) else colValue(item, col)
                var display = value
                if (col == "DESCRIPTION" || col == "ITEM NAME" || col == "ITEM" || col == "DESC") {
                    val maxW = colWidths[i] - 8f
                    while (rowP.measureText(display) > maxW && display.length > 4) display = display.dropLast(1)
                    if (display != value) display = display.dropLast(1) + "…"
                }
                val p = if (col == "TOTAL INCL. GST" || col == "TOTAL INCL. GST (RS)" || col == "TOTAL") paint(navy, 8.5f, true) else rowP
                if (col in rightAlignedCols) right(c, display, x + colWidths[i] - 4f, y, p)
                else c.drawText(display, x + 4f, y, p)
            }
            y += 6f
            c.drawLine(M, y, PAGE_W - M, y, thin(line))
        }
        y += 20f
        if (y > 620f) {
            doc.finishPage(page)
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create())
            c = page.canvas; y = 50f
        }

        val notesTop = y
        c.drawText("AMOUNT IN WORDS", M, y, smallBold); y += 14f
        wrap(amountInWordsPKR(inv.amount), body, PAGE_W - 2 * M).take(3).forEach { c.drawText(it, M, y, body); y += 11f }

        var ty = notesTop + 14f
        fun totalRow(label: String, value: String, big: Boolean) {
            c.drawText(label, 330f, ty, if (big) paint(navy, 12f, true) else body)
            right(c, value, PAGE_W - M, ty, if (big) paint(navy, 12f, true) else bodyBold)
            ty += 17f
        }
        totalRow("Subtotal", "PKR ${plain(inv.subtotal)}", false)
        totalRow("Sales Tax", "PKR ${plain(inv.tax)}", false)
        totalRow("Further Tax / FED", "PKR ${plain(inv.furtherTax)}", false)
        c.drawLine(330f, ty - 11f, PAGE_W - M, ty - 11f, Paint().apply { color = navy; strokeWidth = 1.5f })
        totalRow("GRAND TOTAL", "PKR ${plain(inv.amount)}", true)
        y = maxOf(y + 30f, ty + 10f)

        if (y > 680f) {
            doc.finishPage(page)
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, doc.pages.size + 1).create())
            c = page.canvas; y = 50f
        }

        val fbrBoxY = y
        c.drawRoundRect(RectF(M, y, PAGE_W - M, y + 80f), 8f, 8f, Paint().apply {
            color = lightFill; style = Paint.Style.STROKE; strokeWidth = 1f
        })
        c.drawText("FBR DIGITAL VERIFICATION", M + 12f, y + 20f, paint(navy, 10f, true))
        c.drawText("FBR Invoice No. ${inv.fbrInvoiceNo.ifBlank { "-" }}", M + 12f, y + 38f, body)
        c.drawText("Validation code: ${inv.validationCode.ifBlank { "-" }}", M + 12f, y + 54f, body)

        val qrContent = buildString {
            append("Invoice: ${inv.number}")
            if (inv.fbrInvoiceNo.isNotBlank()) append("\nToken: ${inv.fbrInvoiceNo}")
            if (inv.validationCode.isNotBlank()) append("\nValidation: ${inv.validationCode}")
            append("\nAmount: PKR ${plain(inv.amount)}")
            append("\nDate: ${inv.date}")
        }
        try {
            val qrBitmap = generateQrBitmap(qrContent, 120)
            val qrX = PAGE_W - M - 100f
            val qrY = fbrBoxY + 10f
            c.drawBitmap(qrBitmap, null, RectF(qrX, qrY, qrX + 60f, qrY + 60f), null)
        } catch (_: Exception) { }

        y += 96f

        c.drawLine(M, PAGE_H - 40f, PAGE_W - M, PAGE_H - 40f, thin(line))
        val foot = "System-generated invoice — no signature or stamp required"
        c.drawText(foot, (PAGE_W - tiny.measureText(foot)) / 2f, PAGE_H - 24f, tiny)

        doc.finishPage(page)
        val file = File(context.cacheDir, "Invoice-${inv.number.replace("#", "")}.pdf")
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun uri(context: Context, file: File) =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    fun share(context: Context, inv: PendingItem) {
        val file = runCatching { generate(context, inv) }.getOrNull() ?: return
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_SUBJECT, "Invoice ${inv.number}")
            putExtra(Intent.EXTRA_STREAM, uri(context, file))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(send, "Share invoice ${inv.number}")) }
    }

    fun view(context: Context, inv: PendingItem) {
        val file = runCatching { generate(context, inv) }.getOrNull() ?: return
        val open = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri(context, file), "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(Intent.createChooser(open, "Open invoice ${inv.number}")) }
    }
}
