package com.alchemedia.qrmax.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.util.UUID
import kotlin.math.max

object Logos {
    /** Copies a picked picture into app storage at a small size and returns its file name, or null if it cannot be read. */
    fun import(ctx: Context, uri: Uri): String? = try {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        var sample = 1; while (max(opts.outWidth, opts.outHeight) / (sample * 2) >= 320) sample *= 2
        val src = ctx.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) } ?: throw IllegalStateException()
        val scale = minOf(1f, 160f / max(src.width, src.height))
        val bmp = if (scale < 1f) Bitmap.createScaledBitmap(src, (src.width * scale).toInt().coerceAtLeast(1), (src.height * scale).toInt().coerceAtLeast(1), true) else src
        val dir = File(ctx.filesDir, "logos").apply { mkdirs() }
        val name = UUID.randomUUID().toString() + ".png"
        File(dir, name).outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
        name
    } catch (e: Exception) { null }
}
