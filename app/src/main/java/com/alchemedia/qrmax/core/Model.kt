package com.alchemedia.qrmax.core

import androidx.compose.ui.graphics.Color

/** Dot shapes for the QR body. */
enum class Shape(val key: String) { Square("square"), Round("round"), Dots("dots"), Classy("classy"), Diamond("diamond"), Bars("bars"), Heart("heart"), Star("star");
    companion object { fun of(k: String?) = entries.firstOrNull { it.key == k } ?: Square } }
enum class Eye(val key: String) { Square("square"), Round("round"), Circle("circle"), Leaf("leaf");
    companion object { fun of(k: String?) = entries.firstOrNull { it.key == k } ?: Square } }
enum class Ball(val key: String) { Square("square"), Round("round"), Circle("circle");
    companion object { fun of(k: String?) = entries.firstOrNull { it.key == k } ?: Square } }
enum class FrameKind(val key: String) { None("none"), Bottom("bottom"), Top("top"), Tag("pill"), Border("border");
    companion object { fun of(k: String?) = entries.firstOrNull { it.key == k } ?: None } }
enum class LogoKind(val key: String) { None("none"), Image("image"), Icon("icon"), Text("text");
    companion object { fun of(k: String?) = entries.firstOrNull { it.key == k } ?: None } }
enum class Plate(val key: String) { Square("square"), Round("round"), None("none");
    companion object { fun of(k: String?) = entries.firstOrNull { it.key == k } ?: Square } }

data class LogoStyle(
    val kind: LogoKind = LogoKind.None,
    val icon: String = "link",
    val text: String = "QR",
    /** File name inside filesDir/logos, for picture logos. */
    val image: String? = null,
    val size: Int = 20,
    val plate: Plate = Plate.Square,
) {
    val active get() = kind == LogoKind.Icon || kind == LogoKind.Text || (kind == LogoKind.Image && image != null)
}

data class FrameStyle(val kind: FrameKind = FrameKind.None, val text: String = "Scan me", val color: Int? = null)

/** Everything that decides how a code looks. Colours are ARGB ints. */
data class QrStyle(
    val fg: Int = 0xFF0E1626.toInt(),
    val fg2: Int = 0xFF3A5BFF.toInt(),
    val grad: Boolean = false,
    val bg: Int = 0xFFFFFFFF.toInt(),
    val shape: Shape = Shape.Square,
    val round: Float = 0.4f,
    val eye: Eye = Eye.Square,
    val ball: Ball = Ball.Square,
    val eyeSame: Boolean = true,
    val eyeColor: Int = 0xFF3A5BFF.toInt(),
    val ecl: Char = 'M',
    val margin: Int = 2,
    val logo: LogoStyle = LogoStyle(),
    val frame: FrameStyle = FrameStyle(),
    val barText: Boolean = true,
)

data class ScanItem(val id: String, val raw: String, val ts: Long, val example: Boolean = false)

data class SavedItem(
    val id: String,
    val name: String,
    val cat: String,
    val type: String?,
    val vals: Map<String, String>,
    val raw: String,
    val fmt: String,
    val style: QrStyle,
    val ts: Long,
    val example: Boolean = false,
)

fun Int.toColor() = Color(this)

/** WCAG contrast helpers used by the colour meter and scan warnings. */
object Contrast {
    private fun chan(v: Int): Double { val c = v / 255.0; return if (c <= 0.03928) c / 12.92 else Math.pow((c + 0.055) / 1.055, 2.4) }
    fun lum(c: Int): Double = 0.2126 * chan((c shr 16) and 0xFF) + 0.7152 * chan((c shr 8) and 0xFF) + 0.0722 * chan(c and 0xFF)
    fun ratio(a: Int, b: Int): Double { val x = lum(a); val y = lum(b); return (maxOf(x, y) + 0.05) / (minOf(x, y) + 0.05) }
}
