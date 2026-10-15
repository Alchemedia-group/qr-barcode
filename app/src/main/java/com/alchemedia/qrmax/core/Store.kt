package com.alchemedia.qrmax.core

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Keeps history and saved codes on the device as a small JSON file. Nothing leaves the phone. */
class Store(private val ctx: Context) {
    private val file = File(ctx.filesDir, "qrmax.json")
    val scans = mutableListOf<ScanItem>()
    val saved = mutableListOf<SavedItem>()

    init { load() }

    fun load() {
        scans.clear(); saved.clear()
        try {
            if (file.exists()) {
                val o = JSONObject(file.readText())
                val s = o.getJSONArray("scans"); for (i in 0 until s.length()) s.getJSONObject(i).let { scans += ScanItem(it.getString("id"), it.getString("raw"), it.getLong("ts"), it.optBoolean("example")) }
                val v = o.getJSONArray("saved"); for (i in 0 until v.length()) saved += savedFromJson(v.getJSONObject(i))
                return
            }
        } catch (e: Exception) { scans.clear(); saved.clear() }
        seed()
    }

    private fun seed() {
        val now = System.currentTimeMillis(); val h = 3_600_000L
        scans += ScanItem("s1", "https://example.com/spring-menu", now - h * 2, true)
        scans += ScanItem("s2", "WIFI:T:WPA;S:Guest-Lounge;P:welcome-2026;;", now - h * 26, true)
        scans += ScanItem("s3", "5901234123457", now - h * 50, true)
        val wifi = CodeType.Wifi.defaults() + mapOf("hid" to "")
        saved += SavedItem("c1", "Guest Wi-Fi", "Business", "wifi", wifi, CodeType.Wifi.build(wifi), "qr",
            QrStyle(fg = 0xFF0B3D91.toInt(), fg2 = 0xFF0E7490.toInt(), grad = true, shape = Shape.Round, eye = Eye.Round, ball = Ball.Round, logo = LogoStyle(kind = LogoKind.Icon, icon = "wifi")), now - h * 5, true)
        val contact = CodeType.Contact.defaults()
        saved += SavedItem("c2", "My contact card", "Personal", "contact", contact, CodeType.Contact.build(contact), "qr",
            QrStyle(fg = 0xFF701A75.toInt(), fg2 = 0xFFBE185D.toInt(), grad = true, shape = Shape.Classy, eye = Eye.Leaf, ball = Ball.Round, frame = FrameStyle(FrameKind.Bottom, "Say hello")), now - h * 70, true)
        save()
    }

    fun save() {
        try {
            val o = JSONObject()
            o.put("scans", JSONArray().apply { scans.forEach { put(JSONObject().put("id", it.id).put("raw", it.raw).put("ts", it.ts).put("example", it.example)) } })
            o.put("saved", JSONArray().apply { saved.forEach { put(savedToJson(it)) } })
            file.writeText(o.toString())
        } catch (_: Exception) { /* storage full or blocked: the app keeps working for this session */ }
    }

    fun recordScan(raw: String) {
        scans.removeAll { it.raw == raw && !it.example }
        scans.add(0, ScanItem("s" + System.currentTimeMillis(), raw, System.currentTimeMillis()))
        while (scans.size > 200) scans.removeAt(scans.size - 1)
        save()
    }

    companion object {
        fun styleToJson(s: QrStyle) = JSONObject().apply {
            put("fg", s.fg); put("fg2", s.fg2); put("grad", s.grad); put("bg", s.bg); put("shape", s.shape.key); put("round", s.round.toDouble())
            put("eye", s.eye.key); put("ball", s.ball.key); put("eyeSame", s.eyeSame); put("eyeColor", s.eyeColor); put("ecl", s.ecl.toString()); put("margin", s.margin); put("barText", s.barText)
            put("logo", JSONObject().put("kind", s.logo.kind.key).put("icon", s.logo.icon).put("text", s.logo.text).put("image", s.logo.image ?: JSONObject.NULL).put("size", s.logo.size).put("plate", s.logo.plate.key))
            put("frame", JSONObject().put("kind", s.frame.kind.key).put("text", s.frame.text).put("color", s.frame.color ?: JSONObject.NULL))
        }
        fun styleFromJson(o: JSONObject): QrStyle {
            val d = QrStyle(); val l = o.optJSONObject("logo"); val f = o.optJSONObject("frame")
            return QrStyle(
                fg = o.optInt("fg", d.fg), fg2 = o.optInt("fg2", d.fg2), grad = o.optBoolean("grad"), bg = o.optInt("bg", d.bg), shape = Shape.of(o.optString("shape")),
                round = o.optDouble("round", 0.4).toFloat(), eye = Eye.of(o.optString("eye")), ball = Ball.of(o.optString("ball")), eyeSame = o.optBoolean("eyeSame", true), eyeColor = o.optInt("eyeColor", d.eyeColor),
                ecl = o.optString("ecl", "M").firstOrNull() ?: 'M', margin = o.optInt("margin", 2), barText = o.optBoolean("barText", true),
                logo = if (l == null) LogoStyle() else LogoStyle(LogoKind.of(l.optString("kind")), l.optString("icon", "link"), l.optString("text", "QR"), if (l.isNull("image")) null else l.optString("image"), l.optInt("size", 20), Plate.of(l.optString("plate"))),
                frame = if (f == null) FrameStyle() else FrameStyle(FrameKind.of(f.optString("kind")), f.optString("text", "Scan me"), if (f.isNull("color")) null else f.optInt("color")),
            )
        }
        private fun savedToJson(s: SavedItem) = JSONObject().put("id", s.id).put("name", s.name).put("cat", s.cat).put("type", s.type ?: JSONObject.NULL).put("raw", s.raw).put("fmt", s.fmt).put("ts", s.ts)
            .put("example", s.example).put("vals", JSONObject(s.vals as Map<*, *>)).put("style", styleToJson(s.style))
        private fun savedFromJson(o: JSONObject): SavedItem {
            val v = o.optJSONObject("vals"); val vals = mutableMapOf<String, String>(); v?.keys()?.forEach { vals[it] = v.optString(it) }
            return SavedItem(o.getString("id"), o.getString("name"), o.optString("cat", "Personal"), if (o.isNull("type")) null else o.optString("type"), vals, o.getString("raw"), o.optString("fmt", "qr"),
                o.optJSONObject("style")?.let { styleFromJson(it) } ?: QrStyle(), o.getLong("ts"), o.optBoolean("example"))
        }
    }
}
