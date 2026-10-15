package com.alchemedia.qrmax

import android.app.Application
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.alchemedia.qrmax.core.CodeType
import com.alchemedia.qrmax.ui.CreateScreen
import com.alchemedia.qrmax.ui.HistoryScreen
import com.alchemedia.qrmax.ui.QrTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class CreateScreenTest {
    @get:Rule val compose = createComposeRule()
    private fun vm() = AppViewModel(ApplicationProvider.getApplicationContext<Application>())

    @Test fun freeUserTappingColorOrLogoGetsTheProSheet() {
        val vm = vm(); compose.setContent { QrTheme { CreateScreen(vm) } }
        compose.onNodeWithText("Color").performClick(); compose.waitForIdle(); assertEquals(SheetKind.Pro(CTab.Color), vm.sheet)
        vm.sheet = null; compose.onNodeWithText("Logo").performClick(); compose.waitForIdle(); assertEquals(SheetKind.Pro(CTab.Logo), vm.sheet)
        assertEquals(CTab.Content, vm.cTab)
    }

    @Test fun shapeAndFrameStayFree() {
        val vm = vm(); compose.setContent { QrTheme { CreateScreen(vm) } }
        compose.onNodeWithText("Shape").performClick(); compose.waitForIdle(); assertEquals(CTab.Shape, vm.cTab); assertTrue(vm.sheet == null)
        compose.onNodeWithText("CORNER FRAMES").assertExists()
        compose.onNodeWithText("Frame").performClick(); compose.waitForIdle(); assertEquals(CTab.Frame, vm.cTab)
        compose.onNodeWithText("FRAME STYLE").assertExists()
    }

    @Test fun proUserOpensColorAndSeesTheControls() {
        val vm = vm(); vm.proManager.debugUnlock(true); compose.setContent { QrTheme { CreateScreen(vm) } }
        compose.onNodeWithText("Color").performClick(); compose.waitForIdle(); assertEquals(CTab.Color, vm.cTab)
        compose.onNodeWithText("CODE COLOR").assertIsDisplayed(); compose.onNodeWithText("READY-MADE THEMES").assertIsDisplayed()
        compose.onNodeWithText("Logo").performClick(); compose.waitForIdle(); compose.onNodeWithText("Add your logo").assertIsDisplayed()
    }

    @Test fun theLiveCodeIsTestScannedAndSaysSo() {
        val vm = vm(); compose.setContent { QrTheme { CreateScreen(vm) } }
        compose.waitUntil(15_000) { runCatching { compose.onNodeWithText("Tested: scans correctly").assertIsDisplayed() }.isSuccess }
    }

    @Test fun switchingTypeUpdatesTheFormAndEmptyDetailsAsksForInput() {
        val vm = vm(); compose.setContent { QrTheme { CreateScreen(vm) } }
        compose.onNodeWithText("Wi-Fi").performClick(); compose.waitForIdle(); assertEquals(CodeType.Wifi, vm.cType)
        compose.onNodeWithText("NETWORK NAME").assertIsDisplayed()
    }

    @Test fun historyListsTheExamplesAndOpensAnItem() {
        val vm = vm(); compose.setContent { QrTheme { HistoryScreen(vm) } }
        compose.onNodeWithText("History").assertIsDisplayed(); compose.onNodeWithText("example.com").assertIsDisplayed()
        compose.onNodeWithText("example.com").performClick(); compose.waitForIdle()
        assertTrue(vm.sheet is SheetKind.Result)
    }
}
