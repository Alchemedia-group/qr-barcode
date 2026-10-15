package com.alchemedia.qrmax

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.core.app.ApplicationProvider
import com.alchemedia.qrmax.core.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class RendererTest {
    private lateinit var ctx: Context
    private val url = "https://example.com/spring-menu"

    @Before fun setUp() {
        ctx = ApplicationProvider.getApplicationContext()
        // a simple orange badge to use as a picture logo
        val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888); val c = Canvas(bmp)
        c.drawCircle(60f, 60f, 52f, Paint().apply { color = Color.rgb(234, 88, 12); isAntiAlias = true })
        c.drawRect(46f, 30f, 74f, 90f, Paint().apply { color = Color.WHITE }); c.drawRect(30f, 46f, 90f, 74f, Paint().apply { color = Color.WHITE })
        File(ctx.filesDir, "logos").mkdirs(); File(ctx.filesDir, "logos/test.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun scans(raw: String, st: QrStyle): Boolean = QrRenderer.render(ctx, raw, "qr", st, 600, verify = true).let { it.error == null && it.verified == true }
    private val imageLogo get() = LogoStyle(kind = LogoKind.Image, image = "test.png")

    @Test fun everyDefaultCodeTypeScans() {
        CodeType.entries.filter { it != CodeType.Barcode }.forEach { t -> assertTrue("${t.key} should scan", scans(t.build(t.defaults()), QrStyle())) }
    }

    @Test fun everyBodyShapeCornerStyleAndFrameScans() {
        val failures = mutableListOf<String>()
        Shape.entries.forEach { s -> if (!scans(url, QrStyle(shape = s))) failures += "shape ${s.key}" }
        Eye.entries.forEach { e -> Ball.entries.forEach { b -> if (!scans(url, QrStyle(eye = e, ball = b))) failures += "eye ${e.key} + ball ${b.key}" } }
        FrameKind.entries.forEach { f -> if (!scans(url, QrStyle(frame = FrameStyle(f, "Scan me")))) failures += "frame ${f.key}" }
        assertTrue("These styles did not scan: $failures", failures.isEmpty())
    }

    @Test fun logosOfEveryKindStillScan() {
        val failures = mutableListOf<String>()
        listOf(imageLogo, LogoStyle(kind = LogoKind.Icon, icon = "wifi"), LogoStyle(kind = LogoKind.Text, text = "NS")).forEach { logo ->
            Plate.entries.forEach { p -> if (!scans(url, QrStyle(logo = logo.copy(plate = p)))) failures += "${logo.kind} plate ${p.key}" }
            Shape.entries.forEach { s -> if (!scans(url, QrStyle(shape = s, logo = logo))) failures += "${logo.kind} + shape ${s.key}" }
            if (!scans(url, QrStyle(logo = logo, frame = FrameStyle(FrameKind.Tag, "Menu")))) failures += "${logo.kind} + frame"
        }
        IconPaths.logoChoices.forEach { i -> if (!scans(url, QrStyle(logo = LogoStyle(kind = LogoKind.Icon, icon = i)))) failures += "icon $i" }
        assertTrue("Logo combinations that did not scan: $failures", failures.isEmpty())
    }

    @Test fun anOversizedLogoShrinksInsteadOfBreakingTheCode() {
        val out = QrRenderer.render(ctx, url, "qr", QrStyle(logo = imageLogo.copy(size = 26)), 600, verify = true)
        assertEquals(true, out.verified)
    }

    @Test fun gradientsAndThemesWithALogoScan() {
        val themes = listOf(
            QrStyle(fg = 0xFF0B3D91.toInt(), fg2 = 0xFF0E7490.toInt(), grad = true, shape = Shape.Round, eye = Eye.Round, ball = Ball.Round),
            QrStyle(fg = 0xFF701A75.toInt(), fg2 = 0xFFBE185D.toInt(), grad = true, shape = Shape.Classy, eye = Eye.Leaf, ball = Ball.Round),
            QrStyle(fg = 0xFF9A3412.toInt(), fg2 = 0xFF9F1239.toInt(), grad = true, shape = Shape.Bars, eye = Eye.Round, ball = Ball.Round),
            QrStyle(fg = 0xFF064E3B.toInt(), bg = 0xFFD1FAE5.toInt(), shape = Shape.Dots, eye = Eye.Circle, ball = Ball.Circle),
            QrStyle(fg = 0xFF3B2A1A.toInt(), bg = 0xFFF6ECD9.toInt(), shape = Shape.Classy),
        )
        themes.forEachIndexed { i, t -> assertTrue("theme $i", scans(url, t)); assertTrue("theme $i + logo", scans(url, t.copy(logo = imageLogo))) }
    }

    @Test fun theTestScanCatchesAFaintCode() {
        val faint = QrStyle(fg = 0xFFE6E6E6.toInt(), bg = 0xFFFFFFFF.toInt())
        assertFalse("a near-white code on white must be flagged", scans(url, faint))
    }

    @Test fun unicodeSurvivesTheRoundTrip() { assertTrue(scans("Café ☕ 日本語 😀 naïve", QrStyle())) }

    @Test fun tooMuchTextGivesAClearErrorNotACrash() {
        val out = QrRenderer.render(ctx, "x".repeat(6000), "qr", QrStyle(), 600)
        assertNull(out.bitmap); assertNotNull(out.error)
    }

    @Test fun barcodesAcceptGoodValuesAndRejectBadOnes() {
        assertNotNull(QrRenderer.render(ctx, "abc-123", "CODE128", QrStyle(), 600).bitmap)
        assertNotNull(QrRenderer.render(ctx, "5901234123457", "EAN13", QrStyle(), 600).bitmap)
        assertNotNull(QrRenderer.render(ctx, "590123412345", "EAN13", QrStyle(), 600).bitmap)
        assertNotNull(QrRenderer.render(ctx, "123456789012", "UPC", QrStyle(), 600).bitmap)
        assertNotNull(QrRenderer.render(ctx, "ABC-123", "CODE39", QrStyle(), 600).bitmap)
        assertNotNull(QrRenderer.render(ctx, "abc", "EAN13", QrStyle(), 600).error)
        assertNotNull(QrRenderer.render(ctx, "5901234123450", "EAN13", QrStyle(), 600).error)
        assertNotNull(QrRenderer.render(ctx, "12", "UPC", QrStyle(), 600).error)
        assertNotNull(QrRenderer.render(ctx, "", "CODE128", QrStyle(), 600).error)
    }

    @Test fun frameAddsSpaceAroundTheCode() {
        val plain = QrRenderer.render(ctx, url, "qr", QrStyle(), 600).bitmap!!
        val framed = QrRenderer.render(ctx, url, "qr", QrStyle(frame = FrameStyle(FrameKind.Bottom, "Scan me")), 600).bitmap!!
        assertTrue(framed.height > plain.height && framed.width > plain.width)
    }
}
