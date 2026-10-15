package com.alchemedia.qrmax.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.qrmax.R
import com.alchemedia.qrmax.core.Ball
import com.alchemedia.qrmax.core.Contrast
import com.alchemedia.qrmax.core.Eye
import com.alchemedia.qrmax.core.FieldKind
import com.alchemedia.qrmax.core.FrameKind
import com.alchemedia.qrmax.core.IconPaths
import com.alchemedia.qrmax.core.Shape
import java.util.Calendar

val q: QrColors @Composable get() = LocalQr.current

@Composable
fun Lbl(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), modifier, color = q.muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.6.sp)
}

@Composable
fun SurfaceCard(modifier: Modifier = Modifier, pad: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(q.surface).border(BorderStroke(1.dp, q.line.copy(alpha = .6f)), RoundedCornerShape(24.dp)).padding(pad), verticalArrangement = Arrangement.spacedBy(14.dp), content = content)
}

@Composable
fun AppButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: String? = null, ghost: Boolean = false, enabled: Boolean = true, compact: Boolean = false, danger: Boolean = false) {
    val bg = if (ghost) q.surface2 else q.accent
    val fg = if (danger) q.danger else if (ghost) q.ink else q.accentInk
    Row(
        modifier.clip(RoundedCornerShape(if (compact) 14.dp else 16.dp)).background(if (enabled) bg else bg.copy(alpha = .4f)).clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = if (compact) 11.dp else 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) Ico(icon, tint = fg, size = if (compact) 18.dp else 20.dp)
        Text(text, color = fg, fontWeight = if (ghost) FontWeight.SemiBold else FontWeight.Bold, fontSize = if (compact) 14.sp else 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
    }
}

@Composable
fun SegRow(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(q.surface2).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        options.forEach { (value, label) ->
            val on = value == selected
            Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(if (on) q.surface else Color.Transparent).clickable(role = Role.RadioButton) { onSelect(value) }
                .semantics { this.selected = on }.padding(vertical = 10.dp, horizontal = 4.dp), contentAlignment = Alignment.Center) {
                Text(label, color = if (on) q.ink else q.muted, fontWeight = FontWeight.Bold, fontSize = 13.5.sp, textAlign = TextAlign.Center, maxLines = 2)
            }
        }
    }
}

@Composable
fun SwitchRow(label: String, hint: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Switch) { onChange(!checked) }, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, color = q.ink, fontWeight = FontWeight.SemiBold, fontSize = 14.5.sp)
            if (!hint.isNullOrEmpty()) Text(hint, color = q.muted, fontSize = 12.5.sp)
        }
        Switch(checked, onChange, colors = SwitchDefaults.colors(checkedTrackColor = q.accent, checkedThumbColor = Color.White, uncheckedTrackColor = q.line, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent))
    }
}

@Composable
fun TintIcon(icon: String, hue: Int, size: Dp = 40.dp, iconSize: Dp = 22.dp) {
    val tint = hueColor(hue, q.dark)
    Box(Modifier.size(size).clip(RoundedCornerShape(size * .35f)).background(tint.copy(alpha = .15f)), contentAlignment = Alignment.Center) { Ico(icon, tint = tint, size = iconSize) }
}

@Composable
fun QrTextField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, kind: FieldKind = FieldKind.Text, placeholder: String = "", singleLine: Boolean = kind != FieldKind.Area, maxLength: Int = Int.MAX_VALUE, label: String? = null, trailing: (@Composable () -> Unit)? = null) {
    val kb = when (kind) { FieldKind.Url -> KeyboardType.Uri; FieldKind.Tel -> KeyboardType.Phone; FieldKind.Email -> KeyboardType.Email; else -> KeyboardType.Text }
    TextField(
        value, { if (it.length <= maxLength) onChange(it) }, modifier.fillMaxWidth().then(if (label != null) Modifier.semantics { contentDescription = label } else Modifier),
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder, color = q.muted.copy(alpha = .7f)) }, singleLine = singleLine, minLines = if (singleLine) 1 else 3,
        keyboardOptions = KeyboardOptions(keyboardType = kb), shape = RoundedCornerShape(14.dp), trailingIcon = trailing,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = q.surface2, unfocusedContainerColor = q.surface2, focusedTextColor = q.ink, unfocusedTextColor = q.ink, cursorColor = q.accent,
            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent, disabledIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
fun LabeledField(label: String, content: @Composable () -> Unit) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Lbl(label); content() } }

/** Date and time without free typing: a date picker, then a time picker. Value is "yyyy-MM-dd'T'HH:mm". */
@Composable
fun DateTimeField(value: String, onChange: (String) -> Unit) {
    val ctx = LocalContext.current
    val res = LocalResources.current
    val cfg = LocalConfiguration.current
    val shown = remember(value, cfg) { if (value.length >= 16) value.substring(8, 10).trimStart('0') + " " + (res.getString(res.getIdentifier("month_" + value.substring(5, 7).toInt(), "string", ctx.packageName))) + " " + value.substring(0, 4) + ", " + value.substring(11, 16) else "—" }
    Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(q.surface2).clickable {
        val c = Calendar.getInstance()
        if (value.length >= 16) c.set(value.substring(0, 4).toInt(), value.substring(5, 7).toInt() - 1, value.substring(8, 10).toInt(), value.substring(11, 13).toInt(), value.substring(14, 16).toInt())
        DatePickerDialog(ctx, { _, y, m, d ->
            TimePickerDialog(ctx, { _, h, min -> onChange("%04d-%02d-%02dT%02d:%02d".format(y, m + 1, d, h, min)) }, c[Calendar.HOUR_OF_DAY], c[Calendar.MINUTE], true).show()
        }, c[Calendar.YEAR], c[Calendar.MONTH], c[Calendar.DAY_OF_MONTH]).show()
    }.padding(horizontal = 16.dp, vertical = 16.dp)) { Text(shown, color = q.ink, fontSize = 16.sp) }
}

/* ---------------- colour ---------------- */

val FgSwatches = listOf(0xFF0E1626, 0xFF2F4BFF, 0xFF0B6E4F, 0xFF8A1F5C, 0xFFC2410C, 0xFF6D28D9).map { it.toInt() }
val BgSwatches = listOf(0xFFFFFFFF, 0xFFF6ECD9, 0xFFE0F2FE, 0xFFD1FAE5, 0xFFFDE2E4, 0xFF111827).map { it.toInt() }

fun hexOf(c: Int) = "#%06X".format(c and 0xFFFFFF)
fun parseHex(s: String): Int? { val t = s.trim().removePrefix("#"); return if (t.length == 6 && t.all { it.isDigit() || it.lowercaseChar() in 'a'..'f' }) (0xFF000000.toInt() or t.toInt(16)) else null }

@Composable
fun ColorRow(label: String, color: Int, swatches: List<Int>, onChange: (Int) -> Unit, extra: (@Composable () -> Unit)? = null, onBadHex: () -> Unit = {}) {
    var picker by remember { mutableStateOf(false) }
    var hex by remember(color) { mutableStateOf(hexOf(color)) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Lbl(label)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp), itemVerticalAlignment = Alignment.CenterVertically) {
            swatches.forEach { c ->
                val on = c == color
                Box(Modifier.size(32.dp).clip(CircleShape).background(Color(c)).border(if (on) 2.5.dp else 1.dp, if (on) q.accent else q.line, CircleShape).clickable(role = Role.RadioButton) { onChange(c) }.semantics { selected = on; contentDescription = hexOf(c) })
            }
            Box(Modifier.size(34.dp).clip(CircleShape).background(Brush.sweepGradient(listOf(Color(0xFFFF3B30), Color(0xFFFFCC00), Color(0xFF34C759), Color(0xFF00C7BE), Color(0xFF3A5BFF), Color(0xFFAF52DE), Color(0xFFFF3B30)))).clickable { picker = true }.semantics { contentDescription = label })
            Box(Modifier.width(100.dp)) {
                QrTextField(hex, { v -> hex = v.uppercase(); parseHex(v)?.let(onChange) }, maxLength = 7, label = "$label hex")
            }
            extra?.invoke()
        }
    }
    if (picker) ColorPickerDialog(color, { picker = false }) { onChange(it); picker = false }
}

@Composable
fun ColorPickerDialog(initial: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val hsv = remember { FloatArray(3).also { android.graphics.Color.colorToHSV(initial, it) } }
    var h by remember { mutableStateOf(hsv[0]) }; var s by remember { mutableStateOf(hsv[1]) }; var v by remember { mutableStateOf(hsv[2]) }
    val current = Color.hsv(h, s, v)
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = q.surface,
        confirmButton = { TextButton({ onPick(0xFF000000.toInt() or (current.toArgb() and 0xFFFFFF)) }) { Text(stringResource(R.string.save), color = q.accent, fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onDismiss) { Text(stringResource(R.string.close), color = q.muted) } },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(14.dp)).background(current), contentAlignment = Alignment.Center) { Text(hexOf(current.toArgb()), color = if (Contrast.lum(current.toArgb()) > .4) Color.Black else Color.White, fontWeight = FontWeight.Bold) }
                Slider(h, { h = it }, valueRange = 0f..360f, colors = SliderDefaults.colors(thumbColor = q.accent, activeTrackColor = Color.Transparent, inactiveTrackColor = Color.Transparent), modifier = Modifier.background(Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f, 1f, 1f) }), RoundedCornerShape(50)))
                Slider(s, { s = it }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = q.accent, activeTrackColor = q.accent))
                Slider(v, { v = it }, valueRange = 0f..1f, colors = SliderDefaults.colors(thumbColor = q.accent, activeTrackColor = q.accent))
            }
        },
    )
}

/* ---------------- option grids with drawn glyphs ---------------- */

@Composable
fun OptionTile(selected: Boolean, label: String, onClick: () -> Unit, modifier: Modifier = Modifier, glyph: @Composable BoxScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(14.dp)).background(if (selected) q.accentSoft else q.surface2).border(1.5.dp, if (selected) q.accent else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(role = Role.RadioButton, onClick = onClick).semantics { this.selected = selected }.padding(vertical = 9.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(34.dp), content = glyph)
        Text(label, color = if (selected) q.ink else q.muted, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@Composable
fun <T> OptionGrid(columns: Int, items: List<Triple<T, String, @Composable BoxScope.(Color) -> Unit>>, selected: T, onSelect: (T) -> Unit) {
    items.chunked(columns).forEach { row ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            row.forEach { (value, label, glyph) ->
                val on = value == selected
                OptionTile(on, label, { onSelect(value) }, Modifier.weight(1f)) { glyph(if (on) q.ink else q.muted) }
            }
            repeat(columns - row.size) { Box(Modifier.weight(1f)) }
        }
        Box(Modifier.height(8.dp))
    }
}

private fun DrawScope.u() = size.minDimension / 34f

@Composable fun ShapeGlyph(shape: Shape, color: Color) {
    Canvas(Modifier.size(34.dp)) {
        val k = u(); val pts = listOf(6f to 6f, 19f to 6f, 6f to 19f, 19f to 19f)
        if (shape == Shape.Bars) { drawRoundRect(color, Offset(6 * k, 6 * k), Size(22 * k, 9 * k), CornerRadius(4.5f * k)); drawRoundRect(color, Offset(6 * k, 19 * k), Size(22 * k, 9 * k), CornerRadius(4.5f * k)); return@Canvas }
        pts.forEach { (x, y) ->
            val o = Offset(x * k, y * k); val sz = Size(9 * k, 9 * k)
            when (shape) {
                Shape.Square -> drawRect(color, o, sz)
                Shape.Round -> drawRoundRect(color, o, sz, CornerRadius(3 * k))
                Shape.Dots -> drawCircle(color, 4.5f * k, Offset(o.x + 4.5f * k, o.y + 4.5f * k))
                Shape.Classy -> drawPath(Path().apply { addRoundRect(RoundRect(Rect(o, sz), CornerRadius(4.5f * k), CornerRadius.Zero, CornerRadius(4.5f * k), CornerRadius.Zero)) }, color)
                Shape.Diamond -> drawPath(Path().apply { moveTo(o.x + 4.5f * k, o.y - .5f * k); lineTo(o.x + 9.5f * k, o.y + 4.5f * k); lineTo(o.x + 4.5f * k, o.y + 9.5f * k); lineTo(o.x - .5f * k, o.y + 4.5f * k); close() }, color)
                Shape.Heart -> { val p = PathParser().parsePathString(IconPaths.map.getValue("heart")).toPath(); val m = Matrix(); m.translate(o.x + 4.5f * k, o.y + 4.8f * k); m.scale(.55f * k, .55f * k); m.translate(-12f, -13f); p.transform(m); drawPath(p, color) }
                Shape.Star -> drawPath(Path().apply { for (i in 0 until 10) { val a = -Math.PI / 2 + i * Math.PI / 5; val r = (if (i % 2 == 1) 2.4f else 5.4f) * k; val px = (o.x + 4.5f * k + Math.cos(a) * r).toFloat(); val py = (o.y + 4.7f * k + Math.sin(a) * r).toFloat(); if (i == 0) moveTo(px, py) else lineTo(px, py) }; close() }, color)
                Shape.Bars -> {}
            }
        }
    }
}

@Composable fun EyeGlyph(eye: Eye, color: Color) {
    Canvas(Modifier.size(34.dp)) {
        val k = u(); val ring = Path().apply { fillType = PathFillType.EvenOdd }
        val outer = Rect(4 * k, 4 * k, 30 * k, 30 * k); val inner = Rect(9 * k, 9 * k, 25 * k, 25 * k)
        when (eye) {
            Eye.Square -> { ring.addRect(outer); ring.addRect(inner) }
            Eye.Round -> { ring.addRoundRect(RoundRect(outer, CornerRadius(9 * k))); ring.addRoundRect(RoundRect(inner, CornerRadius(4 * k))) }
            Eye.Circle -> { ring.addOval(outer); ring.addOval(inner) }
            Eye.Leaf -> { ring.addRoundRect(RoundRect(outer, CornerRadius(12 * k), CornerRadius(2 * k), CornerRadius(12 * k), CornerRadius(2 * k))); ring.addRoundRect(RoundRect(inner, CornerRadius(6 * k), CornerRadius(.5f * k), CornerRadius(6 * k), CornerRadius(.5f * k))) }
        }
        drawPath(ring, color); drawRoundRect(color, Offset(12 * k, 12 * k), Size(10 * k, 10 * k), CornerRadius(if (eye == Eye.Square) 0f else 3 * k))
    }
}

@Composable fun BallGlyph(ball: Ball, color: Color) {
    Canvas(Modifier.size(34.dp)) { val k = u(); when (ball) { Ball.Square -> drawRect(color, Offset(9 * k, 9 * k), Size(16 * k, 16 * k)); Ball.Round -> drawRoundRect(color, Offset(9 * k, 9 * k), Size(16 * k, 16 * k), CornerRadius(5 * k)); Ball.Circle -> drawCircle(color, 8.5f * k, center) } }
}

@Composable fun FrameGlyph(kind: FrameKind, color: Color) {
    Canvas(Modifier.size(34.dp)) {
        val k = u()
        if (kind == FrameKind.None) { drawRoundRect(color, Offset(7 * k, 7 * k), Size(20 * k, 20 * k), CornerRadius(3 * k), style = Stroke(2 * k, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(3 * k, 3 * k)))); return@Canvas }
        val ring = Path().apply { fillType = PathFillType.EvenOdd }
        val outer = Rect(5 * k, 4 * k, 29 * k, if (kind == FrameKind.Tag) 26 * k else 30 * k)
        ring.addRoundRect(RoundRect(outer, CornerRadius(4 * k)))
        when (kind) { FrameKind.Bottom -> ring.addRect(Rect(9 * k, 8 * k, 25 * k, 22 * k)); FrameKind.Top -> ring.addRect(Rect(9 * k, 12 * k, 25 * k, 26 * k)); FrameKind.Tag -> ring.addRoundRect(RoundRect(Rect(7.5f * k, 6.5f * k, 26.5f * k, 23.5f * k), CornerRadius(1.5f * k))); else -> ring.addRect(Rect(9 * k, 8 * k, 25 * k, 26 * k)) }
        drawPath(ring, color)
        if (kind == FrameKind.Tag) drawRoundRect(color, Offset(9 * k, 23.5f * k), Size(16 * k, 6 * k), CornerRadius(3 * k))
    }
}
