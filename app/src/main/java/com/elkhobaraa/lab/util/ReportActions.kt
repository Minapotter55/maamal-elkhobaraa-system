package com.elkhobaraa.lab.util

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object ReportActions {
    private fun uri(ctx: Context, f: File): Uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".fileprovider", f)

    fun view(ctx: Context, f: File) {
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri(ctx, f), "application/pdf")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            ctx.startActivity(i)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "لا يوجد تطبيق لعرض ملفات PDF على الجهاز", Toast.LENGTH_LONG).show()
        }
    }

    fun whatsapp(ctx: Context, f: File, phone: String) {
        val u = uri(ctx, f)
        val i = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, u)
            .putExtra(Intent.EXTRA_TEXT, "نتيجة التحاليل - ${LabInfo.NAME}\n${LabInfo.TAGLINE}")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        i.clipData = ClipData.newRawUri("report", u)
        val digits = phone.filter { it.isDigit() }
        if (digits.length >= 10) {
            val intl = if (digits.startsWith("0")) "2$digits" else digits
            i.putExtra("jid", "$intl@s.whatsapp.net")
        }
        for (pkg in listOf("com.whatsapp", "com.whatsapp.w4b")) {
            try {
                ctx.startActivity(Intent(i).setPackage(pkg))
                return
            } catch (_: ActivityNotFoundException) {
            }
        }
        i.removeExtra("jid")
        ctx.startActivity(Intent.createChooser(i, "مشاركة التقرير"))
    }

    fun print(ctx: Context, f: File) {
        val pm = ctx.getSystemService(Context.PRINT_SERVICE) as PrintManager
        pm.print(f.nameWithoutExtension, object : PrintDocumentAdapter() {
            override fun onLayout(
                oldAttributes: PrintAttributes?, newAttributes: PrintAttributes?,
                cancellationSignal: CancellationSignal?, callback: LayoutResultCallback?, extras: Bundle?,
            ) {
                if (cancellationSignal?.isCanceled == true) { callback?.onLayoutCancelled(); return }
                val info = PrintDocumentInfo.Builder(f.name)
                    .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                    .build()
                callback?.onLayoutFinished(info, true)
            }

            override fun onWrite(
                pages: Array<out PageRange>?, destination: ParcelFileDescriptor?,
                cancellationSignal: CancellationSignal?, callback: WriteResultCallback?,
            ) {
                try {
                    f.inputStream().use { input ->
                        FileOutputStream(destination!!.fileDescriptor).use { input.copyTo(it) }
                    }
                    callback?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
                } catch (e: Exception) {
                    callback?.onWriteFailed(e.message)
                }
            }
        }, null)
    }
}
