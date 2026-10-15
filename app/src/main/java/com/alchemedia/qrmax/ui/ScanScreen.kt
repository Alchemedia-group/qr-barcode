package com.alchemedia.qrmax.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.alchemedia.qrmax.AppViewModel
import com.alchemedia.qrmax.R
import com.alchemedia.qrmax.ScanPhase
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

private val Green = Color(0xFF3DDC97)
private val picker by lazy { BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build()) }

private fun scanPicture(ctx: Context, uri: Uri, vm: AppViewModel) {
    try {
        picker.process(InputImage.fromFilePath(ctx, uri))
            .addOnSuccessListener { list -> val raw = list.firstNotNullOfOrNull { it.rawValue }; if (raw != null) vm.onPictureDetected(raw) else vm.showToast(R.string.no_code_picture) }
            .addOnFailureListener { vm.showToast(R.string.cant_read_picture) }
    } catch (e: Exception) { vm.showToast(R.string.cant_read_picture) }
}

private class CodeAnalyzer(private val enabled: () -> Boolean, private val onCode: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val scanner = BarcodeScanning.getClient(BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS).build())
    @ExperimentalGetImage
    override fun analyze(image: ImageProxy) {
        val media = image.image
        if (media == null || !enabled()) { image.close(); return }
        scanner.process(InputImage.fromMediaImage(media, image.imageInfo.rotationDegrees))
            .addOnSuccessListener { list -> list.firstNotNullOfOrNull { it.rawValue }?.let(onCode) }
            .addOnCompleteListener { image.close() }
    }
}

@Composable
fun ScanScreen(vm: AppViewModel) {
    val ctx = LocalContext.current
    var granted by remember { mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri -> if (uri != null) scanPicture(ctx, uri, vm) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var torch by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    val isPro by vm.isPro.collectAsState()
    DisposableEffect(Unit) { vm.onLocked = { haptic.performHapticFeedback(HapticFeedbackType.LongPress) }; onDispose { vm.onLocked = null; vm.resetScan() } }
    LaunchedEffect(torch, camera) { camera?.cameraControl?.enableTorch(torch) }

    Box(Modifier.fillMaxSize().background(Brush.radialGradient(listOf(Color(0xFF1C2540), Color(0xFF070B14)), radius = 1400f))) {
        if (granted) { CameraLayer(vm) { camera = it }; Reticle(vm.phase) } else PermissionCard { permission.launch(Manifest.permission.CAMERA) }
        Column(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars).padding(horizontal = 20.dp, vertical = 14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                QrMaxLogo(38.dp)
                Text("QR Max", Modifier.padding(start = 10.dp), color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
                if (isPro) Text(stringResource(R.string.pro).uppercase(), Modifier.padding(start = 8.dp).clip(RoundedCornerShape(50)).background(Brush.linearGradient(listOf(Gold1, Gold2))).padding(horizontal = 8.dp, vertical = 3.dp), color = GoldInk, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold)
                Box(Modifier.weight(1f))
                if (granted && camera?.cameraInfo?.hasFlashUnit() != false) RoundButton("torch", stringResource(R.string.torch), torch) { torch = !torch }
                Box(Modifier.size(8.dp))
                RoundButton("globe", stringResource(R.string.language), false) { vm.openLanguage() }
            }
            Box(Modifier.weight(1f))
            if (granted) Text(stringResource(vm.statusRes), Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(50)).background(if (vm.phase == ScanPhase.Locked) Green.copy(alpha = .28f) else Color.White.copy(alpha = .12f)).padding(horizontal = 16.dp, vertical = 8.dp), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp, textAlign = TextAlign.Center)
            Box(Modifier.size(14.dp))
            Row(Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .14f)).clickable(role = Role.Button) { pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }.padding(horizontal = 18.dp, vertical = 11.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Ico("image", tint = Color.White, size = 20.dp); Text(stringResource(R.string.from_picture), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
            Box(Modifier.size(96.dp))
        }
    }
}

@Composable
private fun RoundButton(icon: String, description: String, on: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(44.dp).clip(CircleShape).background(if (on) Color.White else Color.White.copy(alpha = .14f)).clickable(role = Role.Button, onClick = onClick).semantics { contentDescription = description }, contentAlignment = Alignment.Center) {
        Ico(icon, tint = if (on) BrandInk else Color.White)
    }
}

@Composable
private fun PermissionCard(onAllow: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(72.dp).clip(RoundedCornerShape(24.dp)).background(Color.White.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Ico("scan", tint = Color.White, size = 36.dp) }
        Text(stringResource(R.string.cam_permission_title), Modifier.padding(top = 20.dp), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        Text(stringResource(R.string.cam_permission_body), Modifier.padding(top = 8.dp, bottom = 22.dp), color = Color.White.copy(alpha = .7f), textAlign = TextAlign.Center)
        AppButton(stringResource(R.string.cam_permission_btn), onAllow)
    }
}

@Composable
private fun CameraLayer(vm: AppViewModel, onCamera: (Camera?) -> Unit) {
    val ctx = LocalContext.current; val owner = LocalLifecycleOwner.current
    val executor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; implementationMode = PreviewView.ImplementationMode.COMPATIBLE } }
    DisposableEffect(owner) {
        val future = ProcessCameraProvider.getInstance(ctx)
        var provider: ProcessCameraProvider? = null
        future.addListener({
            try {
                val p = future.get(); provider = p
                val preview = Preview.Builder().build().also { it.surfaceProvider = previewView.surfaceProvider }
                val analysis = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor, CodeAnalyzer({ vm.sheet == null && vm.phase == ScanPhase.Hunting }) { raw -> previewView.post { vm.onDetected(raw) } })
                p.unbindAll()
                onCamera(p.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis))
            } catch (e: Exception) { vm.showToast(R.string.cam_unavailable) }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose { provider?.unbindAll(); onCamera(null); executor.shutdown() }
    }
    AndroidView({ previewView }, Modifier.fillMaxSize())
}

/** The scanning frame: it breathes while hunting, shrinks onto a code, sweeps a laser, then locks on in green. */
@Composable
internal fun Reticle(phase: ScanPhase) {
    val inset by animateDpAsState(when (phase) { ScanPhase.Tracking, ScanPhase.Reading -> 24.dp; ScanPhase.Locked -> 14.dp; else -> 0.dp }, spring(dampingRatio = .55f, stiffness = 260f), label = "inset")
    val breathe by rememberInfiniteTransition(label = "b").animateFloat(0f, 10f, infiniteRepeatable(tween(1500), RepeatMode.Reverse), label = "breathe")
    val laser = remember { Animatable(0f) }
    LaunchedEffect(phase) {
        if (phase == ScanPhase.Locked || phase == ScanPhase.Waiting) return@LaunchedEffect
        val ms = when (phase) { ScanPhase.Hunting -> 1900; ScanPhase.Tracking -> 1100; else -> 700 }
        while (true) { laser.snapTo(0f); laser.animateTo(1f, tween(ms, easing = LinearEasing)) }
    }
    val ripple = remember { Animatable(0f) }
    val pop by animateFloatAsState(if (phase == ScanPhase.Locked) 1f else 0f, spring(dampingRatio = .5f, stiffness = 300f), label = "pop")
    LaunchedEffect(phase) { if (phase == ScanPhase.Locked) { ripple.snapTo(0f); ripple.animateTo(1f, tween(900)) } else ripple.snapTo(0f) }
    val color = if (phase == ScanPhase.Locked) Green else Color.White
    Canvas(Modifier.fillMaxSize()) {
        val side = 252.dp.toPx(); val c = Offset(size.width / 2f, size.height * 0.40f)
        val box = Rect(c.x - side / 2, c.y - side / 2, c.x + side / 2, c.y + side / 2)
        val dim = Path().apply { addRect(Rect(Offset.Zero, size)) }
        val hole = Path().apply { addRoundRect(RoundRect(box.inflate(4.dp.toPx()), CornerRadius(28.dp.toPx()))) }
        clipPath(hole, ClipOp.Difference) { drawRect(Color(0x66030610)) }
        val off = (inset + (if (phase == ScanPhase.Hunting) breathe.dp else 0.dp)).toPx()
        val len = 46.dp.toPx(); val sw = 5.dp.toPx(); val r = 22.dp.toPx()
        fun corner(x: Float, y: Float, sx: Float, sy: Float) {
            val p = Path().apply { moveTo(x, y + sy * len); lineTo(x, y + sy * r); quadraticTo(x, y, x + sx * r, y); lineTo(x + sx * len, y) }
            drawPath(p, color, style = Stroke(sw, cap = StrokeCap.Round))
        }
        corner(box.left + off, box.top + off, 1f, 1f); corner(box.right - off, box.top + off, -1f, 1f); corner(box.left + off, box.bottom - off, 1f, -1f); corner(box.right - off, box.bottom - off, -1f, -1f)
        if (phase != ScanPhase.Locked && phase != ScanPhase.Waiting) {
            val y = box.top + 14.dp.toPx() + (box.height - 28.dp.toPx()) * laser.value
            val a = (minOf(laser.value / .12f, (1f - laser.value) / .12f, 1f)).coerceIn(0f, 1f)
            drawRect(Brush.verticalGradient(listOf(Laser.copy(alpha = 0f), Laser.copy(alpha = .4f * a)), startY = y - 70.dp.toPx(), endY = y), Offset(box.left + 8.dp.toPx(), y - 70.dp.toPx()), Size(box.width - 16.dp.toPx(), 70.dp.toPx()))
            drawRect(Brush.horizontalGradient(listOf(Laser.copy(alpha = 0f), Laser.copy(alpha = a), Laser.copy(alpha = a), Laser.copy(alpha = 0f)), startX = box.left, endX = box.right), Offset(box.left + 8.dp.toPx(), y - 1.5.dp.toPx()), Size(box.width - 16.dp.toPx(), 3.dp.toPx()))
        }
        if (phase == ScanPhase.Locked) {
            for (k in 0..1) { val t = (ripple.value - k * .2f).coerceIn(0f, 1f); if (t > 0f) drawCircle(Green.copy(alpha = .85f * (1f - t)), radius = 110.dp.toPx() * (0.4f + 1.35f * t), center = c, style = Stroke(3.dp.toPx())) }
            val rad = 38.dp.toPx() * pop
            drawCircle(Color(0xFF1FCF84), rad, c)
            val s = rad / 38f
            val tick = Path().apply { moveTo(c.x - 12 * s, c.y + 1 * s); lineTo(c.x - 3 * s, c.y + 10 * s); lineTo(c.x + 14 * s, c.y - 9 * s) }
            drawPath(tick, Color.White, style = Stroke(6.dp.toPx() * pop, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}
