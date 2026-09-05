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
import com.fbr.ntn.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.fbr.ntn.model.PendingItem
import com.fbr.ntn.model.amountInWordsPKR
import java.io.File
import java.io.FileOutputStream
import kotlin.math.round

/**
 * FBR digital-invoice PDF, matching the official template layout:
 * Zeeno header, TAX INVOICE title, number/date/due meta, seller/buyer blocks,
 * HS-code item table, payment notes + totals, FBR verification block with QR.
 */
object PdfInvoice {
    private const val PAGE_W = 595
    private const val PAGE_H = 842
    private const val M = 40f

    private val navy = Color.rgb(0x11, 0x13, 0x18)
    private val accent = Color.rgb(0x07, 0x00, 0xFF)
    private val muted = Color.rgb(0x6B, 0x72, 0x80)
    private val line = Color.rgb(0xE7, 0xE2, 0xD8)
    private val lightFill = Color.rgb(0xF3, 0xF0, 0xE9)
    private val error = Color.rgb(0xE4, 0x45, 0x3A)

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

    private fun qrBitmap(data: String, size: Int = 180): Bitmap? = runCatching {
        val m = QRCodeWriter().encode(data, BarcodeFormat.QR_CODE, size, size)
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        for (x in 0 until size) for (y in 0 until size) {
            bmp.setPixel(x, y, if (m.get(x, y)) Color.BLACK else Color.WHITE)
        }
        bmp
    }.getOrNull()

    fun generate(context: Context, inv: PendingItem): File {
        val doc = PdfDocument()
        var page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        var c = page.canvas
        var pageNo = 1

        val title = paint(navy, 22f, true)
        val brand = paint(navy, 15f, true)
        val h = paint(navy, 12f, true)
        val body = paint(navy, 9.5f)
        val bodyBold = paint(navy, 9.5f, true)
        val small = paint(muted, 8.5f)
        val smallBold = paint(muted, 8.5f, true)
        val tiny = paint(muted, 7.5f)
        val whiteBold = paint(Color.WHITE, 10f, true)
        val totalPaint = paint(navy, 12f, true)
        val thin: (Int) -> Paint = { col -> Paint().apply { color = col; strokeWidth = 1f } }

        val apiLogo = if (inv.sellerLogoUrl.isNotBlank()) {
            runCatching {
                android.graphics.BitmapFactory.decodeFile(
                    java.io.File(context.cacheDir, "api_seller_logo.png").absolutePath
                )
            }.getOrNull()
        } else null
        val logo = apiLogo ?: android.graphics.BitmapFactory.decodeResource(context.resources, R.drawable.fbr_logo)
        if (logo != null) {
            val boxW = 96f; val boxH = 48f
            val scale = minOf(boxW / logo.width, boxH / logo.height)
            val dw = logo.width * scale
            val dh = logo.height * scale
            val dx = M + (boxW - dw) / 2f
            val dy = 30f + (boxH - dh) / 2f
            c.drawBitmap(logo, null, RectF(dx, dy, dx + dw, dy + dh), null)
        } else {
            c.drawRoundRect(RectF(M, 30f, M + 40f, 78f), 8f, 8f, Paint().apply { color = accent })
            val zp = paint(Color.WHITE, 24f, true)
            c.drawText("Z", M + 20f - zp.measureText("Z") / 2f, 60f, zp)
        }
        right(c, "SALES TAX INVOICE", PAGE_W - M, 52f, title)
        right(c, "FBR DIGITAL INVOICE", PAGE_W - M, 68f, paint(accent, 10f, true))

        var y = 100f
        fun meta(label: String, value: String, x: Float) {
            c.drawText(label, x, y, tiny)
            c.drawText(value, x, y + 14f, bodyBold)
        }
        meta("INVOICE NUMBER", inv.number, M)
        meta("INVOICE DATE", inv.date.ifBlank { "-" }, 220f)
        meta("DUE DATE", inv.dueDate.ifBlank { "-" }, 360f)
        val badge = if (inv.status == com.fbr.ntn.model.PendingStatus.POSTED) accent else error
        val badgeText = if (inv.status == com.fbr.ntn.model.PendingStatus.POSTED) "VALIDATED BY FBR" else "AWAITING VALIDATION"
        val bw = paint(Color.WHITE, 8.5f, true).measureText(badgeText) + 20f
        c.drawRoundRect(RectF(PAGE_W - M - bw, y - 4f, PAGE_W - M, y + 16f), 10f, 10f, Paint().apply { color = badge })
        c.drawText(badgeText, PAGE_W - M - bw + 10f, y + 9f, paint(Color.WHITE, 8.5f, true))
        y += 34f
        c.drawLine(M, y, PAGE_W - M, y, thin(line)); y += 20f

        val midX = PAGE_W / 2f + 10f
        c.drawText("FROM / SELLER", M, y, smallBold)
        c.drawText("BILL TO / BUYER", midX, y, smallBold); y += 16f
        c.drawText(inv.sellerName, M, y, bodyBold)
        c.drawText(inv.client.ifBlank { "Walk-in customer" }, midX, y, bodyBold); y += 14f
        c.drawText("NTN: ${inv.sellerNtn}  STRN: ${inv.sellerStrn}", M, y, small)
        if (inv.buyerNtn.isNotBlank()) c.drawText("NTN: ${inv.buyerNtn}  STRN: ${inv.buyerStrn}", midX, y, small)
        y += 13f
        wrap(inv.sellerAddr, small, midX - M - 16f).take(2).forEach { c.drawText(it, M, y, small); y += 13f }
        val buyerLines = mutableListOf<String>()
        if (inv.buyerAddr.isNotBlank()) buyerLines += inv.buyerAddr
        buyerLines += "Registration Type: ${inv.buyerRegType}"
        c.drawText(inv.sellerContact, M, y, small)
        buyerLines.take(2).forEachIndexed { i, line -> c.drawText(line, midX, y + i * 13f, small) }
        y += 32f
        c.drawLine(M, y, PAGE_W - M, y, thin(line)); y += 20f

        fun tableHeader(yy: Float) {
            c.drawRect(M, yy, PAGE_W - M, yy + 22f, Paint().apply { color = lightFill })
            val hp = paint(navy, 8f, true)
            c.drawText("#", M + 4f, yy + 15f, hp)
            c.drawText("HS CODE", 62f, yy + 15f, hp)
            c.drawText("DESCRIPTION", 128f, yy + 15f, hp)
            c.drawText("UOM", 300f, yy + 15f, hp)
            c.drawText("QTY", 336f, yy + 15f, hp)
            c.drawText("RATE", 372f, yy + 15f, hp)
            right(c, "VALUE EXCL. ST", 472f, yy + 15f, hp)
            right(c, "SALES TAX", 522f, yy + 15f, hp)
            right(c, "TOTAL", PAGE_W - M - 4f, yy + 15f, hp)
        }
        tableHeader(y); y += 22f
        val rowP = paint(navy, 8.5f)
        inv.items.forEachIndexed { index, item ->
            if (y > 640f) {
                doc.finishPage(page); pageNo++
                page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
                c = page.canvas; y = 50f
                tableHeader(y); y += 22f
            }
            y += 18f
            c.drawText("%02d".format(index + 1), M + 4f, y, rowP)
            c.drawText(item.hsCode, 62f, y, rowP)
            var desc = item.description
            while (rowP.measureText(desc) > 166f && desc.length > 4) desc = desc.dropLast(1)
            if (desc != item.description) desc = desc.dropLast(1) + "…"
            c.drawText(desc, 128f, y, rowP)
            c.drawText(item.uom, 300f, y, rowP)
            c.drawText(com.fbr.ntn.model.qtyFmt(item.quantity), 336f, y, rowP)
            c.drawText(plain(item.rate), 372f, y, rowP)
            right(c, plain(item.valueExcl), 472f, y, rowP)
            right(c, "${plain(item.tax)} (${com.fbr.ntn.model.qtyFmt(item.taxRate)}%)", 522f, y, rowP)
            right(c, plain(item.total), PAGE_W - M - 4f, y, paint(navy, 8.5f, true))
            y += 7f
            c.drawLine(M, y, PAGE_W - M, y, thin(line))
        }
        y += 24f
        if (y > 620f) {
            doc.finishPage(page); pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            c = page.canvas; y = 50f
        }

        val notesTop = y
        c.drawText("PAYMENT & NOTES", M, y, smallBold); y += 15f
        c.drawText("Amount in words", M, y, tiny); y += 12f
        wrap(amountInWordsPKR(inv.amount), body, 280f).take(3).forEach { c.drawText(it, M, y, body); y += 12f }
        y += 6f
        c.drawText("Payment terms", M, y, tiny); y += 12f
        wrap(inv.paymentTerms, body, 280f).take(2).forEach { c.drawText(it, M, y, body); y += 12f }
        c.drawText("Sale type: ${inv.saleType}", M, y + 12f, small)

        var ty = notesTop + 15f
        fun totalRow(label: String, value: String, big: Boolean) {
            c.drawText(label, 330f, ty, if (big) totalPaint else body)
            right(c, value, PAGE_W - M, ty, if (big) totalPaint else bodyBold)
            ty += 18f
        }
        totalRow("Subtotal", "PKR ${plain(inv.subtotal)}", false)
        totalRow("Sales Tax", "PKR ${plain(inv.tax)}", false)
        totalRow("Further Tax / FED / Discount", "PKR ${plain(inv.furtherTax)}", false)
        c.drawLine(330f, ty - 12f, PAGE_W - M, ty - 12f, Paint().apply { color = navy; strokeWidth = 1.5f })
        totalRow("GRAND TOTAL", "PKR ${plain(inv.amount)}", true)
        y = maxOf(y + 32f, ty + 12f)

        if (y > 660f) {
            doc.finishPage(page); pageNo++
            page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNo).create())
            c = page.canvas; y = 50f
        }
        c.drawRoundRect(RectF(M, y, PAGE_W - M, y + 108f), 10f, 10f, Paint().apply {
            color = lightFill; style = Paint.Style.STROKE; strokeWidth = 1.5f
        })
        c.drawText("FBR DIGITAL VERIFICATION", M + 14f, y + 22f, paint(navy, 10f, true))
        c.drawText("FBR Invoice No. ${inv.fbrInvoiceNo.ifBlank { "-" }}", M + 14f, y + 42f, body)
        c.drawText("Validation code: ${inv.validationCode}", M + 14f, y + 58f, body)
        c.drawText("Scenario: ${inv.scenario}", M + 14f, y + 74f, body)
        c.drawText("Scan to verify the invoice record. Demo QR data only.", M + 14f, y + 90f, tiny)
        val qr = qrBitmap("https://verify.fbr.gov.pk/invoice/${inv.fbrInvoiceNo.ifBlank { inv.number }}")
        if (qr != null) c.drawBitmap(qr, null, RectF(PAGE_W - M - 96f, y + 8f, PAGE_W - M - 16f, y + 88f), null)
        else c.drawRect(PAGE_W - M - 96f, y + 8f, PAGE_W - M - 16f, y + 88f, Paint().apply {
            color = line; style = Paint.Style.STROKE; strokeWidth = 1.5f
        })
        y += 124f

        c.drawLine(M, PAGE_H - 56f, PAGE_W - M, PAGE_H - 56f, thin(line))
        val foot = "System-generated invoice - no signature or stamp required"
        c.drawText(foot, (PAGE_W - tiny.measureText(foot)) / 2f, PAGE_H - 40f, tiny)
        val demo = "DEMO DOCUMENT - Replace all sample NTN, STRN, invoice, buyer, amounts, and verification data before production use."
        c.drawText(demo, (PAGE_W - paint(error, 6.5f).measureText(demo)) / 2f, PAGE_H - 26f, paint(error, 6.5f, true))
        c.drawText("Page $pageNo", PAGE_W - M - 40f, PAGE_H - 26f, tiny)

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
