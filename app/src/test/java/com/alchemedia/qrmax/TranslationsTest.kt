package com.alchemedia.qrmax

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class TranslationsTest {
    private val folders = mapOf("en" to "values", "es" to "values-es", "de" to "values-de", "fr" to "values-fr", "it" to "values-it", "pt" to "values-pt", "hi" to "values-hi", "id" to "values-in")
    private fun keys(folder: String): Map<String, String> {
        val f = File("src/main/res/$folder/strings.xml"); assertTrue("${f.path} exists", f.exists())
        return Regex("<string name=\"([a-z0-9_]+)\">(.*?)</string>").findAll(f.readText()).associate { it.groupValues[1] to it.groupValues[2] }
    }

    @Test fun allEightLanguagesHaveEveryString() {
        val en = keys("values")
        folders.forEach { (lang, folder) ->
            val k = keys(folder)
            assertEquals("$lang is missing keys", emptySet<String>(), en.keys - k.keys)
            assertEquals("$lang has extra keys", emptySet<String>(), k.keys - en.keys)
            assertTrue("$lang has empty strings", k.values.none { it.isBlank() })
        }
    }

    @Test fun placeholdersMatchInEveryLanguage() {
        val en = keys("values"); val ph = Regex("%\\d\\\$s")
        folders.values.forEach { folder -> keys(folder).forEach { (key, v) -> assertEquals("$folder/$key", ph.findAll(en.getValue(key)).map { it.value }.toSet(), ph.findAll(v).map { it.value }.toSet()) } }
    }

    @Test fun realTranslationsAreLoadedForEachLocale() {
        val base: Context = ApplicationProvider.getApplicationContext()
        fun s(lang: String, id: Int) = base.createConfigurationContext(Configuration().apply { setLocale(Locale.forLanguageTag(lang)) }).getString(id)
        assertEquals("Scannen", s("de", R.string.tab_scan)); assertEquals("Escanear", s("es", R.string.tab_scan)); assertEquals("Scanner", s("fr", R.string.tab_scan))
        assertEquals("Scansiona", s("it", R.string.tab_scan)); assertEquals("Digitalizar", s("pt", R.string.tab_scan)); assertEquals("स्कैन", s("hi", R.string.tab_scan)); assertEquals("Pindai", s("id", R.string.tab_scan))
        assertEquals("Scan", s("en", R.string.tab_scan))
        assertEquals("Desbloquear Pro por £4.99", base.createConfigurationContext(Configuration().apply { setLocale(Locale("es")) }).getString(R.string.pro_buy, "£4.99"))
    }

    @Test fun appNameStaysQrMaxEverywhere() { folders.values.forEach { assertEquals("QR Max", keys(it)["app_name"]) } }
}
