package com.megathon.incidentreporter.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.megathon.incidentreporter.contract.IncidentReport
import com.megathon.incidentreporter.ui.components.ContentColumn
import com.megathon.incidentreporter.ui.theme.*
import com.megathon.incidentreporter.viewmodel.IncidentViewModel
import com.megathon.incidentreporter.viewmodel.Stage

private const val MotionMs = 200

// Screen C: 10 editable fields + Screen D: submit + confirmation.
@Composable
fun ReportReviewScreen(vm: IncidentViewModel, onSubmitted: () -> Unit, onBack: () -> Unit) {
    val st by vm.s.collectAsState()
    val d = st.draft
    if (st.stage == Stage.SUBMITTED) {
        SubmittedView(report = d, id = st.savedId, onHome = onBack)
        return
    }
    ContentColumn {
        Column(Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState())) {
            Spacer(Modifier.height(16.dp))
            Text("Review report", color = Ink, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("All 10 fields are editable. Fields marked required must be filled.", color = Ink2, style = MaterialTheme.typography.bodySmall)
            if (st.missingFields.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text("Assistant still missing: ${st.missingFields.joinToString()}", color = Amber, style = MaterialTheme.typography.bodySmall)
            }
            val need = d.missingMandatoryLabels()
            AnimatedVisibility(visible = need.isNotEmpty(), enter = fadeIn(tween(MotionMs)), exit = fadeOut(tween(MotionMs))) {
                Column {
                    Spacer(Modifier.height(4.dp))
                    Text("Required blank: ${need.joinToString()}", color = Danger, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(12.dp))
            EditField("Incident Type (required)", d.incidentType, true) { v -> vm.editDraft { r -> r.copy(incidentType = v.ifBlank { null }) } }
            EditField("Severity Level", d.severityLevel, false, single = true) { v -> vm.editDraft { r -> r.copy(severityLevel = v.ifBlank { null }) } }
            EditField("Incident Location (required)", d.incidentLocation, true) { v -> vm.editDraft { r -> r.copy(incidentLocation = v.ifBlank { null }) } }
            EditField("Date & Time (required)", d.dateTime, true) { v -> vm.editDraft { r -> r.copy(dateTime = v.ifBlank { null }) } }
            EditField("Persons Involved", d.personsInvolved, false) { v -> vm.editDraft { r -> r.copy(personsInvolved = v.ifBlank { null }) } }
            EditField("Equipment / Asset Involved", d.equipmentAssetInvolved, false) { v -> vm.editDraft { r -> r.copy(equipmentAssetInvolved = v.ifBlank { null }) } }
            EditField("Incident Description", d.incidentDescription, false, multi = true) { v -> vm.editDraft { r -> r.copy(incidentDescription = v.ifBlank { null }) } }
            EditField("Immediate Actions Taken", d.immediateActionsTaken, false, multi = true) { v -> vm.editDraft { r -> r.copy(immediateActionsTaken = v.ifBlank { null }) } }
            EditField("Witness Information", d.witnessInformation, false) { v -> vm.editDraft { r -> r.copy(witnessInformation = v.ifBlank { null }) } }
            EditField("Recommended Follow-Up Action", d.recommendedFollowUpAction, false) { v -> vm.editDraft { r -> r.copy(recommendedFollowUpAction = v.ifBlank { null }) } }
            Spacer(Modifier.height(12.dp))
            AnimatedVisibility(
                visible = st.stage == Stage.ERROR && st.errorCode == "REQUIRED_BLANK",
                enter = fadeIn(tween(MotionMs)), exit = fadeOut(tween(MotionMs))
            ) {
                Text(st.errorMessage ?: "", color = Danger, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { vm.submit() },
                enabled = !st.busy && d.isSubmittable(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, disabledContainerColor = Border)
            ) {
                if (d.isSubmittable() && !st.busy) {
                    Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    if (st.busy) "Saving…" else if (d.isSubmittable()) "Submit report" else "Fill required fields to submit",
                    style = MaterialTheme.typography.titleSmall
                )
            }
            Spacer(Modifier.height(16.dp))
            LaunchedEffect(st.stage) { if (st.stage == Stage.SUBMITTED) onSubmitted() }
        }
    }
}

@Composable
private fun EditField(label: String, value: String?, required: Boolean, single: Boolean = false, multi: Boolean = false, onChange: (String) -> Unit) {
    val blank = value.isNullOrBlank()
    Card(
        colors = CardDefaults.cardColors(containerColor = Surface),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (required && blank) Danger.copy(alpha = 0.45f) else Border),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(label, color = Ink2, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                if (required && blank) Text("Required", color = Danger, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = value ?: "", onValueChange = onChange, modifier = Modifier.fillMaxWidth(),
                singleLine = !multi, minLines = if (multi) 3 else 1,
                placeholder = { Text("Empty", color = Ink3) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Accent, cursorColor = Accent)
            )
        }
    }
}

@Composable
private fun SubmittedView(report: IncidentReport, id: Long?, onHome: () -> Unit) {
    ContentColumn {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(40.dp))
            Icon(Icons.Default.CheckCircle, null, tint = Mint, modifier = Modifier.size(72.dp))
            Spacer(Modifier.height(12.dp))
            Text("Report saved", color = Ink, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Local ID #${id ?: "-"} • stored offline", color = Ink2, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            Text("${report.incidentType ?: ""} • ${report.incidentLocation ?: ""} • ${report.dateTime ?: ""}", color = Ink, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(24.dp))
            Button(onClick = onHome, modifier = Modifier.fillMaxWidth().height(52.dp), colors = ButtonDefaults.buttonColors(containerColor = Accent)) {
                Text("Back to start", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SummaryRow(icon: ImageVector, label: String, value: String?) {
    Row(Modifier.padding(vertical = 2.dp)) {
        Icon(icon, null, tint = Accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text("$label: ${value ?: "—"}", color = Ink, style = MaterialTheme.typography.bodySmall)
    }
}
