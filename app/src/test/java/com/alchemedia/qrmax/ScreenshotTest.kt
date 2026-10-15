package com.alchemedia.qrmax

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.alchemedia.qrmax.core.*
import com.alchemedia.qrmax.ui.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/** Renders the real screens and writes PNGs to build/screenshots so the UI can be inspected without a device. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w412dp-h892dp-xxhdpi")
class ScreenshotTest {
    @get:Rule val compose = createComposeRule()
    private val out = File("build/screenshots").apply { mkdirs() }
    private fun vm() = AppViewModel(ApplicationProvider.getApplicationContext<Application>())

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.mainClock.autoAdvance = false
        compose.setContent { QrTheme { Box(Modifier.size(412.dp, 892.dp)) { content() } } }
        repeat(8) { Thread.sleep(250); compose.mainClock.advanceTimeBy(300); compose.waitForIdle() }
        val bmp: Bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        File(out, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    private fun sheet(vm: AppViewModel, body: @Composable () -> Unit): @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().background(Color(0xAA000000))) {
            Column(Modifier.fillMaxWidth().padding(top = 160.dp).background(q.surface).padding(20.dp).verticalScroll(rememberScrollState()), verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp)) { body() }
        }
    }

    @Test fun createTabsFree() {
        val vm = vm(); shot("create-content") { CreateScreen(vm) }
    }
    @Test fun createTabsPro() {
        val vm = vm(); vm.proManager.debugUnlock(true)
        vm.cTab = CTab.Color; shot("create-color") { CreateScreen(vm) }
    }
    @Test fun createShape() { val vm = vm(); vm.cTab = CTab.Shape; shot("create-shape") { CreateScreen(vm) } }
    @Test fun createFrame() { val vm = vm(); vm.cTab = CTab.Frame; vm.cStyle = vm.cStyle.copy(frame = FrameStyle(FrameKind.Bottom, "Scan me")); shot("create-frame") { CreateScreen(vm) } }
    @Test fun createLogoPro() { val vm = vm(); vm.proManager.debugUnlock(true); vm.cTab = CTab.Logo; vm.cStyle = vm.cStyle.copy(logo = LogoStyle(LogoKind.Icon, "heart")); shot("create-logo") { CreateScreen(vm) } }
    @Test fun history() { val vm = vm(); shot("history") { HistoryScreen(vm) } }
    @Test fun scanNoCamera() { val vm = vm(); shot("scan-permission") { ScanScreen(vm) } }
    private fun reticle(ph: ScanPhase) = shot("reticle-${ph.name.lowercase()}") { Box(Modifier.fillMaxSize().background(Color(0xFF111A2E))) { Reticle(ph) } }
    @Test fun reticleHunting() = reticle(ScanPhase.Hunting)
    @Test fun reticleTracking() = reticle(ScanPhase.Tracking)
    @Test fun reticleLocked() = reticle(ScanPhase.Locked)
    @Test fun splash() { shot("splash") { Splash() } }
    @Test fun tabBar() { val vm = vm(); shot("tabbar") { Box(Modifier.fillMaxSize().background(q.bg)) { TabBar(vm, Modifier.align(androidx.compose.ui.Alignment.BottomCenter)) } } }
    private fun wifi(vm: AppViewModel) = vm.store.saved.first { it.id == "c1" }
    @Test fun sheetResultSaved() { val vm = vm(); val w = wifi(vm); shot("sheet-result-saved", sheet(vm) { ResultSheet(vm, SheetKind.Result(w.raw, w.ts, w)) }) }
    @Test fun sheetResultUrl() { val vm = vm(); shot("sheet-result-url", sheet(vm) { ResultSheet(vm, SheetKind.Result("https://example.com/spring-menu", System.currentTimeMillis(), null)) }) }
    @Test fun sheetShare() { val vm = vm(); val w = wifi(vm); shot("sheet-share", sheet(vm) { ShareSheet(vm, SheetKind.Share(w.raw, "qr", w.style, "Guest Wi-Fi", null)) }) }
    @Test fun sheetPro() { val vm = vm(); shot("sheet-pro", sheet(vm) { ProSheet(vm, SheetKind.Pro(CTab.Color)) }) }
    @Test fun sheetLanguage() { val vm = vm(); shot("sheet-language", sheet(vm) { LanguageSheet(vm) }) }
    @Test fun sheetSave() { val vm = vm(); vm.cType = CodeType.Wifi; vm.cVals = CodeType.Wifi.defaults(); shot("sheet-save", sheet(vm) { SaveSheet(vm) }) }
}
