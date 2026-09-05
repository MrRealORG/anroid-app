package com.fbr.ntn.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.fbr.ntn.R
import com.fbr.ntn.model.PendingItem
import com.fbr.ntn.model.amountInWordsPKR
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
        if (inv.buyerNtn.isNotBlank()) c.drawText("NTN: ${inv.buyerNtn}", midX, y, small)
        y += 12f
        wrap(inv.sellerAddr, small, midX - M - 16f).take(2).forEach { c.drawText(it, M, y, small); y += 12f }
        c.drawText(inv.sellerContact, M, y, small)
        y += 24f
        c.drawLine(M, y, PAGE_W - M, y, thin(line)); y += 16f

        fun tableHeader(yy: Float) {
            c.drawRect(M, yy, PAGE_W - M, yy + 20f, Paint().apply { color = lightFill })
            val hp = paint(navy, 8f, true)
            c.drawText("#", M + 4f, yy + 14f, hp)
            c.drawText("HS CODE", 56f, yy + 14f, hp)
            c.drawText("DESCRIPTION", 120f, yy + 14f, hp)
            c.drawText("UOM", 290f, yy + 14f, hp)
            c.drawText("QTY", 320f, yy + 14f, hp)
            c.drawText("RATE", 360f, yy + 14f, hp)
            right(c, "AMOUNT", 470f, yy + 14f, hp)
            right(c, "TAX", 520f, yy + 14f, hp)
            right(c, "TOTAL", PAGE_W - M - 4f, yy + 14f, hp)
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
            c.drawText("%02d".format(index + 1), M + 4f, y, rowP)
            c.drawText(item.hsCode, 56f, y, rowP)
            var desc = item.description
            while (rowP.measureText(desc) > 164f && desc.length > 4) desc = desc.dropLast(1)
            if (desc != item.description) desc = desc.dropLast(1) + "…"
            c.drawText(desc, 120f, y, rowP)
            c.drawText(item.uom, 290f, y, rowP)
            c.drawText(com.fbr.ntn.model.qtyFmt(item.quantity), 320f, y, rowP)
            c.drawText(plain(item.rate), 360f, y, rowP)
            right(c, plain(item.valueExcl), 470f, y, rowP)
            right(c, "${plain(item.tax)} (${com.fbr.ntn.model.qtyFmt(item.taxRate)}%)", 520f, y, rowP)
            right(c, plain(item.total), PAGE_W - M - 4f, y, paint(navy, 8.5f, true))
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
        c.drawText("PAYMENT & NOTES", M, y, smallBold); y += 14f
        c.drawText("Amount in words", M, y, tiny); y += 11f
        wrap(amountInWordsPKR(inv.amount), body, 280f).take(3).forEach { c.drawText(it, M, y, body); y += 11f }
        y += 5f
        c.drawText("Payment terms", M, y, tiny); y += 11f
        wrap(inv.paymentTerms, body, 280f).take(2).forEach { c.drawText(it, M, y, body); y += 11f }
        c.drawText("Sale type: ${inv.saleType}", M, y + 10f, small)

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
        c.drawRoundRect(RectF(M, y, PAGE_W - M, y + 80f), 8f, 8f, Paint().apply {
            color = lightFill; style = Paint.Style.STROKE; strokeWidth = 1f
        })
        c.drawText("FBR DIGITAL VERIFICATION", M + 12f, y + 20f, paint(navy, 10f, true))
        c.drawText("FBR Invoice No. ${inv.fbrInvoiceNo.ifBlank { "-" }}", M + 12f, y + 38f, body)
        c.drawText("Validation code: ${inv.validationCode}", M + 12f, y + 54f, body)
        c.drawText("Scenario: ${inv.scenario}", M + 12f, y + 70f, body)
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
