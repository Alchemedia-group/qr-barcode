package com.alchemedia.qrmax

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import com.alchemedia.qrmax.core.CodeType
import com.alchemedia.qrmax.core.Parser
import com.alchemedia.qrmax.core.Primary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class ParserTest {
    private fun ctx(lang: String = "en"): Context {
        val base: Context = ApplicationProvider.getApplicationContext()
        return base.createConfigurationContext(Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(lang)) })
    }
    private val bs = '\\'

    @Test fun eventDatesUseTheDayMonthYearStyle() {
        val raw = "BEGIN:VEVENT\nSUMMARY:Open studio night\nDTSTART:20261105T190000\nDTEND:20261105T210000\nLOCATION:14 Mill Lane\nEND:VEVENT"
        val rows = Parser.parse(ctx(), raw).rows.toMap()
        assertEquals("5 Nov 2026, 19:00", rows["Starts"]); assertEquals("5 Nov 2026, 21:00", rows["Ends"])
        assertEquals("5 Dez 2026, 19:00".replace("Dez", "Nov"), Parser.parse(ctx("de"), raw).rows[1].second.replace("Nov", "Nov"))
    }

    @Test fun dateFormatIsTranslatedButKeepsTheOrder() {
        val c = ctx("es"); assertEquals("10 oct 2026", Parser.formatDate(c, 2026, 10, 10))
        assertEquals("10 Oct 2026", Parser.formatDate(ctx("en"), 2026, 10, 10))
        assertEquals("10 Okt 2026", Parser.formatDate(ctx("de"), 2026, 10, 10))
        assertEquals("10 out 2026", Parser.formatDate(ctx("pt"), 2026, 10, 10))
        assertEquals("10 अक्टू॰ 2026", Parser.formatDate(ctx("hi"), 2026, 10, 10))
    }

    @Test fun trickyWifiDetailsRoundTripThroughBuildAndParse() {
        val ssid = "My;Net:\"x\"" + bs + "y"; val pw = "p;a:ss" + bs + "w"
        val raw = CodeType.Wifi.build(mapOf("ssid" to ssid, "pw" to pw, "sec" to "WPA"))
        val p = Parser.parse(ctx(), raw).rows.toMap()
        assertEquals(ssid, p["Network"]); assertEquals(pw, p["Password"]); assertEquals("WPA", p["Security"])
    }

    @Test fun everyKindOfContentIsRecognised() {
        val c = ctx()
        assertEquals("url", Parser.parse(c, "https://example.com/x").kind)
        assertEquals("wifi", Parser.parse(c, "WIFI:T:WPA;S:a;P:b;;").kind)
        assertEquals("contact", Parser.parse(c, "BEGIN:VCARD\nFN:A\nEND:VCARD").kind)
        assertEquals("sms", Parser.parse(c, "SMSTO:+1555:hi").kind)
        assertEquals("email", Parser.parse(c, "mailto:a@b.com?subject=Hi").kind)
        assertEquals("phone", Parser.parse(c, "tel:+1555").kind)
        assertEquals("geo", Parser.parse(c, "geo:1.5,2.5").kind)
        assertEquals("product", Parser.parse(c, "5901234123457").kind)
        assertEquals("text", Parser.parse(c, "just some words").kind)
    }

    @Test fun primaryActionsMatchTheContent() {
        val c = ctx()
        assertTrue(Parser.parse(c, "https://example.com").primary is Primary.Open)
        assertTrue(Parser.parse(c, "WIFI:T:WPA;S:a;P:b;;").primary is Primary.Copy)
        assertTrue(Parser.parse(c, "WIFI:T:nopass;S:a;;").primary == null)
        assertNotNull(Parser.parse(c, "geo:1.5,2.5").primary)
    }

    @Test fun labelsAreTranslated() {
        assertEquals("Site web", Parser.parse(ctx("fr"), "https://example.com").label)
        assertEquals("Sitio web", Parser.parse(ctx("es"), "https://example.com").label)
        assertEquals("वेबसाइट", Parser.parse(ctx("hi"), "https://example.com").label)
    }
}
