package com.megathon.incidentreporter.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.megathon.incidentreporter.audio.TtsPlayer
import com.megathon.incidentreporter.ui.components.*
import com.megathon.incidentreporter.ui.theme.*
import com.megathon.incidentreporter.viewmodel.IncidentViewModel
import com.megathon.incidentreporter.viewmodel.Speaker
import com.megathon.incidentreporter.viewmodel.Stage

private const val MotionMs = 200

// Screen B: Voice Recording + Conversation (voice-first, optional typed fallback)
@Composable
fun ConversationScreen(vm: IncidentViewModel, onReview: () -> Unit) {
    val st by vm.s.collectAsState()
    val ctx = LocalContext.current
    var showTextFallback by remember { mutableStateOf(false) }
    var typed by remember { mutableStateOf("") }
    val tts = remember { TtsPlayer(ctx) }
    DisposableEffect(Unit) { onDispose { tts.stop(); tts.release() } }

    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.startRecording(st.followUpQuestion != null) else {
            // Denial handled gracefully: stay, show retry. No forced text workflow.
        }
    }
    fun mic() {
        if (st.busy || st.isRecording) return
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            perm.launch(Manifest.permission.RECORD_AUDIO); return
        }
        vm.startRecording(st.followUpQuestion != null || st.conversation.isNotEmpty())
    }

    // Speak each new AI question aloud (offline voice), no overlap.
    LaunchedEffect(st.followUpQuestion) { st.followUpQuestion?.let { tts.speak(it) } }

    val listState = rememberLazyListState()
    LaunchedEffect(st.conversation.size) { if (st.conversation.isNotEmpty()) listState.animateScrollToItem(st.conversation.size - 1) }

    ContentColumn {
        Column(Modifier.fillMaxSize().imePadding()) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Report session", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                DemoBanner()
            }
            Spacer(Modifier.height(12.dp))
            WaveformCard(active = st.isRecording)
            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    st.isRecording -> "Recording ${formatDuration(st.recordingSec)} — press Stop when done"
                    st.stage == Stage.TRANSCRIBING -> "Transcribing voice…"
                    st.stage == Stage.EXTRACTING -> "Extracting details…"
                    st.followUpQuestion != null -> "Assistant asked a follow-up — answer by voice"
                    st.conversation.isEmpty() -> "Press the microphone and describe what happened"
                    else -> "Ready for review"
                },
                color = Ink2, style = MaterialTheme.typography.bodySmall
            )
            Spacer(Modifier.height(8.dp))
            LazyColumn(Modifier.weight(1f), state = listState, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(st.conversation, key = { it.timestampMs.hashCode().toString() + it.text.hashCode() }) { m ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(tween(MotionMs)) + slideInVertically(tween(MotionMs)) { it / 4 },
                        exit = fadeOut(tween(MotionMs))
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = if (m.speaker == Speaker.ASSISTANT) Arrangement.Start else Arrangement.End) {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (m.speaker == Speaker.ASSISTANT) Surface else Accent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = if (m.speaker == Speaker.ASSISTANT) BorderStroke(1.dp, Border) else null,
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                                modifier = Modifier.widthIn(max = 340.dp)
                            ) {
                                Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.Top) {
                                    if (m.speaker == Speaker.ASSISTANT) {
                                        Icon(Icons.Default.SmartToy, null, tint = Accent, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text(
                                        m.text,
                                        color = if (m.speaker == Speaker.ASSISTANT) Ink else androidx.compose.ui.graphics.Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
            AnimatedVisibility(
                visible = st.followUpQuestion != null,
                enter = fadeIn(tween(MotionMs)), exit = fadeOut(tween(MotionMs))
            ) {
                Column {
                    OutlinedButton(onClick = { tts.replay() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) {
                        Icon(Icons.Default.Replay, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Replay question", style = MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            AnimatedVisibility(
                visible = st.stage == Stage.ERROR,
                enter = fadeIn(tween(MotionMs)), exit = fadeOut(tween(MotionMs))
            ) {
                Column {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DangerBg),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, Danger.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(st.errorMessage ?: "Error", color = Danger, style = MaterialTheme.typography.bodySmall)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { vm.retryFromError() }, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Retry") }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (st.isRecording) {
                    Button(
                        onClick = { vm.stopAndProcess() },
                        modifier = Modifier.weight(1f).height(52.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Recording)
                    ) {
                        Icon(Icons.Default.Stop, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Stop (${formatDuration(st.recordingSec)})", style = MaterialTheme.typography.titleSmall)
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { vm.discardRecording() }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) {
                        Text("Discard", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        PulsingMicButton(listening = false, enabled = !st.busy) { mic() }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            // Optional secondary fallback only — never required.
            if (!showTextFallback) {
                TextButton(onClick = { showTextFallback = true }) {
                    Text("Type instead (optional)", color = Ink2, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(typed, { typed = it }, Modifier.weight(1f), placeholder = { Text("Optional typed answer…") }, singleLine = true)
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { vm.answerWithText(typed); typed = "" }, enabled = typed.isNotBlank() && !st.busy, colors = ButtonDefaults.buttonColors(containerColor = Accent)) { Text("Send") }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onReview,
                enabled = st.conversation.isNotEmpty() && !st.busy,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Text("Review report", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.width(8.dp))
                Icon(Icons.Default.ArrowForward, null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
