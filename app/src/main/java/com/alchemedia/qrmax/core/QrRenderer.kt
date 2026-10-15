package com.alchemedia.qrmax.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import androidx.core.graphics.PathParser
import com.alchemedia.qrmax.R
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.qrcode.QRCodeReader
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.google.zxing.qrcode.encoder.Encoder
import java.io.File
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Draws styled QR codes and barcodes, and test-scans them so we never hand out a code that does not read. */
object QrRenderer {
    class Out(val bitmap: Bitmap? = null, val info: String = "", val verified: Boolean? = null, val note: String? = null, val error: String? = null)

    private val fitCache = object : LinkedHashMap<String, Pair<QrStyle, Boolean>>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Pair<QrStyle, Boolean>>?) = size > 30
    }
    private val logoCache = HashMap<String, Bitmap?>()

    fun logoFile(ctx: Context, name: String) = File(File(ctx.filesDir, "logos"), name)
    private fun logoBitmap(ctx: Context, name: String): Bitmap? = logoCache.getOrPut(name) { runCatching { BitmapFactory.decodeFile(logoFile(ctx, name).path) }.getOrNull() }

    /** Main entry. fmt is "qr" or a barcode format name (CODE128, EAN13, UPC, CODE39). */
    @Synchronized
    fun render(ctx: Context, raw: String, fmt: String, st: QrStyle, px: Int, verify: Boolean = false): Out {
        if (fmt != "qr") return barcode(ctx, raw, fmt, st)
        return try {
            var eff = st; var verified: Boolean? = null; var note: String? = null
            if (st.logo.active) {
                val key = raw + "|" + st.hashCode() + "|" + st.logo.image
                val hit = fitCache[key]
                if (hit != null) { eff = hit.first; verified = hit.second }
                else {
                    val chosen = st.logo.size; var found = false; var size = chosen
                    while (size >= 8) {
                        eff = st.copy(logo = st.logo.copy(size = size))
                        if (verifyQr(drawQr(ctx, raw, eff, 420), raw)) { found = true; break }
                        size -= 2
                    }
                    if (!found) eff = st.copy(logo = st.logo.copy(size = chosen))
                    verified = found; fitCache[key] = eff to found
                }
                if (verified == true && eff.logo.size < st.logo.size) note = ctx.getString(R.string.l_shrunk, eff.logo.size)
            }
            val bmp = drawQr(ctx, raw, eff, px)
            if (verified == null && verify) verified = verifyQr(drawQr(ctx, raw, eff, 420), raw)
            Out(bmp, ctx.getString(R.string.info_qr, raw.length), verified, note)
        } catch (e: Exception) { Out(error = ctx.getString(R.string.err_toolong)) }
    }

    fun verifyQr(bmp: Bitmap, raw: String): Boolean = try {
        val s = min(1f, 480f / max(bmp.width, bmp.height))
        val b = if (s < 1f) Bitmap.createScaledBitmap(bmp, (bmp.width * s).roundToInt(), (bmp.height * s).roundToInt(), true) else bmp
        val px = IntArray(b.width * b.height); b.getPixels(px, 0, b.width, 0, 0, b.width, b.height)
        // Flatten transparent corners (frames) onto white so the reader sees a clean image.
        for (i in px.indices) { val a = px[i] ushr 24; if (a < 255) px[i] = 0xFFFFFFFF.toInt() }
        val res = QRCodeReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(b.width, b.height, px))), mapOf(DecodeHintType.TRY_HARDER to true, DecodeHintType.CHARACTER_SET to "UTF-8"))
        res.text == raw
    } catch (e: Exception) { false }

    private fun roundRect(c: Canvas, x: Float, y: Float, w: Float, h: Float, r: Float, p: Paint) { val rr = min(r, min(w, h) / 2f); c.drawRoundRect(RectF(x, y, x + w, y + h), rr, rr, p) }
    /** Radii order: top-left, top-right, bottom-right, bottom-left. */
    private fun corners(path: Path, x: Float, y: Float, w: Float, h: Float, tl: Float, tr: Float, br: Float, bl: Float) =
        path.addRoundRect(RectF(x, y, x + w, y + h), floatArrayOf(tl, tl, tr, tr, br, br, bl, bl), Path.Direction.CW)

    private val heartPath: Path by lazy { PathParser.createPathFromPathData(IconPaths.map.getValue("heart")) }
    private fun starPath(cx: Float, cy: Float, outer: Float, inner: Float): Path {
        val p = Path()
        for (i in 0 until 10) { val a = -PI / 2 + i * PI / 5; val r = if (i % 2 == 1) inner else outer; val x = (cx + cos(a) * r).toFloat(); val y = (cy + sin(a) * r).toFloat(); if (i == 0) p.moveTo(x, y) else p.lineTo(x, y) }
        p.close(); return p
    }

    private fun drawQr(ctx: Context, text: String, st: QrStyle, px: Int): Bitmap {
        val logo = st.logo; val withLogo = logo.active
        val ecl = if (withLogo) ErrorCorrectionLevel.H else when (st.ecl) { 'L' -> ErrorCorrectionLevel.L; 'Q' -> ErrorCorrectionLevel.Q; 'H' -> ErrorCorrectionLevel.H; else -> ErrorCorrectionLevel.M }
        val matrix = Encoder.encode(text, ecl, mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")).matrix
        val n = matrix.width; val q = st.margin
        val cell = max(2, px / (n + 2 * q)).toFloat()
        val code = cell * (n + 2 * q)
        val fk = st.frame.kind
        val pad = if (fk == FrameKind.None) 0f else cell * 2.2f; val bar = cell * 7f
        val w = code + 2 * pad; var h = w; val ox = pad; var oy = pad
        when (fk) { FrameKind.Bottom -> h = w + bar; FrameKind.Top -> { h = w + bar; oy = pad + bar }; FrameKind.Tag -> h = w + cell * 2.6f; else -> {} }
        val bmp = Bitmap.createBitmap(ceil(w).toInt(), ceil(h).toInt(), Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = st.fg; if (st.grad) shader = LinearGradient(ox, oy, ox + code, oy + code, st.fg, st.fg2, Shader.TileMode.CLAMP) }
        val frameFill = if (st.frame.color != null) Paint(Paint.ANTI_ALIAS_FLAG).apply { color = st.frame.color!! } else fill
        val ink = if (Contrast.lum(st.frame.color ?: st.fg) < 0.45) 0xFFFFFFFF.toInt() else 0xFF0E1626.toInt()
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = st.bg }
        val label = st.frame.text.ifEmpty { "Scan me" }
        val text1 = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
        fun fit(maxW: Float, size: Float) { var f = size; do { text1.textSize = f; f -= 1f } while (text1.measureText(label) > maxW && f > 6f) }
        fun drawText(t: String, cx: Float, cy: Float, p: Paint) { val m = p.fontMetrics; c.drawText(t, cx, cy - (m.ascent + m.descent) / 2f, p) }
        when (fk) {
            FrameKind.None -> c.drawRect(0f, 0f, w, h, bgPaint)
            FrameKind.Tag -> { roundRect(c, 0f, 0f, w, w, cell * 2.6f, frameFill); roundRect(c, cell * .9f, cell * .9f, w - cell * 1.8f, w - cell * 1.8f, cell * 1.8f, bgPaint) }
            else -> {
                roundRect(c, 0f, 0f, w, h, cell * 2.6f, frameFill); roundRect(c, ox, oy, code, code, cell * 1.3f, bgPaint)
                if (fk == FrameKind.Bottom || fk == FrameKind.Top) {
                    text1.color = ink; fit(w * .8f, cell * 3.4f)
                    drawText(label, w / 2f, if (fk == FrameKind.Bottom) pad + code + (pad + bar) / 2f + cell * .2f else (pad + bar) / 2f + cell * .2f, text1)
                }
            }
        }
        var ln = 0; var l0 = 0; var l1 = 0
        if (withLogo) { ln = (n * logo.size / 100f).roundToInt(); if ((n - ln) % 2 != 0) ln++; l0 = (n - ln) / 2; l1 = l0 + ln }
        fun isEye(r: Int, k: Int) = (r < 7 && k < 7) || (r < 7 && k >= n - 7) || (r >= n - 7 && k < 7)
        fun inLogo(r: Int, k: Int) = withLogo && r in l0 until l1 && k in l0 until l1
        fun on(r: Int, k: Int) = r in 0 until n && k in 0 until n && matrix.get(k, r).toInt() == 1 && !isEye(r, k) && !inLogo(r, k)
        val rd = st.round.coerceIn(0f, 0.5f)
        val path = Path()
        for (r in 0 until n) for (k in 0 until n) {
            if (!on(r, k)) continue
            val x = ox + (k + q) * cell; val y = oy + (r + q) * cell; val h2 = cell / 2f
            when (st.shape) {
                Shape.Round -> roundRect(c, x, y, cell, cell, cell * rd, fill)
                Shape.Dots -> c.drawCircle(x + h2, y + h2, cell * .49f, fill)
                Shape.Classy -> { path.rewind(); corners(path, x, y, cell, cell, h2, 0f, h2, 0f); c.drawPath(path, fill) }
                Shape.Diamond -> { path.rewind(); path.moveTo(x + h2, y - cell * .16f); path.lineTo(x + cell * 1.16f, y + h2); path.lineTo(x + h2, y + cell * 1.16f); path.lineTo(x - cell * .16f, y + h2); path.close(); c.drawPath(path, fill) }
                Shape.Bars -> { val lc = if (on(r, k - 1)) 0f else h2; val rc = if (on(r, k + 1)) 0f else h2; path.rewind(); corners(path, x, y, cell + (if (rc == 0f) .5f else 0f), cell, lc, rc, rc, lc); c.drawPath(path, fill) }
                Shape.Heart -> { val m = Matrix(); val s = cell / 14f * 1.08f; m.postTranslate(-12f, -13.2f); m.postScale(s, s); m.postTranslate(x + h2, y + h2 * 1.04f); path.rewind(); heartPath.transform(m, path); c.drawPath(path, fill) }
                Shape.Star -> c.drawPath(starPath(x + h2, y + h2 * 1.04f, cell * .74f, cell * .36f), fill)
                Shape.Square -> c.drawRect(x, y, x + cell, y + cell, fill)
            }
        }
        val eyePaint = if (st.eyeSame) fill else Paint(Paint.ANTI_ALIAS_FLAG).apply { color = st.eyeColor }
        for ((er, ek) in listOf(0 to 0, 0 to n - 7, n - 7 to 0)) {
            val x = ox + (ek + q) * cell; val y = oy + (er + q) * cell; val s = 7 * cell; val cx = x + s / 2; val cy = y + s / 2; val hole = s - 2 * cell
            val ring = Path().apply { fillType = Path.FillType.EVEN_ODD }
            when (st.eye) {
                Eye.Circle -> { ring.addCircle(cx, cy, s / 2, Path.Direction.CW); ring.addCircle(cx, cy, s / 2 - cell, Path.Direction.CW) }
                Eye.Round -> { ring.addRoundRect(RectF(x, y, x + s, y + s), cell * 2.4f, cell * 2.4f, Path.Direction.CW); ring.addRoundRect(RectF(x + cell, y + cell, x + cell + hole, y + cell + hole), cell * 1.4f, cell * 1.4f, Path.Direction.CW) }
                Eye.Leaf -> { corners(ring, x, y, s, s, cell * 3.2f, cell * .5f, cell * 3.2f, cell * .5f); corners(ring, x + cell, y + cell, hole, hole, cell * 2f, cell * .15f, cell * 2f, cell * .15f) }
                Eye.Square -> { ring.addRect(x, y, x + s, y + s, Path.Direction.CW); ring.addRect(x + cell, y + cell, x + cell + hole, y + cell + hole, Path.Direction.CW) }
            }
            c.drawPath(ring, eyePaint)
            val bx = x + 2 * cell; val by = y + 2 * cell; val bs = 3 * cell
            when (st.ball) { Ball.Circle -> c.drawCircle(cx, cy, cell * 1.65f, eyePaint); Ball.Round -> roundRect(c, bx, by, bs, bs, cell * 1.1f, eyePaint); Ball.Square -> c.drawRect(bx, by, bx + bs, by + bs, eyePaint) }
        }
        if (withLogo) {
            val lx = ox + (l0 + q) * cell; val ly = oy + (l0 + q) * cell; val ls = ln * cell; val cx2 = lx + ls / 2; val cy2 = ly + ls / 2
            if (logo.plate != Plate.None) { if (logo.plate == Plate.Round) c.drawCircle(cx2, cy2, ls / 2, bgPaint) else roundRect(c, lx, ly, ls, ls, ls * .28f, bgPaint) }
            when (logo.kind) {
                LogoKind.Icon -> {
                    val p = Path(PathParser.createPathFromPathData(IconPaths.map[logo.icon] ?: IconPaths.map.getValue("link")))
                    val m = Matrix(); val sc = ls * .56f / 24f; m.postTranslate(-12f, -12f); m.postScale(sc, sc); m.postTranslate(cx2, cy2); p.transform(m)
                    val sp = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE; color = st.fg; strokeWidth = 2.2f * sc; strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND }
                    c.drawPath(p, sp)
                }
                LogoKind.Text -> {
                    val t = logo.text.ifEmpty { "QR" }.take(3)
                    val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = st.fg; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD); textAlign = Paint.Align.CENTER }
                    var f = ls * .46f; do { tp.textSize = f; f -= 1f } while (tp.measureText(t) > ls * .74f && f > 6f)
                    drawText(t, cx2, cy2 + ls * .02f, tp)
                }
                LogoKind.Image -> logo.image?.let { name ->
                    logoBitmap(ctx, name)?.let { img ->
                        val box = ls * (if (logo.plate == Plate.None) .96f else .8f)
                        val sc = min(box / img.width, box / img.height); val w2 = img.width * sc; val h3 = img.height * sc
                        c.save()
                        val clip = Path(); if (logo.plate == Plate.Round) clip.addCircle(cx2, cy2, box / 2, Path.Direction.CW) else clip.addRoundRect(RectF(cx2 - box / 2, cy2 - box / 2, cx2 + box / 2, cy2 + box / 2), box * .22f, box * .22f, Path.Direction.CW)
                        c.clipPath(clip)
                        c.drawBitmap(img, null, RectF(cx2 - w2 / 2, cy2 - h3 / 2, cx2 + w2 / 2, cy2 + h3 / 2), Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
                        c.restore()
                    }
                }
                LogoKind.None -> {}
            }
        }
        if (fk == FrameKind.Tag) {
            text1.color = ink; fit(w * .7f, cell * 3f)
            val tw = text1.measureText(label); val pw = max(tw + cell * 5, cell * 12); val ph = cell * 5; val pxx = (w - pw) / 2; val pyy = w - ph / 2
            roundRect(c, pxx, pyy, pw, ph, ph / 2, frameFill); drawText(label, w / 2, pyy + ph / 2 + cell * .15f, text1)
        }
        return bmp
    }

    private fun barcode(ctx: Context, raw: String, fmt: String, st: QrStyle): Out {
        val format = when (fmt) { "EAN13" -> BarcodeFormat.EAN_13; "UPC" -> BarcodeFormat.UPC_A; "CODE39" -> BarcodeFormat.CODE_39; else -> BarcodeFormat.CODE_128 }
        val m = try { MultiFormatWriter().encode(raw, format, 0, 1, mapOf(EncodeHintType.MARGIN to 10)) } catch (e: Exception) {
            return Out(error = ctx.getString(when (fmt) { "EAN13" -> R.string.err_ean13; "UPC" -> R.string.err_upc; "CODE39" -> R.string.err_code39; else -> R.string.err_barcode }))
        }
        val scale = 3; val barH = 220; val textH = if (st.barText) 64 else 0; val w = m.width * scale; val h = barH + textH + 16
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        c.drawColor(st.bg)
        val p = Paint().apply { color = st.fg; style = Paint.Style.FILL; isAntiAlias = false }
        for (x in 0 until m.width) if (m.get(x, 0)) c.drawRect((x * scale).toFloat(), 8f, ((x + 1) * scale).toFloat(), 8f + barH, p)
        if (st.barText) { val tp = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = st.fg; textSize = 44f; textAlign = Paint.Align.CENTER; typeface = Typeface.MONOSPACE }; c.drawText(raw, w / 2f, barH + 8f + 48f, tp) }
        val name = when (fmt) { "EAN13" -> "EAN-13"; "UPC" -> "UPC-A"; "CODE39" -> "Code 39"; else -> "Code 128" }
        return Out(bmp, ctx.getString(R.string.info_bc, name, raw.length))
    }
}
