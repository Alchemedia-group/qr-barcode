package com.alchemedia.qrmax.ui

import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.qrmax.AppViewModel
import com.alchemedia.qrmax.CTab
import com.alchemedia.qrmax.R
import com.alchemedia.qrmax.SheetKind
import com.alchemedia.qrmax.core.*
import kotlinx.coroutines.delay

private class Preset(val name: Int, val fg: Long, val fg2: Long, val grad: Boolean, val bg: Long, val shape: Shape, val eye: Eye, val ball: Ball)
private val Presets = listOf(
    Preset(R.string.pre_classic, 0xFF0E1626, 0xFF3A5BFF, false, 0xFFFFFFFF, Shape.Square, Eye.Square, Ball.Square),
    Preset(R.string.pre_ocean, 0xFF0B3D91, 0xFF0E7490, true, 0xFFFFFFFF, Shape.Round, Eye.Round, Ball.Round),
    Preset(R.string.pre_forest, 0xFF0B6E4F, 0xFF3A5BFF, false, 0xFFFFFFFF, Shape.Round, Eye.Round, Ball.Circle),
    Preset(R.string.pre_berry, 0xFF701A75, 0xFFBE185D, true, 0xFFFFFFFF, Shape.Classy, Eye.Leaf, Ball.Round),
    Preset(R.string.pre_sunset, 0xFF9A3412, 0xFF9F1239, true, 0xFFFFFFFF, Shape.Bars, Eye.Round, Ball.Round),
    Preset(R.string.pre_mint, 0xFF064E3B, 0xFF3A5BFF, false, 0xFFD1FAE5, Shape.Dots, Eye.Circle, Ball.Circle),
    Preset(R.string.pre_sand, 0xFF3B2A1A, 0xFF3A5BFF, false, 0xFFF6ECD9, Shape.Classy, Eye.Square, Ball.Square),
)
private fun Preset.on(s: QrStyle) = s.fg == fg.toInt() && s.bg == bg.toInt() && s.grad == grad && (!grad || s.fg2 == fg2.toInt()) && s.shape == shape

@Composable
fun CreateScreen(vm: AppViewModel) {
    val isPro by vm.isPro.collectAsState()
    val tab = if (!isPro && (vm.cTab == CTab.Color || vm.cTab == CTab.Logo)) CTab.Content else vm.cTab
    Column(Modifier.fillMaxSize().background(q.bg).imePadding()) {
        Column(Modifier.windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 16.dp).padding(top = 10.dp, bottom = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PreviewBand(vm)
            TabsRow(vm, tab, isPro)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(top = 6.dp, bottom = 120.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            when (tab) {
                CTab.Content -> ContentTab(vm)
                CTab.Color -> ColorTab(vm)
                CTab.Shape -> ShapeTab(vm)
                CTab.Logo -> LogoTab(vm)
                CTab.Frame -> FrameTab(vm, isPro)
            }
        }
    }
}

@Composable
private fun PreviewBand(vm: AppViewModel) {
    val ctx = LocalContext.current
    val raw = vm.payload(); val fmt = vm.fmt(); val st = vm.cStyle
    val out by produceState<QrRenderer.Out?>(null, raw, fmt, st) { if (raw.isEmpty()) value = null else { delay(80); value = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) { QrRenderer.render(ctx, raw, fmt, st, 600, verify = true) } } }
    val tint = hueColor(vm.cType.hue, q.dark)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(tint.copy(alpha = .10f).compositeOver(q.surface), q.surface))).border(1.dp, q.line.copy(alpha = .6f), RoundedCornerShape(24.dp)).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        CodeBox(out?.bitmap, Modifier.size(128.dp), if (raw.isEmpty()) stringResource(R.string.pv_fill) else out?.error?.let { stringResource(R.string.pv_none) })
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                Ico(vm.cType.icon, tint = tint, size = 16.dp)
                Text(stringResource(R.string.pv_label, stringResource(vm.cType.label)), color = tint, fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            val o = out
            if (o != null && o.error == null && fmt == "qr") {
                val ok = o.verified != false
                Row(Modifier.clip(RoundedCornerShape(12.dp)).background((if (ok) q.ok else Color(0xFFF59E0B)).copy(alpha = .16f)).padding(horizontal = 10.dp, vertical = 5.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (ok) Ico("check", tint = q.ok, size = 14.dp)
                    Text(if (ok) (o.note ?: stringResource(R.string.ok_tested)) else if (st.logo.active) stringResource(R.string.warn_logo) else stringResource(R.string.warn_style), color = if (ok) q.ok else if (q.dark) Color(0xFFFBBF24) else Color(0xFFB45309), fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 15.sp)
                }
            }
            Text(o?.error ?: o?.info ?: "", color = if (o?.error != null) q.danger else q.muted, fontSize = 12.sp, maxLines = 2)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconActionButton("share", stringResource(R.string.share), { if (raw.isEmpty() || o?.bitmap == null) vm.showToast(R.string.fill_first) else vm.sheet = SheetKind.Share(raw, fmt, st, vm.cName.ifEmpty { "QR Max" }, null) }, Modifier.weight(1f))
                IconActionButton("bookmark", stringResource(R.string.save), { if (raw.isEmpty() || o?.bitmap == null) vm.showToast(R.string.fill_first) else vm.sheet = SheetKind.Save }, Modifier.weight(1f), ghost = true)
            }
        }
    }
}

@Composable
private fun TabsRow(vm: AppViewModel, current: CTab, isPro: Boolean) {
    val isBar = vm.cType == CodeType.Barcode
    val tabs = listOf(Triple(CTab.Content, R.string.ct_content, "edit"), Triple(CTab.Color, R.string.ct_color, "palette"), Triple(CTab.Shape, R.string.ct_shape, "shapes"), Triple(CTab.Logo, R.string.ct_logo, "image"), Triple(CTab.Frame, R.string.ct_frame, "frame"))
        .filter { !(isBar && (it.first == CTab.Shape || it.first == CTab.Logo || it.first == CTab.Frame)) }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(q.surface).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        tabs.forEach { (t, label, icon) ->
            val on = t == current; val locked = !isPro && (t == CTab.Color || t == CTab.Logo)
            Box(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).background(if (on) q.accentSoft else Color.Transparent).clickable(role = Role.Tab) { if (locked) vm.gate(t) else vm.cTab = t }.padding(vertical = 7.dp)) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Ico(icon, tint = if (on) q.accent else q.muted, size = 20.dp)
                    Text(stringResource(label), color = if (on) q.accent else q.muted, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (locked) Box(Modifier.align(Alignment.TopEnd).padding(top = 0.dp, end = 4.dp).size(15.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Gold1, Gold2))), contentAlignment = Alignment.Center) { Ico("lock", tint = GoldInk, size = 9.dp) }
            }
        }
    }
}

/* ---------------- content ---------------- */

@Composable
private fun ContentTab(vm: AppViewModel) {
    val ctx = LocalContext.current
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(vertical = 2.dp)) {
        items(CodeType.entries) { t ->
            val on = t == vm.cType
            Column(Modifier.width(76.dp).clip(RoundedCornerShape(18.dp)).background(q.surface).border(1.5.dp, if (on) q.accent else Color.Transparent, RoundedCornerShape(18.dp)).clickable(role = Role.RadioButton) { vm.selectType(t) }.padding(vertical = 10.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                TintIcon(t.icon, t.hue, 38.dp, 21.dp)
                Text(stringResource(t.label), color = if (on) q.ink else q.muted, fontWeight = FontWeight.SemiBold, fontSize = 11.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
    SurfaceCard {
        vm.cType.fields.forEach { f ->
            val v = vm.cVals[f.key].orEmpty()
            fun set(x: String) { vm.cVals = vm.cVals + (f.key to x) }
            when (f.kind) {
                FieldKind.Switch -> SwitchRow(stringResource(f.label), null, v.isNotEmpty()) { set(if (it) "1" else "") }
                FieldKind.Select -> LabeledField(stringResource(f.label)) { SelectField(f.options.map { it.first to stringResource(it.second) }, v) { set(it) } }
                FieldKind.DateTime -> LabeledField(stringResource(f.label)) { DateTimeField(v) { set(it) } }
                else -> LabeledField(stringResource(f.label)) {
                    QrTextField(v, { set(it) }, kind = f.kind, placeholder = f.placeholderRes?.let { stringResource(it) } ?: f.placeholder,
                        trailing = if (f.canPaste) ({ Text(stringResource(R.string.paste), Modifier.clip(RoundedCornerShape(12.dp)).clickable { val cm = ctx.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager; cm.primaryClip?.getItemAt(0)?.coerceToText(ctx)?.toString()?.let { set(it) } }.padding(horizontal = 12.dp, vertical = 8.dp), color = q.accent, fontWeight = FontWeight.Bold) }) else null)
                }
            }
        }
    }
}

@Composable
private fun SelectField(options: List<Pair<String, String>>, value: String, onChange: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(q.surface2).clickable { open = true }.padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(options.firstOrNull { it.first == value }?.second ?: "", color = q.ink, fontSize = 16.sp, modifier = Modifier.weight(1f)); Ico("chevron", tint = q.muted)
        }
        DropdownMenu(open, { open = false }, containerColor = q.surface) { options.forEach { (k, label) -> DropdownMenuItem({ Text(label, color = q.ink) }, { onChange(k); open = false }) } }
    }
}

/* ---------------- colour ---------------- */

@Composable
private fun ColorTab(vm: AppViewModel) {
    val st = vm.cStyle; val isBar = vm.cType == CodeType.Barcode
    fun upd(s: QrStyle) { vm.cStyle = s }
    if (!isBar) {
        Lbl(stringResource(R.string.c_themes))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(Presets) { p ->
                val on = p.on(st)
                Column(Modifier.width(62.dp).clickable(role = Role.RadioButton) { upd(st.copy(fg = p.fg.toInt(), fg2 = p.fg2.toInt(), grad = p.grad, bg = p.bg.toInt(), shape = p.shape, eye = p.eye, ball = p.ball, eyeSame = true)) }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)).background(Color(p.bg.toInt())).border(if (on) 2.5.dp else 1.dp, if (on) q.accent else q.line, RoundedCornerShape(17.dp)), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(if (p.grad) Brush.linearGradient(listOf(Color(p.fg.toInt()), Color(p.fg2.toInt()))) else Brush.linearGradient(listOf(Color(p.fg.toInt()), Color(p.fg.toInt())))))
                    }
                    Text(stringResource(p.name), color = if (on) q.ink else q.muted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }
        }
    }
    SurfaceCard {
        val badHex = { vm.showToast(R.string.c_hex_err) }
        ColorRow(stringResource(if (isBar) R.string.c_bar else R.string.c_code), st.fg, FgSwatches, { upd(st.copy(fg = it)) }, onBadHex = badHex)
        if (!isBar) {
            SwitchRow(stringResource(R.string.c_blend), stringResource(R.string.c_blend_hint), st.grad) { upd(st.copy(grad = it)) }
            if (st.grad) ColorRow(stringResource(R.string.c_second), st.fg2, FgSwatches, { upd(st.copy(fg2 = it)) }, onBadHex = badHex)
        }
        ColorRow(stringResource(R.string.c_bg), st.bg, BgSwatches, { upd(st.copy(bg = it)) }, onBadHex = badHex)
        if (!isBar) {
            SwitchRow(stringResource(R.string.c_match), stringResource(R.string.c_match_hint), st.eyeSame) { upd(st.copy(eyeSame = it)) }
            if (!st.eyeSame) ColorRow(stringResource(R.string.c_corner), st.eyeColor, FgSwatches, { upd(st.copy(eyeColor = it)) }, onBadHex = badHex)
        } else SwitchRow(stringResource(R.string.c_show_number), null, st.barText) { upd(st.copy(barText = it)) }
        ContrastMeter(st, isBar)
    }
}

@Composable
private fun ContrastMeter(st: QrStyle, isBar: Boolean) {
    val cols = listOf(st.fg) + if (st.grad && !isBar) listOf(st.fg2) else emptyList()
    val r = cols.minOf { Contrast.ratio(it, st.bg) }; val inv = Contrast.lum(st.fg) > Contrast.lum(st.bg)
    val ok = r >= 4.5 && !inv
    val msg = if (inv && r >= 3) R.string.ct_inverted else if (r >= 7 && !inv) R.string.ct_excellent else if (ok) R.string.ct_good else if (r >= 3) R.string.ct_low else R.string.ct_bad
    val dot = if (ok) q.ok else if (r >= 3) Color(0xFFF59E0B) else q.danger
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(q.surface2).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).clip(CircleShape).background(dot))
        Text("%.1f:1 · %s".format(r, stringResource(msg)), color = q.ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

/* ---------------- shape ---------------- */

@Composable
private fun ShapeTab(vm: AppViewModel) {
    val st = vm.cStyle
    SurfaceCard {
        Lbl(stringResource(R.string.s_dots))
        OptionGrid(4, listOf(
            Triple(Shape.Square, stringResource(R.string.sh_square)) { c: Color -> ShapeGlyph(Shape.Square, c) }, Triple(Shape.Round, stringResource(R.string.sh_round)) { c: Color -> ShapeGlyph(Shape.Round, c) },
            Triple(Shape.Dots, stringResource(R.string.sh_dots)) { c: Color -> ShapeGlyph(Shape.Dots, c) }, Triple(Shape.Classy, stringResource(R.string.sh_classy)) { c: Color -> ShapeGlyph(Shape.Classy, c) },
            Triple(Shape.Diamond, stringResource(R.string.sh_diamond)) { c: Color -> ShapeGlyph(Shape.Diamond, c) }, Triple(Shape.Bars, stringResource(R.string.sh_bars)) { c: Color -> ShapeGlyph(Shape.Bars, c) },
            Triple(Shape.Heart, stringResource(R.string.sh_hearts)) { c: Color -> ShapeGlyph(Shape.Heart, c) }, Triple(Shape.Star, stringResource(R.string.sh_stars)) { c: Color -> ShapeGlyph(Shape.Star, c) },
        ), st.shape) { vm.cStyle = st.copy(shape = it) }
        if (st.shape == Shape.Round) SliderRow(stringResource(R.string.s_roundness), "${(st.round * 200).toInt()}%", st.round * 100f, 0f..50f) { vm.cStyle = st.copy(round = it / 100f) }
        if (st.shape == Shape.Heart || st.shape == Shape.Star) Text(stringResource(R.string.s_fun_hint), color = q.muted, fontSize = 12.5.sp)
        Lbl(stringResource(R.string.s_corner_frames))
        OptionGrid(4, listOf(
            Triple(Eye.Square, stringResource(R.string.eye_square)) { c: Color -> EyeGlyph(Eye.Square, c) }, Triple(Eye.Round, stringResource(R.string.eye_round)) { c: Color -> EyeGlyph(Eye.Round, c) },
            Triple(Eye.Circle, stringResource(R.string.eye_circle)) { c: Color -> EyeGlyph(Eye.Circle, c) }, Triple(Eye.Leaf, stringResource(R.string.eye_leaf)) { c: Color -> EyeGlyph(Eye.Leaf, c) },
        ), st.eye) { vm.cStyle = st.copy(eye = it) }
        Lbl(stringResource(R.string.s_corner_dots))
        OptionGrid(3, listOf(
            Triple(Ball.Square, stringResource(R.string.eye_square)) { c: Color -> BallGlyph(Ball.Square, c) }, Triple(Ball.Round, stringResource(R.string.eye_round)) { c: Color -> BallGlyph(Ball.Round, c) }, Triple(Ball.Circle, stringResource(R.string.eye_circle)) { c: Color -> BallGlyph(Ball.Circle, c) },
        ), st.ball) { vm.cStyle = st.copy(ball = it) }
    }
    SurfaceCard {
        Lbl(stringResource(R.string.s_damage))
        SegRow(listOf("L" to stringResource(R.string.ecl_l), "M" to stringResource(R.string.ecl_m), "Q" to stringResource(R.string.ecl_q), "H" to stringResource(R.string.ecl_h)), if (st.logo.active) "H" else st.ecl.toString(), { vm.cStyle = st.copy(ecl = it[0]) })
        Text(stringResource(if (st.logo.active) R.string.s_ecl_auto else R.string.s_ecl_hint), color = q.muted, fontSize = 12.5.sp)
        SliderRow(stringResource(R.string.s_border), st.margin.toString(), st.margin.toFloat(), 1f..4f, steps = 2) { vm.cStyle = st.copy(margin = it.toInt()) }
    }
}

@Composable
private fun SliderRow(label: String, valueText: String, value: Float, range: ClosedFloatingPointRange<Float>, steps: Int = 0, onChange: (Float) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Lbl(label); Text(valueText, color = q.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
        Slider(value, onChange, valueRange = range, steps = steps, colors = SliderDefaults.colors(thumbColor = q.accent, activeTrackColor = q.accent, inactiveTrackColor = q.line))
    }
}

/* ---------------- logo ---------------- */

@Composable
private fun LogoTab(vm: AppViewModel) {
    val ctx = LocalContext.current; val st = vm.cStyle; val logo = st.logo
    val pick = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { val name = Logos.import(ctx, uri); if (name == null) vm.showToast(R.string.l_cant_open) else { vm.cStyle = vm.cStyle.copy(logo = vm.cStyle.logo.copy(kind = LogoKind.Image, image = name, size = 20)); vm.showToast(R.string.l_added) } }
    }
    val launch = { pick.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    if (logo.kind == LogoKind.Image && logo.image != null) {
        val bmp = remember(logo.image) { android.graphics.BitmapFactory.decodeFile(QrRenderer.logoFile(ctx, logo.image).path) }
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(q.surface2).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(52.dp).clip(RoundedCornerShape(15.dp)).background(q.surface), contentAlignment = Alignment.Center) { if (bmp != null) androidx.compose.foundation.Image(bmp.asImageBitmap(), null, Modifier.size(44.dp)) }
            Column(Modifier.weight(1f)) { Text(stringResource(R.string.l_yours), color = q.ink, fontWeight = FontWeight.Bold); Text(stringResource(R.string.l_note), color = q.muted, fontSize = 12.5.sp) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(stringResource(R.string.l_replace), { launch() }, Modifier.weight(1f), ghost = true, compact = true)
            AppButton(stringResource(R.string.l_remove), { vm.cStyle = st.copy(logo = logo.copy(kind = LogoKind.None, image = null, size = 20)) }, Modifier.weight(1f), ghost = true, compact = true, danger = true)
        }
    } else {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(q.accentSoft).border(BorderStroke(2.dp, q.accent.copy(alpha = .5f)), RoundedCornerShape(20.dp)).clickable(role = Role.Button) { launch() }.padding(14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).clip(RoundedCornerShape(15.dp)).background(q.surface), contentAlignment = Alignment.Center) { Ico("image", tint = q.accent, size = 26.dp) }
            Column { Text(stringResource(R.string.l_add), color = q.accent, fontWeight = FontWeight.Bold, fontSize = 15.sp); Text(stringResource(R.string.l_add_hint), color = q.muted, fontSize = 12.5.sp) }
        }
    }
    SurfaceCard {
        Lbl(stringResource(R.string.l_symbol))
        IconPaths.logoChoices.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { n ->
                    val on = logo.kind == LogoKind.Icon && logo.icon == n
                    Box(Modifier.weight(1f).height(48.dp).clip(RoundedCornerShape(14.dp)).background(if (on) q.accentSoft else q.surface2).border(1.5.dp, if (on) q.accent else Color.Transparent, RoundedCornerShape(14.dp)).clickable(role = Role.RadioButton) { vm.cStyle = st.copy(logo = logo.copy(kind = LogoKind.Icon, icon = n)) }, contentAlignment = Alignment.Center) { Ico(n, tint = if (on) q.accent else q.ink) }
                }
            }
        }
        LabeledField(stringResource(R.string.l_letters)) { QrTextField(if (logo.kind == LogoKind.Text) logo.text else "", { t -> vm.cStyle = st.copy(logo = if (t.isEmpty()) logo.copy(kind = LogoKind.None, text = "QR") else logo.copy(kind = LogoKind.Text, text = t)) }, placeholder = "NS", maxLength = 3) }
    }
    if (logo.active) SurfaceCard {
        SliderRow(stringResource(R.string.l_size), "${logo.size}%", logo.size.toFloat(), 12f..26f, steps = 13) { vm.cStyle = st.copy(logo = logo.copy(size = it.toInt())) }
        Lbl(stringResource(R.string.l_bg))
        SegRow(listOf("square" to stringResource(R.string.l_round), "round" to stringResource(R.string.l_circle), "none" to stringResource(R.string.l_none)), logo.plate.key, { vm.cStyle = st.copy(logo = logo.copy(plate = Plate.of(it))) })
        Text(stringResource(R.string.l_shrink_hint), color = q.muted, fontSize = 12.5.sp)
        AppButton(stringResource(R.string.l_remove_btn), { vm.cStyle = st.copy(logo = LogoStyle()) }, Modifier.fillMaxWidth(), ghost = true)
    }
}

/* ---------------- frame ---------------- */

@Composable
private fun FrameTab(vm: AppViewModel, isPro: Boolean) {
    val st = vm.cStyle; val f = st.frame
    SurfaceCard {
        Lbl(stringResource(R.string.fr_style))
        OptionGrid(5, listOf(
            Triple(FrameKind.None, stringResource(R.string.fr_none)) { c: Color -> FrameGlyph(FrameKind.None, c) }, Triple(FrameKind.Bottom, stringResource(R.string.fr_bottom)) { c: Color -> FrameGlyph(FrameKind.Bottom, c) },
            Triple(FrameKind.Top, stringResource(R.string.fr_top)) { c: Color -> FrameGlyph(FrameKind.Top, c) }, Triple(FrameKind.Tag, stringResource(R.string.fr_tag)) { c: Color -> FrameGlyph(FrameKind.Tag, c) },
            Triple(FrameKind.Border, stringResource(R.string.fr_border)) { c: Color -> FrameGlyph(FrameKind.Border, c) },
        ), f.kind) { vm.cStyle = st.copy(frame = f.copy(kind = it)) }
        if (f.kind == FrameKind.Bottom || f.kind == FrameKind.Top || f.kind == FrameKind.Tag) {
            LabeledField(stringResource(R.string.fr_text)) { QrTextField(f.text, { vm.cStyle = st.copy(frame = f.copy(text = it)) }, maxLength = 18) }
            val chips = listOf(R.string.fr_c1, R.string.fr_c2, R.string.fr_c3, R.string.fr_c4, R.string.fr_c5, R.string.fr_c6).map { stringResource(it) }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                chips.forEach { c -> Text(c, Modifier.clip(RoundedCornerShape(50)).background(q.surface2).clickable { vm.cStyle = st.copy(frame = f.copy(text = c)) }.padding(horizontal = 13.dp, vertical = 8.dp), color = q.ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
            }
        }
        if (f.kind != FrameKind.None) {
            if (isPro) ColorRow(stringResource(R.string.fr_color), f.color ?: st.fg, FgSwatches, { vm.cStyle = st.copy(frame = f.copy(color = it)) }, extra = { Text(stringResource(R.string.fr_match), Modifier.clip(RoundedCornerShape(10.dp)).clickable { vm.cStyle = st.copy(frame = f.copy(color = null)) }.padding(8.dp), color = q.accent, fontWeight = FontWeight.Bold) })
            else Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(q.surface2).border(1.5.dp, q.line, RoundedCornerShape(16.dp)).clickable(role = Role.Button) { vm.gate(CTab.Frame) }.padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.fr_color), color = q.ink, fontWeight = FontWeight.Bold)
                Row(Modifier.clip(RoundedCornerShape(50)).background(Brush.linearGradient(listOf(Gold1, Gold2))).padding(horizontal = 9.dp, vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) { Ico("lock", tint = GoldInk, size = 12.dp); Text(stringResource(R.string.pro).uppercase(), color = GoldInk, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp) }
            }
        }
    }
}
