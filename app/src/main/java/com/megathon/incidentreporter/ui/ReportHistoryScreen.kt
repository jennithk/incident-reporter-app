package com.megathon.incidentreporter.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.megathon.incidentreporter.data.IncidentReportEntity
import com.megathon.incidentreporter.data.ReportStore
import com.megathon.incidentreporter.data.toReport
import com.megathon.incidentreporter.ui.components.ContentColumn
import com.megathon.incidentreporter.ui.components.HoverCard
import com.megathon.incidentreporter.ui.theme.*
import kotlinx.coroutines.launch

private const val MotionMs = 200

// Screen E: history list + detail, Room-backed, works offline + after relaunch.
@Composable
fun ReportHistoryScreen(store: ReportStore, onBack: () -> Unit) {
    var items by remember { mutableStateOf<List<IncidentReportEntity>>(emptyList()) }
    var selected by remember { mutableStateOf<IncidentReportEntity?>(null) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { scope.launch { items = store.list() } }
    ContentColumn {
        Column(Modifier.fillMaxSize()) {
            Spacer(Modifier.height(16.dp))
            Text("Submitted reports", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("${items.size} saved locally • available offline", color = Ink2, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = selected != null,
                transitionSpec = { fadeIn(tween(MotionMs)) togetherWith fadeOut(tween(MotionMs)) },
                label = ""
            ) { detail ->
                if (detail) {
                    val sel = selected
                    if (sel != null) {
                        val r = sel.toReport()
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Surface),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Border),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                                Text("#${sel.id} • ${r.incidentType ?: "—"}", color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${r.incidentLocation ?: "—"} • ${r.dateTime ?: "—"}", color = Ink2, style = MaterialTheme.typography.bodySmall)
                                Spacer(Modifier.height(8.dp))
                                DetailRow("Severity", r.severityLevel)
                                DetailRow("Description", r.incidentDescription)
                                DetailRow("Actions", r.immediateActionsTaken)
                                DetailRow("Persons", r.personsInvolved)
                                DetailRow("Equipment", r.equipmentAssetInvolved)
                                DetailRow("Witness", r.witnessInformation)
                                DetailRow("Follow-up", r.recommendedFollowUpAction)
                                sel.transcript?.let { DetailRow("Transcript", it) }
                                Spacer(Modifier.height(12.dp))
                                OutlinedButton(onClick = { selected = null }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) {
                                    Text("Back to list", style = MaterialTheme.typography.labelLarge)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(items, key = { it.id }) { e ->
                            HoverCard(onClick = { selected = e }) {
                                Column(Modifier.padding(14.dp)) {
                                    Text("#${e.id} ${e.incidentType ?: "—"}", color = Ink, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text("${e.incidentLocation ?: "—"} • ${e.dateTime ?: "—"}", color = Ink2, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                    if (items.isEmpty()) Text("No reports yet. Complete the voice flow and submit.", color = Ink2, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)) {
                Text("Back", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Text("$label: ${value ?: "—"}", color = Ink, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 1.dp))
}
