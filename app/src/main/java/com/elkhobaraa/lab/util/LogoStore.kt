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
import androidx.core.content.ContextCompat
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

    /** Square bitmap of the logo, for the PDF report. */
    fun bitmap(ctx: Context, size: Int): Bitmap {
        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(out)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = 0xFFFFFFFF.toInt()
        c.drawCircle(size / 2f, size / 2f, size / 2f, p)
        val custom = custom(ctx)
        if (custom != null) {
            val scale = minOf(size * 0.86f / custom.width, size * 0.86f / custom.height)
            val w = custom.width * scale
            val h = custom.height * scale
            c.drawBitmap(custom, null, RectF((size - w) / 2, (size - h) / 2, (size + w) / 2, (size + h) / 2), p)
        } else {
            p.color = 0xFF0B5FA5.toInt()
            c.drawCircle(size / 2f, size / 2f, size / 2f - size * 0.04f, p)
            val d = ContextCompat.getDrawable(ctx, R.drawable.ic_logo_fg)!!
            d.setBounds(0, 0, size, size)
            d.draw(c)
        }
        return out
    }
}
