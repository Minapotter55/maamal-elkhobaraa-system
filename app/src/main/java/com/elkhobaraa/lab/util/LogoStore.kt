package com.elkhobaraa.lab.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.elkhobaraa.lab.R
import java.io.File

/** The lab logo: a custom image chosen in Settings, or the built-in flask logo. */
object LogoStore {
    var version by mutableIntStateOf(0)
        private set

    private fun file(ctx: Context) = File(ctx.filesDir, "logo.png")

    fun custom(ctx: Context): Bitmap? {
        val f = file(ctx)
        return if (f.exists()) BitmapFactory.decodeFile(f.path) else null
    }

    fun save(ctx: Context, uri: Uri): Boolean {
        val bmp = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it) } ?: return false
        file(ctx).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        version++
        return true
    }

    fun reset(ctx: Context) {
        file(ctx).delete()
        version++
    }

    /** Square bitmap of the logo on white, for the PDF report. */
    fun bitmap(ctx: Context, size: Int): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        c.drawColor(0xFFFFFFFF.toInt())
        val src = custom(ctx) ?: BitmapFactory.decodeResource(ctx.resources, R.drawable.lab_logo)
        val scale = minOf(size.toFloat() / src.width, size.toFloat() / src.height)
        val w = src.width * scale
        val h = src.height * scale
        c.drawBitmap(src, null, RectF((size - w) / 2, (size - h) / 2, (size + w) / 2, (size + h) / 2), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }
}
