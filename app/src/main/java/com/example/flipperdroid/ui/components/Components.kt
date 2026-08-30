package com.example.flipperdroid.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

@Composable
fun HeroBanner(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surface)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            colors.primary.copy(alpha = 0.18f),
                            colors.secondary.copy(alpha = 0.08f),
                            colors.surface
                        )
                    )
                )
                .padding(20.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    Icons.Default.Shield,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(34.dp)
                )
                Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun StatusCard(
    title: String,
    detail: String,
    ok: Boolean,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    ElevatedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(10.dp).size(24.dp)
                )
            }
            Column(Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                if (ok) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                contentDescription = null,
                tint = if (ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
fun FeatureTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text(title, fontWeight = FontWeight.Bold)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectionTitle(title: String, action: (@Composable () -> Unit)? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun ValueCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SignalPill(rssi: Int) {
    val text = when {
        rssi >= -55 -> "Excellent"
        rssi >= -67 -> "Bon"
        rssi >= -75 -> "Moyen"
        else -> "Faible"
    }
    AssistChip(onClick = {}, enabled = false, label = { Text("$text · $rssi dBm") })
}

fun maskMac(value: String, privacy: Boolean): String {
    if (!privacy) return value
    val parts = value.split(":")
    return if (parts.size >= 6) "${parts[0]}:${parts[1]}:**:**:${parts.takeLast(2).joinToString(":")}" else "••••"
}

/**
 * Simple bar chart of Wi‑Fi channel occupancy (2.4 GHz, channels 1–13), drawn with
 * Canvas. Helps pick the least crowded channel — channels 1/6/11 are the only
 * non-overlapping ones in most regions, so a quick visual makes that obvious.
 */
@Composable
fun ChannelOccupancyChart(counts: Map<Int, Int>, modifier: Modifier = Modifier) {
    val maxChannel = 13
    val maxCount = (counts.values.maxOrNull() ?: 0).coerceAtLeast(1)
    val barColor = MaterialTheme.colorScheme.primary
    val cleanColor = MaterialTheme.colorScheme.tertiary
    val nonOverlapping = setOf(1, 6, 11)
    Column(modifier.fillMaxWidth()) {
        Canvas(Modifier.fillMaxWidth().height(90.dp)) {
            val barWidth = size.width / maxChannel
            for (channel in 1..maxChannel) {
                val count = counts[channel] ?: 0
                val barHeight = if (count == 0) 2f else (count.toFloat() / maxCount) * size.height
                val x = (channel - 1) * barWidth
                drawRect(
                    color = if (channel in nonOverlapping) cleanColor else barColor,
                    topLeft = androidx.compose.ui.geometry.Offset(x + barWidth * 0.15f, size.height - barHeight),
                    size = androidx.compose.ui.geometry.Size(barWidth * 0.7f, barHeight)
                )
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("1", style = MaterialTheme.typography.labelSmall)
            Text("6", style = MaterialTheme.typography.labelSmall)
            Text("11", style = MaterialTheme.typography.labelSmall)
            Text("13", style = MaterialTheme.typography.labelSmall)
        }
    }
}

/**
 * Minimal RSSI trend sparkline drawn with Canvas — no external charting library needed.
 * Purely visual aid to spot whether a device's signal is strengthening or weakening
 * during a walk-test, without adding any extra dependency to the project.
 */
@Composable
fun RssiSparkline(history: List<Int>, modifier: Modifier = Modifier) {
    if (history.size < 2) return
    val lineColor = MaterialTheme.colorScheme.primary
    val min = history.min().toFloat()
    val max = history.max().toFloat()
    val range = (max - min).coerceAtLeast(1f)
    Canvas(modifier.fillMaxWidth().height(28.dp)) {
        val stepX = size.width / (history.size - 1).coerceAtLeast(1)
        val path = Path()
        history.forEachIndexed { index, value ->
            val x = index * stepX
            val normalized = (value - min) / range
            val y = size.height - (normalized * size.height)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color = lineColor, style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
