package com.alchemedia.qrmax.ui

import android.graphics.Bitmap
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import com.alchemedia.qrmax.AppViewModel
import com.alchemedia.qrmax.R
import com.alchemedia.qrmax.SheetKind
import com.alchemedia.qrmax.Tab
import com.alchemedia.qrmax.core.Parser
import com.alchemedia.qrmax.core.Primary
import com.alchemedia.qrmax.core.QrRenderer
import com.alchemedia.qrmax.core.QrStyle
import com.alchemedia.qrmax.core.Sharing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private tailrec fun android.content.Context.findActivity(): android.app.Activity? = when (this) { is android.app.Activity -> this; is android.content.ContextWrapper -> baseContext.findActivity(); else -> null }

/** Renders a code off the main thread. */
@Composable
fun rememberCode(raw: String, fmt: String, style: QrStyle, px: Int, verify: Boolean = false): State<QrRenderer.Out?> {
    val ctx = LocalContext.current
    return produceState<QrRenderer.Out?>(null, raw, fmt, style, px, verify) {
        value = if (raw.isEmpty()) null else withContext(Dispatchers.Default) { QrRenderer.render(ctx, raw, fmt, style, px, verify) }
    }
}

@Composable
fun CodeBox(bitmap: Bitmap?, modifier: Modifier = Modifier, placeholder: String? = null) {
    Box(modifier.clip(RoundedCornerShape(18.dp)).background(Color.White).border(1.dp, Color(0x14101828), RoundedCornerShape(18.dp)).padding(8.dp), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap.asImageBitmap(), null, Modifier.fillMaxWidth(), contentScale = androidx.compose.ui.layout.ContentScale.Fit)
        else if (placeholder != null) Text(placeholder, color = Color(0xFF65748B), fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(8.dp))
    }
}

@Composable
fun agoText(ts: Long): String {
    val ctx = LocalContext.current; val d = System.currentTimeMillis() - ts; val h = d / 3_600_000.0
    return if (h < 1) { if (d < 60_000) stringResource(R.string.ago_just_now) else stringResource(R.string.ago_min, Math.round(d / 60_000.0).toInt()) }
    else if (h < 24) stringResource(R.string.ago_hour, Math.round(h).toInt()) else Parser.formatDate(ctx, ts)
}

@Composable
private fun SheetHeader(icon: @Composable () -> Unit, title: String, subtitle: String?, onClose: () -> Unit, onBack: (() -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        if (onBack != null) Box(Modifier.size(40.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onBack), contentAlignment = Alignment.Center) { Ico("back", tint = q.ink) }
        icon()
        Column(Modifier.weight(1f)) {
            Text(title, color = q.ink, fontSize = 20.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (subtitle != null) Text(subtitle, color = q.muted, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.size(40.dp).clip(CircleShape).background(q.surface2).clickable(role = Role.Button, onClick = onClose), contentAlignment = Alignment.Center) { Ico("x", tint = q.ink) }
    }
}

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun SheetHost(vm: AppViewModel) {
    val s = vm.sheet ?: return
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = { vm.closeSheet() }, sheetState = state, containerColor = q.surface, contentColor = q.ink,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), dragHandle = { BottomSheetDefaults.DragHandle(color = q.line) },
    ) {
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            when (s) {
                is SheetKind.Result -> ResultSheet(vm, s)
                is SheetKind.Share -> ShareSheet(vm, s)
                SheetKind.Save -> SaveSheet(vm)
                SheetKind.Language -> LanguageSheet(vm)
                is SheetKind.Pro -> ProSheet(vm, s)
            }
        }
    }
}

@Composable
internal fun ResultSheet(vm: AppViewModel, s: SheetKind.Result) {
    val ctx = LocalContext.current
    val p = remember(s.raw) { Parser.parse(ctx, s.raw) }
    val item = s.item
    SheetHeader({ TintIcon(p.icon, p.hue, 52.dp, 26.dp) }, item?.name ?: p.label, (if (item != null) p.label + " · " else "") + agoText(s.ts), { vm.closeSheet() })
    if (item != null) { val out by rememberCode(item.raw, item.fmt, item.style, 600); Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CodeBox(out?.bitmap, Modifier.fillMaxWidth(.7f), out?.error) } }
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(q.surface2)) {
        p.rows.forEachIndexed { i, (k, v) ->
            if (i > 0) Box(Modifier.fillMaxWidth().height(1.dp).background(q.line))
            Row(Modifier.fillMaxWidth().padding(14.dp, 12.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(k, color = q.muted, fontSize = 14.5.sp)
                Text(v, Modifier.weight(1f), color = q.ink, fontWeight = FontWeight.Bold, fontSize = 14.5.sp, textAlign = TextAlign.End)
            }
        }
    }
    Text(s.raw, Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(q.surface2).padding(12.dp), color = q.muted, fontSize = 12.5.sp, fontFamily = FontFamily.Monospace, maxLines = 4, overflow = TextOverflow.Ellipsis)
    when (val pr = p.primary) {
        is Primary.Open -> AppButton(pr.label, { if (!Sharing.open(ctx, pr.uri)) vm.showToast(R.string.open_failed) }, Modifier.fillMaxWidth(), icon = "ext")
        is Primary.Copy -> AppButton(pr.label, { Sharing.copy(ctx, pr.text); vm.showToast(R.string.t_copied) }, Modifier.fillMaxWidth(), icon = "copy")
        null -> {}
    }
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppButton(stringResource(R.string.act_share), {
            val title = item?.name ?: p.title
            vm.sheet = SheetKind.Share(s.raw, item?.fmt ?: if (Regex("^\\d{13}$").matches(s.raw)) "EAN13" else "qr", item?.style ?: QrStyle(), title, s)
        }, Modifier.weight(1f), icon = "share", ghost = p.primary != null)
        AppButton(stringResource(R.string.act_copy), { Sharing.copy(ctx, s.raw); vm.showToast(R.string.t_copied) }, Modifier.weight(1f), icon = "copy", ghost = true)
    }
    if (item != null) AppButton(stringResource(R.string.act_edit), { vm.edit(item) }, Modifier.fillMaxWidth(), icon = "edit", ghost = true)
}

@Composable
internal fun ShareSheet(vm: AppViewModel, s: SheetKind.Share) {
    val ctx = LocalContext.current
    var asImage by remember { mutableStateOf(true) }
    val preview by rememberCode(s.raw, s.fmt, s.style, 480)
    SheetHeader({}, stringResource(R.string.share_title), stringResource(R.string.share_pick), { vm.closeSheet() }, if (s.back != null) ({ vm.sheet = s.back }) else null)
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(q.surface2).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        CodeBox(preview?.bitmap, Modifier.size(104.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(s.title, color = q.ink, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(s.raw, color = q.muted, fontSize = 12.5.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
    SegRow(listOf("image" to stringResource(R.string.share_image), "text" to stringResource(R.string.share_text)), if (asImage) "image" else "text", { asImage = it == "image" })
    AppButton(stringResource(R.string.share_chooser), {
        val bmp = if (asImage) QrRenderer.render(ctx, s.raw, s.fmt, s.style, 1024).bitmap else null
        if (bmp != null) Sharing.shareImage(ctx, bmp, s.raw, s.title) else Sharing.shareText(ctx, s.raw, s.title)
    }, Modifier.fillMaxWidth(), icon = "share")
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AppButton(stringResource(R.string.act_copy), { Sharing.copy(ctx, s.raw); vm.showToast(R.string.t_copied) }, Modifier.weight(1f), icon = "copy", ghost = true)
        AppButton(stringResource(R.string.save_picture), {
            val bmp = QrRenderer.render(ctx, s.raw, s.fmt, s.style, 1024).bitmap
            if (bmp != null && Sharing.saveToGallery(ctx, bmp)) vm.showToast(R.string.saved_picture) else vm.showToast(R.string.save_failed)
        }, Modifier.weight(1f), icon = "download", ghost = true)
    }
}

@Composable
internal fun SaveSheet(vm: AppViewModel) {
    val ctx = LocalContext.current
    val default = stringResource(R.string.default_name, stringResource(vm.cType.label))
    var name by remember { mutableStateOf(vm.cName) }
    SheetHeader({}, stringResource(R.string.save_title), stringResource(R.string.save_sub), { vm.closeSheet() })
    LabeledField(stringResource(R.string.name)) { QrTextField(name, { name = it }, placeholder = default) }
    SegRow(listOf("Personal" to stringResource(R.string.personal), "Business" to stringResource(R.string.business)), vm.cCat, { vm.cCat = it })
    AppButton(stringResource(R.string.save), {
        val n = name.trim().ifEmpty { default }
        vm.saveCurrent(n); vm.sheet = null
        vm.showToast(R.string.saved_toast, n, actionRes = R.string.view) { vm.tab = Tab.History }
    }, Modifier.fillMaxWidth(), icon = "bookmark")
    AppButton(stringResource(R.string.save_picture), {
        val bmp = QrRenderer.render(ctx, vm.payload(), vm.fmt(), vm.cStyle, 1024).bitmap
        if (bmp != null && Sharing.saveToGallery(ctx, bmp)) vm.showToast(R.string.saved_picture) else vm.showToast(R.string.save_failed)
    }, Modifier.fillMaxWidth(), icon = "download", ghost = true)
}

val LanguageCodes = listOf("en" to R.string.lang_english, "es" to R.string.lang_spanish, "de" to R.string.lang_german, "fr" to R.string.lang_french, "it" to R.string.lang_italian, "pt" to R.string.lang_portuguese, "hi" to R.string.lang_hindi, "id" to R.string.lang_indonesian)

@Composable
internal fun LanguageSheet(vm: AppViewModel) {
    val ctx = LocalContext.current
    val isPro by vm.isPro.collectAsState()
    val cfg = LocalConfiguration.current
    val current = remember(cfg) {
        val explicit = AppCompatDelegate.getApplicationLocales()
        val tag = if (!explicit.isEmpty) explicit[0]!!.language else cfg.locales[0].language
        (if (tag == "in") "id" else tag).takeIf { t -> LanguageCodes.any { it.first == t } } ?: "en"
    }
    SheetHeader({ TintIcon("globe", 205, 52.dp, 26.dp) }, stringResource(R.string.lang_title), stringResource(R.string.lang_sub), { vm.closeSheet() })
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LanguageCodes.forEach { (code, name) ->
            val on = code == current
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(if (on) q.accentSoft else q.surface2).border(1.5.dp, if (on) q.accent else Color.Transparent, RoundedCornerShape(16.dp))
                .clickable(role = Role.RadioButton) { vm.sheet = null; AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(code)) }.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(name), color = if (on) q.accent else q.ink, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                if (on) Ico("check", tint = q.accent)
            }
        }
    }
    if (com.alchemedia.qrmax.BuildConfig.DEBUG && isPro) AppButton(stringResource(R.string.pro_demo_reset), { vm.proManager.debugUnlock(false); vm.closeSheet(); vm.showToast(R.string.pro_reset_done) }, Modifier.fillMaxWidth(), ghost = true)
}

@Composable
internal fun ProSheet(vm: AppViewModel, s: SheetKind.Pro) {
    val ctx = LocalContext.current
    val price = vm.proManager.price.collectAsState().value ?: "£4.99"
    Box(Modifier.fillMaxWidth()) {
        Box(Modifier.align(Alignment.TopEnd).size(40.dp).clip(CircleShape).background(q.surface2).clickable(role = Role.Button) { vm.closeSheet() }, contentAlignment = Alignment.Center) { Ico("x", tint = q.ink) }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(Modifier.size(68.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Gold1, Gold2))), contentAlignment = Alignment.Center) { Ico("crown", tint = GoldInk, size = 34.dp) }
            Text(stringResource(R.string.pro_title), color = q.ink, fontSize = 25.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
            Text(stringResource(R.string.pro_sub), color = q.muted, textAlign = TextAlign.Center)
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
        listOf(R.string.pro_b1, R.string.pro_b2, R.string.pro_b3, R.string.pro_b4).forEach { id ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                Box(Modifier.size(24.dp).clip(CircleShape).background(q.accentSoft), contentAlignment = Alignment.Center) { Ico("check", tint = q.accent, size = 14.dp) }
                Text(stringResource(id), color = q.ink, fontWeight = FontWeight.Medium)
            }
        }
    }
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(q.surface2).padding(18.dp, 14.dp), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(price, color = q.ink, fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Column { Text(stringResource(R.string.pro_once), color = q.ink, fontWeight = FontWeight.Bold); Text(stringResource(R.string.pro_b5), color = q.muted, fontSize = 12.5.sp) }
    }
    AppButton(stringResource(R.string.pro_buy, price), { ctx.findActivity()?.let { vm.proManager.purchase(it) } }, Modifier.fillMaxWidth())
    if (com.alchemedia.qrmax.BuildConfig.DEBUG) AppButton(stringResource(R.string.pro_test_buy), { vm.proManager.debugUnlock(true); vm.sheet = null; vm.showToast(R.string.pro_thanks); s.intent?.let { vm.cTab = it } }, Modifier.fillMaxWidth(), ghost = true)
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(R.string.pro_restore), color = q.accent, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(role = Role.Button) { vm.proManager.restore() }.padding(8.dp))
        Text(stringResource(R.string.pro_not_now), color = q.accent, fontWeight = FontWeight.Bold, modifier = Modifier.clickable(role = Role.Button) { vm.closeSheet() }.padding(8.dp))
    }
}
