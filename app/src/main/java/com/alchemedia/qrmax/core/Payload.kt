package com.alchemedia.qrmax.core

import android.content.Context
import androidx.annotation.StringRes
import com.alchemedia.qrmax.R
import java.net.URI
import java.net.URLEncoder
import java.util.Calendar

enum class FieldKind { Text, Url, Tel, Email, Area, Select, Switch, DateTime }

data class Field(
    val key: String,
    @StringRes val label: Int,
    val kind: FieldKind = FieldKind.Text,
    val default: String = "",
    val placeholder: String = "",
    @StringRes val placeholderRes: Int? = null,
    val options: List<Pair<String, Int>> = emptyList(),
    val canPaste: Boolean = false,
)

/** The kinds of code people can make, with their forms and payload builders. */
enum class CodeType(val key: String, val icon: String, @StringRes val label: Int, val hue: Int, val fields: List<Field>) {
    Url("url", "link", R.string.ty_url, 222, listOf(Field("url", R.string.f_url, FieldKind.Url, "https://example.com/spring-menu", "example.com", canPaste = true))),
    Text("text", "text", R.string.ty_text, 258, listOf(Field("text", R.string.f_text, FieldKind.Area, "Locker 42 · code 7719", placeholderRes = R.string.f_text_ph))),
    Wifi("wifi", "wifi", R.string.ty_wifi, 188, listOf(
        Field("ssid", R.string.f_ssid, default = "Guest-Lounge", placeholder = "Guest-Lounge"),
        Field("pw", R.string.f_pw, default = "welcome-2026"),
        Field("sec", R.string.f_sec, FieldKind.Select, "WPA", options = listOf("WPA" to R.string.f_sec_wpa, "WEP" to R.string.f_sec_wep, "nopass" to R.string.sec_none)),
        Field("hid", R.string.f_hidden, FieldKind.Switch, ""))),
    Contact("contact", "user", R.string.ty_contact, 32, listOf(
        Field("name", R.string.f_fullname, default = "Jordan Lee"), Field("org", R.string.f_company, default = "Northside Studio"),
        Field("tel", R.string.f_phone, FieldKind.Tel, "+15550142"), Field("email", R.string.f_email, FieldKind.Email, "jordan@example.com"))),
    Sms("sms", "sms", R.string.ty_sms, 150, listOf(Field("to", R.string.f_phone_number, FieldKind.Tel, "+15550142"), Field("msg", R.string.f_message, FieldKind.Area, "Running 10 minutes late"))),
    Email("email", "mail", R.string.ty_email, 340, listOf(Field("to", R.string.f_send_to, FieldKind.Email, "hello@example.com"), Field("sub", R.string.f_subject, default = "Booking enquiry"), Field("body", R.string.f_message, FieldKind.Area, ""))),
    Phone("phone", "phone", R.string.ty_phone, 120, listOf(Field("tel", R.string.f_phone_number, FieldKind.Tel, "+15550142"))),
    Event("event", "calendar", R.string.ty_event, 280, listOf(
        Field("title", R.string.f_eventname, default = "Open studio night"), Field("start", R.string.f_starts, FieldKind.DateTime, "2026-11-05T19:00"),
        Field("end", R.string.f_ends, FieldKind.DateTime, "2026-11-05T21:00"), Field("where", R.string.f_where, default = "14 Mill Lane"))),
    Geo("geo", "pin", R.string.ty_geo, 12, listOf(Field("lat", R.string.f_lat, default = "51.5072"), Field("lng", R.string.f_lng, default = "-0.1276"))),
    Barcode("barcode", "barcode", R.string.ty_barcode, 205, listOf(
        Field("fmt", R.string.f_barcode_type, FieldKind.Select, "CODE128", options = listOf("CODE128" to R.string.f_bc128, "EAN13" to R.string.f_ean13, "UPC" to R.string.f_upc, "CODE39" to R.string.f_code39)),
        Field("val", R.string.f_value, default = "INV-2026-0042")));

    fun defaults(): Map<String, String> = fields.associate { it.key to it.default }

    /** Builds the text that goes inside the code. Empty means "not enough details yet". */
    fun build(f: Map<String, String>): String {
        fun g(k: String) = f[k].orEmpty()
        return when (this) {
            Url -> { val u = g("url").trim(); if (u.isEmpty()) "" else if (Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(u)) u else "https://$u" }
            Text -> g("text")
            Wifi -> if (g("ssid").isEmpty()) "" else "WIFI:T:${g("sec").ifEmpty { "WPA" }};S:${wesc(g("ssid"))};" + (if (g("sec") == "nopass") "" else "P:${wesc(g("pw"))};") + (if (g("hid").isNotEmpty()) "H:true;" else "") + ";"
            Contact -> if (g("name").isEmpty()) "" else "BEGIN:VCARD\nVERSION:3.0\nFN:${g("name")}" + (if (g("org").isNotEmpty()) "\nORG:${g("org")}" else "") + (if (g("tel").isNotEmpty()) "\nTEL:${g("tel")}" else "") + (if (g("email").isNotEmpty()) "\nEMAIL:${g("email")}" else "") + "\nEND:VCARD"
            Sms -> if (g("to").isEmpty()) "" else "SMSTO:${g("to")}:${g("msg")}"
            Email -> if (g("to").isEmpty()) "" else { val q = mutableListOf<String>(); if (g("sub").isNotEmpty()) q += "subject=" + enc(g("sub")); if (g("body").isNotEmpty()) q += "body=" + enc(g("body")); "mailto:${g("to")}" + (if (q.isNotEmpty()) "?" + q.joinToString("&") else "") }
            Phone -> if (g("tel").isEmpty()) "" else "tel:${g("tel")}"
            Event -> if (g("title").isEmpty() || g("start").isEmpty()) "" else "BEGIN:VEVENT\nSUMMARY:${g("title")}\nDTSTART:${dt(g("start"))}" + (if (g("end").isNotEmpty()) "\nDTEND:${dt(g("end"))}" else "") + (if (g("where").isNotEmpty()) "\nLOCATION:${g("where")}" else "") + "\nEND:VEVENT"
            Geo -> { val num = Regex("^-?\\d+(\\.\\d+)?$"); if (num.matches(g("lat")) && num.matches(g("lng"))) "geo:${g("lat")},${g("lng")}" else "" }
            Barcode -> g("val")
        }
    }

    companion object {
        fun of(key: String?): CodeType? = entries.firstOrNull { it.key == key }
        /** RFC 5545 date-time with seconds, e.g. 20261105T190000. */
        fun dt(v: String): String = v.replace("-", "").replace(":", "").take(13) + "00"
        /** Escape the characters that have meaning inside Wi-Fi codes, including the backslash itself. */
        fun wesc(s: String): String = buildString { for (c in s) { if (c == '\\' || c == ';' || c == ',' || c == ':' || c == '"') append('\\'); append(c) } }
        private fun enc(s: String) = URLEncoder.encode(s, "UTF-8").replace("+", "%20")
    }
}

/** What to do with a scanned or saved code. */
sealed interface Primary {
    data class Open(val uri: String, val label: String) : Primary
    data class Copy(val text: String, val label: String, val toast: String) : Primary
}

data class Parsed(val kind: String, val label: String, val icon: String, val hue: Int, val title: String, val rows: List<Pair<String, String>>, val primary: Primary?)

object Parser {
    private val calendarShort = Regex("^(\\d{4})(\\d{2})(\\d{2})(?:T(\\d{2})(\\d{2}))?")

    fun formatDate(ctx: Context, y: Int, m: Int, d: Int): String {
        val month = ctx.getString(ctx.resources.getIdentifier("month_$m", "string", ctx.packageName))
        return "$d $month $y"
    }
    fun formatDate(ctx: Context, ts: Long): String { val c = Calendar.getInstance().apply { timeInMillis = ts }; return formatDate(ctx, c[Calendar.YEAR], c[Calendar.MONTH] + 1, c[Calendar.DAY_OF_MONTH]) }

    private fun icalDate(ctx: Context, s: String): String {
        val m = calendarShort.find(s) ?: return s.ifEmpty { "—" }
        val date = formatDate(ctx, m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toInt())
        return if (m.groupValues[4].isNotEmpty()) "$date, ${m.groupValues[4]}:${m.groupValues[5]}" else date
    }
    private fun vline(raw: String, key: String): String = Regex("^$key(?:;[^:]*)?:(.*)$", setOf(RegexOption.IGNORE_CASE, RegexOption.MULTILINE)).find(raw)?.groupValues?.get(1)?.trim().orEmpty()

    fun parse(ctx: Context, raw0: String): Parsed {
        val r = raw0.trim(); fun s(id: Int): String = ctx.getString(id)
        if (r.startsWith("WIFI:", true)) {
            fun w(k: String): String { val m = Regex("(?:^WIFI:|;)$k:((?:\\\\.|[^;\\\\])*)", RegexOption.IGNORE_CASE).find(r); return m?.groupValues?.get(1)?.replace(Regex("\\\\(.)"), "$1").orEmpty() }
            val pw = w("P")
            return Parsed("wifi", s(R.string.lbl_wifi), "wifi", 188, w("S").ifEmpty { "Wi-Fi" },
                listOf(s(R.string.row_network) to w("S"), s(R.string.row_security) to w("T").ifEmpty { s(R.string.sec_none) }, s(R.string.row_password) to pw.ifEmpty { "—" }),
                if (pw.isNotEmpty()) Primary.Copy(pw, s(R.string.act_copy_password), s(R.string.t_password_copied)) else null)
        }
        if (r.startsWith("BEGIN:VCARD", true)) {
            val tel = vline(r, "TEL"); val em = vline(r, "EMAIL"); val nm = vline(r, "FN").ifEmpty { s(R.string.lbl_contact) }
            val rows = mutableListOf(s(R.string.row_name) to nm)
            if (tel.isNotEmpty()) rows += s(R.string.row_phone) to tel
            if (em.isNotEmpty()) rows += s(R.string.row_email) to em
            vline(r, "ORG").takeIf { it.isNotEmpty() }?.let { rows += s(R.string.row_company) to it }
            return Parsed("contact", s(R.string.lbl_contact), "user", 32, nm, rows,
                if (tel.isNotEmpty()) Primary.Copy(tel, s(R.string.act_copy_number), s(R.string.t_number_copied)) else Primary.Copy(r, s(R.string.act_copy_details), s(R.string.t_copied)))
        }
        if (r.startsWith("BEGIN:VEVENT", true)) {
            val ev = vline(r, "SUMMARY").ifEmpty { s(R.string.lbl_event) }
            val rows = mutableListOf(s(R.string.row_title) to ev, s(R.string.row_starts) to icalDate(ctx, vline(r, "DTSTART")))
            if (vline(r, "DTEND").isNotEmpty()) rows += s(R.string.row_ends) to icalDate(ctx, vline(r, "DTEND"))
            if (vline(r, "LOCATION").isNotEmpty()) rows += s(R.string.row_where) to vline(r, "LOCATION")
            return Parsed("event", s(R.string.lbl_event), "calendar", 280, ev, rows, Primary.Copy(rows.joinToString("\n") { "${it.first}: ${it.second}" }, s(R.string.act_copy_details), s(R.string.t_event_copied)))
        }
        Regex("^SMSTO?:([^:]*):?([\\s\\S]*)$", RegexOption.IGNORE_CASE).find(r)?.let { m ->
            val to = m.groupValues[1]; val body = m.groupValues[2]
            return Parsed("sms", s(R.string.lbl_sms), "sms", 150, to.ifEmpty { s(R.string.lbl_sms) }, listOf(s(R.string.row_to) to to, s(R.string.row_message) to body.ifEmpty { "—" }),
                Primary.Open("sms:$to" + (if (body.isNotEmpty()) "?body=" + java.net.URLEncoder.encode(body, "UTF-8").replace("+", "%20") else ""), s(R.string.act_compose_text)))
        }
        Regex("^mailto:([^?]*)\\??([\\s\\S]*)$", RegexOption.IGNORE_CASE).find(r)?.let { m ->
            val q = m.groupValues[2].split("&").filter { it.contains("=") }.associate { it.substringBefore("=") to java.net.URLDecoder.decode(it.substringAfter("="), "UTF-8") }
            return Parsed("email", s(R.string.lbl_email), "mail", 340, m.groupValues[1], listOf(s(R.string.row_to) to m.groupValues[1], s(R.string.row_subject) to (q["subject"] ?: "—"), s(R.string.row_body) to (q["body"] ?: "—")), Primary.Open(r, s(R.string.act_compose_email)))
        }
        Regex("^tel:(.+)$", RegexOption.IGNORE_CASE).find(r)?.let { m -> return Parsed("phone", s(R.string.lbl_phone), "phone", 120, m.groupValues[1], listOf(s(R.string.row_number) to m.groupValues[1]), Primary.Open(r, s(R.string.act_call))) }
        Regex("^geo:(-?[\\d.]+),(-?[\\d.]+)", RegexOption.IGNORE_CASE).find(r)?.let { m ->
            val lat = m.groupValues[1]; val lng = m.groupValues[2]
            return Parsed("geo", s(R.string.lbl_geo), "pin", 12, "$lat, $lng", listOf(s(R.string.row_latitude) to lat, s(R.string.row_longitude) to lng), Primary.Open("geo:$lat,$lng", s(R.string.act_open_map)))
        }
        if (Regex("^https?://", RegexOption.IGNORE_CASE).containsMatchIn(r)) {
            val host = try { URI(r).host ?: r } catch (e: Exception) { r }
            return Parsed("url", s(R.string.lbl_url), "link", 222, host, listOf(s(R.string.row_site) to host, s(R.string.row_link) to (if (r.length > 48) r.take(46) + "…" else r)), Primary.Open(r, s(R.string.act_open_link)))
        }
        if (Regex("^\\d{8,14}$").matches(r)) return Parsed("product", s(R.string.lbl_product), "barcode", 205, r, listOf(s(R.string.row_number) to r, s(R.string.row_digits) to r.length.toString()), Primary.Open("https://www.google.com/search?q=$r", s(R.string.act_search_web)))
        return Parsed("text", s(R.string.lbl_text), "text", 258, if (r.length > 34) r.take(32) + "…" else r, listOf(s(R.string.row_characters) to r.length.toString()), null)
    }
}
