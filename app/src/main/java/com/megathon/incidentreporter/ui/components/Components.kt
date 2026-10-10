package com.megathon.incidentreporter.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.megathon.incidentreporter.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.sin

private const val MotionMs = 200

@Composable
fun DemoBanner() {
    Surface(
        color = AccentSoft,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, Border)
    ) {
        Text(
            "Demo mode — simulated AI responses",
            color = Accent,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}

// Voice capture button. Static size; only container color transitions with state.
// Same signature as before so call sites are untouched.
@Composable
fun PulsingMicButton(listening: Boolean, enabled: Boolean = true, onClick: () -> Unit) {
    val container by animateColorAsState(
        if (listening) Recording else Accent, animationSpec = tween(MotionMs), label = ""
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = onClick, enabled = enabled,
            modifier = Modifier.size(72.dp), shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = container)
        ) {
            Icon(
                if (listening) Icons.Default.Stop else Icons.Default.Mic, null,
                modifier = Modifier.size(30.dp), tint = Color.White
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (listening) {
                Icon(Icons.Default.FiberManualRecord, null, tint = Recording, modifier = Modifier.size(10.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                if (listening) "Recording" else "Tap to speak",
                color = if (listening) Recording else Ink2,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun WaveformCard(active: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        var t by remember { mutableStateOf(0f) }
        LaunchedEffect(active) { while (true) { t += if (active) 0.28f else 0.03f; delay(60) } }
        Canvas(Modifier.fillMaxWidth().height(72.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
            val n = 56
            for (i in 0 until n) {
                val amp = if (active) (0.25f + 0.75f * abs(sin(t + i * 0.45f))) * size.height / 2 else 4f
                val x = i * size.width / n
                drawLine(if (active) Accent else Ink3, Offset(x, center.y - amp), Offset(x, center.y + amp), strokeWidth = 5f)
            }
        }
    }
}

// Centered, width-constrained content column used by every screen.
@Composable
fun ContentColumn(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(Modifier.fillMaxSize().background(Bg), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier.fillMaxWidth().widthIn(max = 640.dp).padding(horizontal = 20.dp),
            content = content
        )
    }
}

// Hover-aware card for clickable rows (mouse/trackpad hover → border accent;
// touch → default pressed/focused ripple). Visual only.
@Composable
fun HoverCard(onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    val border by animateColorAsState(
        if (hovered) Accent else Border, animationSpec = tween(MotionMs), label = ""
    )
    Card(
        onClick = onClick,
        interactionSource = interaction,
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, hoveredElevation = 3.dp),
        modifier = modifier.hoverable(interaction).fillMaxWidth()
    ) { content() }
}

fun formatDuration(sec: Double): String {
    val s = sec.toInt()
    return "%d:%02d / 1:00".format(s / 60, s % 60)
}
