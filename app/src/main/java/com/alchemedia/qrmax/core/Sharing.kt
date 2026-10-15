package com.alchemedia.qrmax.core

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.core.content.FileProvider
import com.alchemedia.qrmax.R
import java.io.File

/** Hands codes to whatever apps the phone has: the system share sheet lists them. */
object Sharing {
    private fun whiteBackdrop(src: Bitmap): Bitmap {
        val pad = (src.width * 0.06f).toInt().coerceAtLeast(12)
        val out = Bitmap.createBitmap(src.width + pad * 2, src.height + pad * 2, Bitmap.Config.ARGB_8888)
        val c = android.graphics.Canvas(out); c.drawColor(0xFFFFFFFF.toInt()); c.drawBitmap(src, pad.toFloat(), pad.toFloat(), null)
        return out
    }

    fun shareImage(ctx: Context, bitmap: Bitmap, text: String, title: String) {
        val dir = File(ctx.cacheDir, "shared").apply { mkdirs() }
        val f = File(dir, "qr-max-code.png")
        f.outputStream().use { whiteBackdrop(bitmap).compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(ctx, ctx.packageName + ".files", f)
        val i = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"; putExtra(Intent.EXTRA_STREAM, uri); putExtra(Intent.EXTRA_TEXT, text); putExtra(Intent.EXTRA_SUBJECT, title)
            clipData = ClipData.newRawUri(title, uri); addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        ctx.startActivity(Intent.createChooser(i, ctx.getString(R.string.share_chooser)))
    }

    fun shareText(ctx: Context, text: String, title: String) {
        val i = Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, text); putExtra(Intent.EXTRA_SUBJECT, title) }
        ctx.startActivity(Intent.createChooser(i, ctx.getString(R.string.share_chooser)))
    }

    fun copy(ctx: Context, text: String) {
        (ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager).setPrimaryClip(ClipData.newPlainText("QR Max", text))
    }

    /** Saves a PNG into Pictures/QR Max. Needs no permission on Android 10 and later. */
    fun saveToGallery(ctx: Context, bitmap: Bitmap): Boolean = try {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "QR-Max-" + System.currentTimeMillis() + ".png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/QR Max")
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri: Uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        ctx.contentResolver.openOutputStream(uri)!!.use { whiteBackdrop(bitmap).compress(Bitmap.CompressFormat.PNG, 100, it) }
        values.clear(); values.put(MediaStore.Images.Media.IS_PENDING, 0); ctx.contentResolver.update(uri, values, null, null)
        true
    } catch (e: Exception) { false }

    fun open(ctx: Context, uri: String): Boolean = try {
        val action = if (uri.startsWith("mailto:") || uri.startsWith("sms:") || uri.startsWith("tel:")) Intent.ACTION_VIEW else Intent.ACTION_VIEW
        ctx.startActivity(Intent(action, Uri.parse(uri)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true
    } catch (e: Exception) { false }
}
