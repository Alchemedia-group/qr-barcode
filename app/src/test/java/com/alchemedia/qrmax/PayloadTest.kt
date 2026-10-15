package com.alchemedia.qrmax

import com.alchemedia.qrmax.core.CodeType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PayloadTest {
    private val bs = '\\'

    @Test fun urlGetsHttpsWhenSchemeIsMissing() {
        assertEquals("https://example.org/a?x=1", CodeType.Url.build(mapOf("url" to "example.org/a?x=1")))
        assertEquals("mailto:me@example.com", CodeType.Url.build(mapOf("url" to "mailto:me@example.com")))
        assertEquals("", CodeType.Url.build(mapOf("url" to "   ")))
    }

    @Test fun wifiEscapesSpecialCharactersIncludingBackslash() {
        val ssid = "My;Net:\"x\"" + bs + "y"; val pw = "p;a:ss" + bs + "w"
        val want = "WIFI:T:WPA;S:My" + bs + ";Net" + bs + ":" + bs + "\"x" + bs + "\"" + bs + bs + "y;P:p" + bs + ";a" + bs + ":ss" + bs + bs + "w;;"
        assertEquals(want, CodeType.Wifi.build(mapOf("ssid" to ssid, "pw" to pw, "sec" to "WPA")))
    }

    @Test fun wifiWithoutSecurityHasNoPasswordAndHiddenFlagIsKept() {
        val raw = CodeType.Wifi.build(mapOf("ssid" to "Cafe", "pw" to "secret", "sec" to "nopass", "hid" to "1"))
        assertTrue(!raw.contains("P:")); assertTrue(raw.contains("H:true;"))
    }

    @Test fun eventTimesCarrySeconds() {
        val raw = CodeType.Event.build(mapOf("title" to "Night", "start" to "2026-11-05T19:00", "end" to "2026-11-05T21:00", "where" to "14 Mill Lane"))
        assertTrue(raw, raw.contains("DTSTART:20261105T190000")); assertTrue(raw.contains("DTEND:20261105T210000")); assertTrue(raw.contains("LOCATION:14 Mill Lane"))
        assertTrue(!CodeType.Event.build(mapOf("title" to "Night", "start" to "", "end" to "")).contains("DTSTART"))
    }

    @Test fun emailEncodesSubjectAndBody() {
        val raw = CodeType.Email.build(mapOf("to" to "a@b.com", "sub" to "Hi there", "body" to "x&y"))
        assertEquals("mailto:a@b.com?subject=Hi%20there&body=x%26y", raw)
    }

    @Test fun geoNeedsRealNumbers() {
        assertEquals("", CodeType.Geo.build(mapOf("lat" to "91abc", "lng" to "1")))
        assertEquals("geo:-33.8688,151.2093", CodeType.Geo.build(mapOf("lat" to "-33.8688", "lng" to "151.2093")))
    }

    @Test fun contactOmitsEmptyOptionalFieldsAndNeedsAName() {
        val raw = CodeType.Contact.build(mapOf("name" to "Jordan", "org" to "", "tel" to "", "email" to ""))
        assertEquals("BEGIN:VCARD\nVERSION:3.0\nFN:Jordan\nEND:VCARD", raw)
        assertEquals("", CodeType.Contact.build(mapOf("name" to "")))
    }

    @Test fun everyTypeHasWorkingDefaults() {
        CodeType.entries.forEach { t -> assertTrue("${t.key} default payload", t.build(t.defaults()).isNotEmpty()) }
    }
}
