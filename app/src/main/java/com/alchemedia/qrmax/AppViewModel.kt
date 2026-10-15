package com.alchemedia.qrmax

import android.app.Application
import androidx.annotation.StringRes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alchemedia.qrmax.core.CodeType
import com.alchemedia.qrmax.core.ProManager
import com.alchemedia.qrmax.core.QrStyle
import com.alchemedia.qrmax.core.SavedItem
import com.alchemedia.qrmax.core.Store
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class Tab { Create, Scan, History }
enum class CTab { Content, Color, Shape, Logo, Frame }
enum class ScanPhase { Hunting, Tracking, Reading, Locked, Waiting }

sealed interface SheetKind {
    data class Result(val raw: String, val ts: Long, val item: SavedItem?) : SheetKind
    data class Share(val raw: String, val fmt: String, val style: QrStyle, val title: String, val back: SheetKind?) : SheetKind
    data object Save : SheetKind
    data object Language : SheetKind
    data class Pro(val intent: CTab?) : SheetKind
}

class ToastMsg(val id: Long, @StringRes val res: Int, val args: List<Any> = emptyList(), @StringRes val actionRes: Int? = null, val action: (() -> Unit)? = null)

class AppViewModel(app: Application) : AndroidViewModel(app) {
    val store = Store(app)
    val proManager = ProManager(app).also { it.start() }
    val isPro: StateFlow<Boolean> get() = proManager.isPro

    var tab by mutableStateOf(Tab.Scan)
    var sheet by mutableStateOf<SheetKind?>(null)
    var toast by mutableStateOf<ToastMsg?>(null)
    /** Bumped whenever history or saved codes change, so lists redraw. */
    var rev by mutableIntStateOf(0)

    fun showToast(@StringRes res: Int, vararg args: Any, @StringRes actionRes: Int? = null, action: (() -> Unit)? = null) {
        toast = ToastMsg(System.nanoTime(), res, args.toList(), actionRes, action)
    }

    /* ---------------- scan ---------------- */
    var phase by mutableStateOf(ScanPhase.Hunting)
    var statusRes by mutableIntStateOf(R.string.scan_looking)
    private var scanJob: Job? = null
    var onLocked: (() -> Unit)? = null

    fun onDetected(raw: String) {
        if (phase != ScanPhase.Hunting || sheet != null || tab != Tab.Scan) return
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            phase = ScanPhase.Tracking; statusRes = R.string.scan_spotted; delay(450)
            phase = ScanPhase.Reading; statusRes = R.string.scan_reading; delay(500)
            phase = ScanPhase.Locked; statusRes = R.string.scan_got; onLocked?.invoke(); delay(800)
            store.recordScan(raw); rev++
            sheet = SheetKind.Result(raw, System.currentTimeMillis(), null)
        }
    }

    /** A code read from a picture skips the hunting phase and goes straight to the lock-on animation. */
    fun onPictureDetected(raw: String) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            phase = ScanPhase.Locked; statusRes = R.string.scan_got; onLocked?.invoke(); delay(700)
            store.recordScan(raw); rev++
            sheet = SheetKind.Result(raw, System.currentTimeMillis(), null)
        }
    }

    fun resumeScanning() {
        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            phase = ScanPhase.Waiting; statusRes = R.string.scan_next; delay(1000)
            phase = ScanPhase.Hunting; statusRes = R.string.scan_looking
        }
    }

    fun resetScan() { scanJob?.cancel(); phase = ScanPhase.Hunting; statusRes = R.string.scan_looking }

    /* ---------------- sheets ---------------- */
    fun closeSheet() {
        val was = sheet; sheet = null
        if (tab == Tab.Scan && (was is SheetKind.Result || was is SheetKind.Share || was is SheetKind.Language)) resumeScanning()
    }

    fun openLanguage() { scanJob?.cancel(); phase = ScanPhase.Waiting; sheet = SheetKind.Language }

    /* ---------------- create ---------------- */
    var cType by mutableStateOf(CodeType.Url)
    var cVals by mutableStateOf(CodeType.Url.defaults())
    var cStyle by mutableStateOf(QrStyle())
    var cTab by mutableStateOf(CTab.Content)
    var cName by mutableStateOf("")
    var cCat by mutableStateOf("Personal")

    fun selectType(t: CodeType) {
        cType = t; cVals = t.defaults()
        if (t == CodeType.Barcode && cTab in listOf(CTab.Shape, CTab.Logo, CTab.Frame)) cTab = CTab.Content
    }
    fun payload(): String = cType.build(cVals)
    fun fmt(): String = if (cType == CodeType.Barcode) (cVals["fmt"] ?: "CODE128") else "qr"

    /** Colours and logos are Pro. Returns true when the Pro sheet was shown instead. */
    fun gate(intent: CTab?): Boolean { if (isPro.value) return false; sheet = SheetKind.Pro(intent); return true }

    fun saveCurrent(name: String) {
        val raw = payload(); if (raw.isEmpty()) return
        val item = SavedItem("c" + System.currentTimeMillis(), name, cCat, cType.key, cVals, raw, fmt(), cStyle, System.currentTimeMillis())
        store.saved.add(0, item); store.save(); rev++; cName = name
    }

    fun edit(item: SavedItem) {
        val t = CodeType.of(item.type) ?: return
        cType = t; cVals = t.defaults() + item.vals; cStyle = item.style; cName = item.name; cTab = CTab.Content; tab = Tab.Create; sheet = null
    }

    /* ---------------- history ---------------- */
    fun deleteScan(id: String) {
        val i = store.scans.indexOfFirst { it.id == id }; if (i < 0) return
        val item = store.scans.removeAt(i); store.save(); rev++
        showToast(R.string.deleted, actionRes = R.string.undo) { store.scans.add(i.coerceAtMost(store.scans.size), item); store.save(); rev++ }
    }
    fun deleteSaved(id: String) {
        val i = store.saved.indexOfFirst { it.id == id }; if (i < 0) return
        val item = store.saved.removeAt(i); store.save(); rev++
        showToast(R.string.deleted, actionRes = R.string.undo) { store.saved.add(i.coerceAtMost(store.saved.size), item); store.save(); rev++ }
    }
}
