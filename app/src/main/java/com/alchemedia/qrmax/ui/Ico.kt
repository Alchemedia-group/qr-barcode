package com.alchemedia.qrmax.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.alchemedia.qrmax.core.IconPaths

private val cache = HashMap<String, ImageVector>()

fun iconVector(name: String): ImageVector = cache.getOrPut(name) {
    ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).addPath(
        PathParser().parsePathString(IconPaths.map.getValue(name)).toNodes(),
        fill = null, stroke = SolidColor(Color.Black), strokeLineWidth = 1.8f, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round,
    ).build()
}

@Composable
fun Ico(name: String, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current, size: Dp = 22.dp) {
    Icon(iconVector(name), contentDescription = null, modifier = modifier.size(size), tint = tint)
}

/** The Laser Eye app icon: a QR corner square with a scan laser, on deep navy. */
@Composable
fun QrMaxLogo(size: Dp, modifier: Modifier = Modifier, rounded: Boolean = true) {
    Canvas(modifier.size(size)) {
        val u = this.size.minDimension / 108f
        drawRoundRect(BrandInk, size = this.size, cornerRadius = CornerRadius(if (rounded) 24f * u else 0f))
        val ring = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(androidx.compose.ui.geometry.Rect(32f * u, 32f * u, 76f * u, 76f * u)); addRect(androidx.compose.ui.geometry.Rect(41f * u, 41f * u, 67f * u, 67f * u))
        }
        drawPath(ring, Color.White)
        drawRect(Color.White, topLeft = Offset(47f * u, 47f * u), size = Size(14f * u, 14f * u))
        drawRoundRect(Laser.copy(alpha = .15f), Offset(24f * u, 49f * u), Size(60f * u, 12f * u), CornerRadius(6f * u))
        drawRoundRect(Laser.copy(alpha = .25f), Offset(24f * u, 51.5f * u), Size(60f * u, 7f * u), CornerRadius(3.5f * u))
        drawRect(Brush.horizontalGradient(listOf(Laser.copy(alpha = 0f), Laser, Laser, Laser.copy(alpha = 0f)), startX = 24f * u, endX = 84f * u), topLeft = Offset(24f * u, 53f * u), size = Size(60f * u, 4f * u))
    }
}
