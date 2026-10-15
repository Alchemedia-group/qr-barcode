package com.alchemedia.qrmax.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.qrmax.AppViewModel
import com.alchemedia.qrmax.R
import com.alchemedia.qrmax.SheetKind
import com.alchemedia.qrmax.core.FieldKind
import com.alchemedia.qrmax.core.Parser
import com.alchemedia.qrmax.core.QrStyle

private data class Row2(val id: String, val raw: String, val name: String?, val cat: String?, val ts: Long, val example: Boolean, val fmt: String, val style: QrStyle)

@Composable
fun HistoryScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    val cfg = LocalConfiguration.current
    var seg by rememberSaveable { mutableStateOf("scan") }
    var query by rememberSaveable { mutableStateOf("") }
    @Suppress("UNUSED_VARIABLE") val rev = vm.rev
    val rows = if (seg == "scan") vm.store.scans.map { Row2(it.id, it.raw, null, null, it.ts, it.example, if (Regex("^\\d{13}$").matches(it.raw)) "EAN13" else "qr", QrStyle()) }
    else vm.store.saved.map { Row2(it.id, it.raw, it.name, it.cat, it.ts, it.example, it.fmt, it.style) }
    val shown = rows.filter { r -> val p = Parser.parse(ctx, r.raw); query.isBlank() || (r.raw + " " + (r.name ?: "") + " " + p.label + " " + p.title).lowercase().contains(query.trim().lowercase()) }

    Column(Modifier.fillMaxSize().background(q.bg).windowInsetsPadding(WindowInsets.statusBars).imePadding()) {
        Column(Modifier.padding(horizontal = 22.dp).padding(top = 18.dp, bottom = 8.dp)) {
            Text(stringResource(R.string.hist_title), color = q.ink, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.hist_sub), color = q.muted, fontSize = 14.5.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SegRow(listOf("scan" to stringResource(R.string.hist_scanned), "saved" to stringResource(R.string.hist_mycodes)), seg, { seg = it })
            QrTextField(query, { query = it }, placeholder = stringResource(R.string.hist_search), label = stringResource(R.string.hist_search))
        }
        if (shown.isEmpty()) {
            Text(if (query.isNotBlank()) stringResource(R.string.hist_none_match, query) else if (seg == "scan") stringResource(R.string.hist_empty_scan) else stringResource(R.string.hist_empty_saved),
                Modifier.fillMaxWidth().padding(36.dp), color = q.muted, textAlign = TextAlign.Center, lineHeight = 22.sp)
        } else LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 130.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shown, key = { it.id }) { r ->
                val p = remember(r.raw, cfg) { Parser.parse(ctx, r.raw) }
                val cat = when (r.cat) { "Business" -> stringResource(R.string.business); "Personal" -> stringResource(R.string.personal); else -> null }
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(q.surface).padding(10.dp, 10.dp, 6.dp, 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).clickable(role = Role.Button) {
                        vm.sheet = SheetKind.Result(r.raw, r.ts, if (seg == "saved") vm.store.saved.firstOrNull { it.id == r.id } else null)
                    }, horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        TintIcon(p.icon, p.hue)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(r.name ?: p.title, Modifier.weight(1f, fill = false), color = q.ink, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (r.example) Text(stringResource(R.string.tag_example).uppercase(), Modifier.clip(RoundedCornerShape(6.dp)).background(q.surface2).padding(horizontal = 6.dp, vertical = 2.dp), color = q.muted, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold)
                            }
                            Text((if (cat != null) "$cat · " else "") + p.label + " · " + agoText(r.ts), color = q.muted, fontSize = 12.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                    IconAction("share", stringResource(R.string.act_share)) {
                        vm.sheet = SheetKind.Share(r.raw, r.fmt, r.style, r.name ?: p.title, null)
                    }
                    IconAction("trash", stringResource(R.string.delete)) { if (seg == "scan") vm.deleteScan(r.id) else vm.deleteSaved(r.id) }
                }
            }
        }
    }
}

@Composable
private fun IconAction(icon: String, description: String, onClick: () -> Unit) {
    Box(Modifier.size(42.dp).clip(CircleShape).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }, contentAlignment = Alignment.Center) { Ico(icon, tint = q.muted) }
}
