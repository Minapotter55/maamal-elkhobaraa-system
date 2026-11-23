package com.elkhobaraa.lab.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.elkhobaraa.lab.data.OrderData
import java.io.File

/** Builds the A4 results report: header with logo, patient box, results table, footer with address/phones. */
object ReportPdf {
    private const val W = 595
    private const val H = 842
    private const val M = 30f
    private const val BLUE = 0xFF991717.toInt() // brand maroon
    private const val LIGHT = 0xFFFBF1F1.toInt()
    private const val RED = 0xFFE53935.toInt()
    private const val DARK = 0xFF2B2B2B.toInt()
    private const val GREY = 0xFF6B7280.toInt()

    fun create(ctx: Context, data: OrderData): File {
        val doc = PdfDocument()
        val logo = LogoStore.bitmap(ctx, 320)
        var pageNo = 0
        var page: PdfDocument.Page? = null
        var c = Canvas()
        var y = 0f

        val cols = floatArrayOf(M, M + 200f, M + 300f, M + 380f, W - M)

        fun tableHeader() {
            val p = Paint().apply { color = BLUE }
            c.drawRect(M, y, W - M, y + 24f, p)
            val tp = paint(10.5f, 0xFFFFFFFF.toInt(), bold = true)
            val heads = listOf("Test", "Result", "Unit", "Reference Range")
            for (i in heads.indices) text(c, heads[i], cols[i] + 6f, y + 5f, cols[i + 1] - cols[i] - 12f, tp)
            y += 24f
        }

        fun footer() {
            c.drawRect(0f, H - 50f, W.toFloat(), H.toFloat(), Paint().apply { color = BLUE })
            val wp = paint(10f, 0xFFFFFFFF.toInt(), bold = true)
            text(c, LabInfo.ADDRESS, M, H - 45f, W - 2 * M, wp, Layout.Alignment.ALIGN_CENTER)
            text(
                c, "واتساب: ${LabInfo.WHATSAPP}   -   اتصال: ${LabInfo.PHONE}", M, H - 29f, W - 2 * M,
                wp, Layout.Alignment.ALIGN_CENTER,
            )
            text(c, "$pageNo", W - M - 20f, H - 16f, 20f, paint(7f, 0xFFF6DADA.toInt()))
        }

        fun newPage() {
            page?.let { footer(); doc.finishPage(it) }
            pageNo++
            val pg = doc.startPage(PdfDocument.PageInfo.Builder(W, H, pageNo).create())
            page = pg
            c = pg.canvas
            // Header: logo + name on white, maroon rule underneath
            c.drawBitmap(logo, null, RectF(W - M - 92f, 10f, W - M, 102f), Paint(Paint.FILTER_BITMAP_FLAG))
            val rightW = 260f
            text(c, LabInfo.NAME, W - M - 102f - rightW, 22f, rightW, paint(26f, BLUE, bold = true))
            text(c, LabInfo.TAGLINE, W - M - 102f - rightW, 60f, rightW, paint(12f, 0xFF3A3A3A.toInt(), bold = true))
            text(c, LabInfo.NAME_EN, M, 22f, 180f, paint(11f, BLUE, bold = true))
            text(c, "WhatsApp: ${LabInfo.WHATSAPP}", M, 42f, 180f, paint(10f, DARK))
            text(c, "Tel: ${LabInfo.PHONE}", M, 58f, 180f, paint(10f, DARK))
            c.drawRect(0f, 108f, W.toFloat(), 112f, Paint().apply { color = BLUE })
            c.drawRect(0f, 114f, W.toFloat(), 115f, Paint().apply { color = 0xFF3A3A3A.toInt() })
            y = 126f
        }

        newPage()

        // Patient box
        val pt = data.patient
        val o = data.order
        val boxH = 78f
        c.drawRoundRect(RectF(M, y, W - M, y + boxH), 8f, 8f, Paint().apply { color = LIGHT })
        c.drawRoundRect(
            RectF(M, y, W - M, y + boxH), 8f, 8f,
            Paint(Paint.ANTI_ALIAS_FLAG).apply { color = BLUE; style = Paint.Style.STROKE; strokeWidth = 1f },
        )
        val half = (W - 2 * M) / 2f
        val lp = paint(11f, DARK)
        val right = listOf(
            "اسم المريض: ${pt.name}",
            "السن: ${pt.age.ifBlank { "-" }}     النوع: ${pt.gender.ifBlank { "-" }}",
            "الطبيب المعالج: ${o.doctor.ifBlank { pt.doctor }.ifBlank { "-" }}",
        )
        val left = listOf(
            "رقم الطلب: ${o.id}",
            "التاريخ: ${Dates.format(o.createdAt)}",
            "التليفون: ${pt.phone.ifBlank { "-" }}",
        )
        for (i in right.indices) {
            text(c, right[i], M + half, y + 8f + i * 22f, half - 12f, lp)
            text(c, left[i], M + 12f, y + 8f + i * 22f, half - 12f, lp)
        }
        y += boxH + 14f

        text(c, "نتائج التحاليل - Laboratory Results", M, y, W - 2 * M, paint(14f, BLUE, bold = true), Layout.Alignment.ALIGN_CENTER)
        y += 26f
        tableHeader()

        val bottomLimit = H - 80f
        data.items.forEachIndexed { idx, item ->
            val flag = item.flag()
            val nameP = paint(10.5f, DARK, bold = true)
            val rowH = maxOf(
                22f,
                measure(item.testName, cols[1] - cols[0] - 12f, nameP) + 10f,
                measure(item.range(), cols[4] - cols[3] - 12f, paint(10f, DARK)) + 10f,
            )
            if (y + rowH > bottomLimit) {
                newPage(); tableHeader()
            }
            if (flag != 0) c.drawRect(M, y, W - M, y + rowH, Paint().apply { color = 0xFFFFEBEE.toInt() })
            else if (idx % 2 == 1) c.drawRect(M, y, W - M, y + rowH, Paint().apply { color = 0xFFF7F7F7.toInt() })
            text(c, item.testName, cols[0] + 6f, y + 5f, cols[1] - cols[0] - 12f, nameP)
            val res = item.result.ifBlank { "-" } + when (flag) { 1 -> "  H"; -1 -> "  L"; else -> "" }
            text(c, res, cols[1] + 6f, y + 5f, cols[2] - cols[1] - 12f, paint(10.5f, if (flag != 0) RED else DARK, bold = flag != 0))
            text(c, item.unit, cols[2] + 6f, y + 5f, cols[3] - cols[2] - 12f, paint(10f, GREY))
            text(c, item.range(), cols[3] + 6f, y + 5f, cols[4] - cols[3] - 12f, paint(10f, DARK))
            c.drawLine(M, y + rowH, W - M, y + rowH, Paint().apply { color = 0xFFD1D5DB.toInt(); strokeWidth = 0.6f })
            y += rowH
        }

        if (o.notes.isNotBlank()) {
            val np = paint(10.5f, DARK)
            val nh = measure("ملاحظات: ${o.notes}", W - 2 * M, np)
            if (y + nh + 20f > bottomLimit) newPage()
            y += 12f
            text(c, "ملاحظات: ${o.notes}", M, y, W - 2 * M, np)
            y += nh
        }
        if (y + 60f > bottomLimit) newPage()
        y += 14f
        text(c, "* النتائج باللون الأحمر خارج المعدل الطبيعي (H مرتفع - L منخفض)", M, y, W - 2 * M, paint(9.5f, RED))
        y += 30f
        text(c, "توقيع المسؤول: ....................", M, y, W - 2 * M, paint(11f, DARK, bold = true))

        footer()
        doc.finishPage(page!!)

        val dir = File(ctx.cacheDir, "reports").apply { mkdirs() }
        val safe = pt.name.replace(Regex("[\\\\/:*?\"<>|]"), "").trim().ifBlank { "patient" }
        val file = File(dir, "تقرير_${safe}_${o.id}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun paint(size: Float, color: Int, bold: Boolean = false) = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun layout(s: String, w: Float, p: TextPaint, align: Layout.Alignment) =
        StaticLayout.Builder.obtain(s, 0, s.length, p, w.toInt().coerceAtLeast(10)).setAlignment(align).build()

    private fun measure(s: String, w: Float, p: TextPaint): Float = layout(s, w, p, Layout.Alignment.ALIGN_NORMAL).height.toFloat()

    private fun text(
        c: Canvas, s: String, x: Float, y: Float, w: Float, p: TextPaint,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL,
    ) {
        val l = layout(s, w, p, align)
        c.save(); c.translate(x, y); l.draw(c); c.restore()
    }
}
