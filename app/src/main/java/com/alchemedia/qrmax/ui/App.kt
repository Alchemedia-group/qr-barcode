package com.alchemedia.qrmax.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.qrmax.AppViewModel
import com.alchemedia.qrmax.R
import com.alchemedia.qrmax.SheetKind
import com.alchemedia.qrmax.Tab
import com.alchemedia.qrmax.core.ProEvent
import kotlinx.coroutines.delay

@Composable
fun QrMaxApp(vm: AppViewModel) {
    QrTheme {
        var splash by rememberSaveable { mutableStateOf(true) }
        LaunchedEffect(Unit) { if (splash) { delay(1300); splash = false } }
        LaunchedEffect(Unit) {
            vm.proManager.events.collect { e ->
                when (e) {
                    ProEvent.Purchased -> { val intent = (vm.sheet as? SheetKind.Pro)?.intent; vm.sheet = null; vm.showToast(R.string.pro_thanks); intent?.let { vm.cTab = it } }
                    ProEvent.Pending -> vm.showToast(R.string.pro_pending)
                    ProEvent.NothingToRestore -> vm.showToast(R.string.pro_no_purchase)
                    ProEvent.Unavailable -> vm.showToast(R.string.pro_unavailable)
                }
            }
        }
        BackHandler(vm.sheet != null || vm.tab != Tab.Scan) { if (vm.sheet != null) vm.closeSheet() else vm.tab = Tab.Scan }
        Box(Modifier.fillMaxSize().background(q.bg)) {
            when (vm.tab) { Tab.Scan -> ScanScreen(vm); Tab.Create -> CreateScreen(vm); Tab.History -> HistoryScreen(vm) }
            TabBar(vm, Modifier.align(Alignment.BottomCenter))
            ToastHost(vm, Modifier.align(Alignment.BottomCenter))
            SheetHost(vm)
            AnimatedVisibility(splash, exit = fadeOut(tween(450))) { Splash() }
        }
    }
}

@Composable
internal fun TabBar(vm: AppViewModel, modifier: Modifier) {
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 14.dp).height(66.dp), contentAlignment = Alignment.BottomCenter) {
        Row(Modifier.fillMaxWidth().height(66.dp).shadow(14.dp, RoundedCornerShape(30.dp)).clip(RoundedCornerShape(30.dp)).background(q.surface.copy(alpha = .94f)).border(1.dp, q.line.copy(alpha = .8f), RoundedCornerShape(30.dp)), verticalAlignment = Alignment.CenterVertically) {
            SideTab(Modifier.weight(1f), "create", R.string.tab_create, vm.tab == Tab.Create) { vm.tab = Tab.Create }
            Box(Modifier.width(84.dp))
            SideTab(Modifier.weight(1f), "history", R.string.tab_history, vm.tab == Tab.History) { vm.tab = Tab.History }
        }
        Column(Modifier.align(Alignment.TopCenter).offset(y = (-14).dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(70.dp).shadow(12.dp, CircleShape, spotColor = q.accent).clip(CircleShape).border(6.dp, q.bg, CircleShape).background(Brush.linearGradient(listOf(Color.White.copy(alpha = .25f).compositeOverCompat(q.accent), q.accent)))
                .clickable(role = Role.Tab) { if (vm.tab == Tab.Scan) vm.resetScan() else vm.tab = Tab.Scan }.semantics { contentDescription = "" }, contentAlignment = Alignment.Center) { Ico("scan", tint = q.accentInk, size = 30.dp) }
        }
        Text(stringResource(R.string.tab_scan), Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp), color = if (vm.tab == Tab.Scan) q.accent else q.muted, fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
    }
}

private fun Color.compositeOverCompat(bg: Color) = this.compositeOver(bg)

@Composable
private fun SideTab(modifier: Modifier, icon: String, label: Int, on: Boolean, onClick: () -> Unit) {
    Column(modifier.fillMaxHeight().clickable(role = Role.Tab, onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Ico(icon, tint = if (on) q.accent else q.muted)
        Text(stringResource(label), color = if (on) q.accent else q.muted, fontWeight = FontWeight.Bold, fontSize = 11.5.sp, modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun ToastHost(vm: AppViewModel, modifier: Modifier) {
    val t = vm.toast
    LaunchedEffect(t?.id) { if (t != null) { delay(if (t.action != null) 5200 else 2800); if (vm.toast?.id == t.id) vm.toast = null } }
    Box(modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 18.dp).padding(bottom = 100.dp)) {
        AnimatedVisibility(t != null, enter = fadeIn() + slideInVertically { it / 2 }, exit = fadeOut() + slideOutVertically { it / 2 }) {
            val shown = remember(t?.id) { t }
            if (shown != null) Row(Modifier.fillMaxWidth().shadow(12.dp, RoundedCornerShape(16.dp)).clip(RoundedCornerShape(16.dp)).background(q.ink).padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(shown.res, *shown.args.toTypedArray()), Modifier.weight(1f), color = q.bg, fontSize = 13.5.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp)
                if (shown.action != null && shown.actionRes != null) Text(stringResource(shown.actionRes), Modifier.padding(start = 12.dp).clip(RoundedCornerShape(8.dp)).clickable { shown.action.invoke(); vm.toast = null }.padding(6.dp), color = q.accent, fontWeight = FontWeight.ExtraBold, fontSize = 13.5.sp)
            }
        }
    }
}

@Composable
internal fun Splash() {
    val scale = remember { Animatable(.6f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = .5f, stiffness = 200f)) }
    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF1C2540), Color(0xFF070B14)), radius = 1500f)).clickable(enabled = false) {}, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            QrMaxLogo(116.dp, Modifier.scale(scale.value))
            Text("QR Max", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.splash_tagline), color = Color.White.copy(alpha = .62f), fontSize = 13.sp, textAlign = TextAlign.Center)
        }
    }
}
