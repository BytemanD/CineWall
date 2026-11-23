package com.cinewall.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.cinewall.app.ui.theme.CineWallTypography

/** CineWall 投影光圈标志 */
@Composable
fun CineWallMark(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Canvas(modifier = modifier.width(size).height(size)) {
        val c = Offset(this.size.width / 2f, this.size.height / 2f)
        val r = this.size.minDimension / 2f
        val ringWidth = r * 0.11f
        drawCircle(color = color, radius = r - ringWidth / 2f, center = c, style = Stroke(width = ringWidth))
        drawCircle(color = color, radius = r * 0.30f, center = c)
        val beam = Path().apply {
            moveTo(c.x + r * 0.06f, c.y - r * 0.22f)
            lineTo(c.x + r * 0.64f, c.y - r * 0.52f)
            lineTo(c.x + r * 0.64f, c.y + r * 0.52f)
            lineTo(c.x + r * 0.06f, c.y + r * 0.22f)
            close()
        }
        drawPath(path = beam, color = color.copy(alpha = 0.22f))
        drawArc(
            color = color.copy(alpha = 0.9f),
            startAngle = -130f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(c.x - r * 0.68f, c.y - r * 0.68f),
            size = Size(r * 1.36f, r * 1.36f),
            style = Stroke(width = r * 0.07f)
        )
    }
}

@Composable
fun CineWallLogo(
    modifier: Modifier = Modifier,
    fontSize: Int = 21,
    markSize: Dp = 38.dp
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        CineWallMark(size = markSize)
        Spacer(Modifier.width((markSize.value * 0.3f).dp))
        Text(
            text = "CINEWALL",
            style = CineWallTypography.titleLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 3.sp,
                fontSize = fontSize.sp
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
