package com.alchemedia.qrmax

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.alchemedia.qrmax.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var app: Application
    private lateinit var vm: AppViewModel

    @Before fun setUp() { Dispatchers.setMain(dispatcher); app = ApplicationProvider.getApplicationContext(); vm = AppViewModel(app) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test fun colourAndLogoAreLockedUntilPro() {
        assertFalse(vm.isPro.value)
        assertTrue(vm.gate(CTab.Color)); assertEquals(SheetKind.Pro(CTab.Color), vm.sheet)
        vm.sheet = null; assertTrue(vm.gate(CTab.Logo)); assertEquals(SheetKind.Pro(CTab.Logo), vm.sheet)
        vm.sheet = null
        vm.proManager.debugUnlock(true)
        assertTrue(vm.isPro.value); assertFalse(vm.gate(CTab.Color)); assertTrue(vm.sheet == null)
    }

    @Test fun proStaysUnlockedForANewSession() {
        vm.proManager.debugUnlock(true)
        assertTrue(AppViewModel(app).isPro.value)
        vm.proManager.debugUnlock(false); assertFalse(AppViewModel(app).isPro.value)
    }

    @Test fun aCodeSeenByTheCameraWalksThroughTheLockOnSteps() = runTest(dispatcher) {
        vm.onDetected("https://example.com/menu")
        runCurrent(); assertEquals(ScanPhase.Tracking, vm.phase); assertEquals(R.string.scan_spotted, vm.statusRes)
        advanceTimeBy(460); runCurrent(); assertEquals(ScanPhase.Reading, vm.phase)
        advanceTimeBy(510); runCurrent(); assertEquals(ScanPhase.Locked, vm.phase); assertEquals(R.string.scan_got, vm.statusRes)
        assertTrue("sheet should not open until the lock animation has played", vm.sheet == null)
        advanceTimeBy(810); runCurrent()
        val sheet = vm.sheet as SheetKind.Result; assertEquals("https://example.com/menu", sheet.raw)
        assertEquals("https://example.com/menu", vm.store.scans.first().raw)
        // detections while the sheet is open are ignored
        val before = vm.store.scans.size; vm.onDetected("other"); runCurrent(); assertEquals(before, vm.store.scans.size)
        vm.closeSheet(); runCurrent(); assertEquals(ScanPhase.Waiting, vm.phase)
        advanceTimeBy(1010); runCurrent(); assertEquals(ScanPhase.Hunting, vm.phase)
    }

    @Test fun detectionsAreIgnoredOffTheScanTab() = runTest(dispatcher) {
        vm.tab = Tab.History; vm.onDetected("x"); runCurrent(); assertEquals(ScanPhase.Hunting, vm.phase); assertTrue(vm.sheet == null)
    }

    @Test fun rescanningTheSameCodeDoesNotDuplicateHistory() {
        vm.store.recordScan("abc"); vm.store.recordScan("abc"); assertEquals(1, vm.store.scans.count { it.raw == "abc" })
    }

    @Test fun savedCodesKeepTheirTypeValuesAndStyleAndComeBackAfterARestart() {
        vm.selectType(CodeType.Wifi)
        vm.cStyle = QrStyle(fg = 0xFF701A75.toInt(), fg2 = 0xFFBE185D.toInt(), grad = true, shape = Shape.Classy, eye = Eye.Leaf, ball = Ball.Round, eyeSame = false, eyeColor = 0xFF123456.toInt(), ecl = 'Q', margin = 3,
            logo = LogoStyle(LogoKind.Icon, "wifi", "NS", null, 18, Plate.Round), frame = FrameStyle(FrameKind.Tag, "Wi-Fi", 0xFF9A3412.toInt()))
        vm.cCat = "Business"; vm.saveCurrent("Cafe wifi")
        val reopened = Store(app); val item = reopened.saved.first()
        assertEquals("Cafe wifi", item.name); assertEquals("Business", item.cat); assertEquals("wifi", item.type); assertEquals(vm.cStyle, item.style); assertEquals(vm.payload(), item.raw)
        vm.selectType(CodeType.Text); vm.cStyle = QrStyle()
        vm.edit(item)
        assertEquals(CodeType.Wifi, vm.cType); assertEquals(item.vals["ssid"], vm.cVals["ssid"]); assertEquals(item.style, vm.cStyle); assertEquals(Tab.Create, vm.tab); assertEquals(CTab.Content, vm.cTab)
    }

    @Test fun deleteCanBeUndoneInPlace() {
        val ids = vm.store.saved.map { it.id }; assertTrue(ids.size >= 2)
        vm.deleteSaved(ids[0]); assertEquals(ids.size - 1, vm.store.saved.size)
        val toast = vm.toast!!; assertEquals(R.string.deleted, toast.res); assertNotNull(toast.action); toast.action!!.invoke()
        assertEquals(ids, vm.store.saved.map { it.id })
        vm.deleteScan(vm.store.scans[1].id); val n = vm.store.scans.size; vm.toast!!.action!!.invoke(); assertEquals(n + 1, vm.store.scans.size)
    }

    @Test fun switchingToABarcodeLeavesTabsThatDoNotApply() {
        vm.cTab = CTab.Shape; vm.selectType(CodeType.Barcode); assertEquals(CTab.Content, vm.cTab)
        assertEquals("CODE128", vm.fmt()); vm.cVals = vm.cVals + ("fmt" to "EAN13"); assertEquals("EAN13", vm.fmt())
        vm.selectType(CodeType.Url); assertEquals("qr", vm.fmt())
    }

    @Test fun storeSurvivesAlmostAnyFileContents() {
        java.io.File(app.filesDir, "qrmax.json").writeText("{not json")
        val s = Store(app); assertEquals(3, s.scans.size); assertEquals(2, s.saved.size) // falls back to the example entries
    }
}
