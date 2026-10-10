package com.megathon.incidentreporter.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.megathon.incidentreporter.ui.components.ContentColumn
import com.megathon.incidentreporter.ui.components.DemoBanner
import com.megathon.incidentreporter.ui.theme.*
import com.megathon.incidentreporter.viewmodel.IncidentViewModel

// Screen A: Start / New Report
@Composable
fun StartScreen(vm: IncidentViewModel, onNew: () -> Unit, onHistory: () -> Unit) {
    vm.newReport()
    ContentColumn {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.Start
        ) {
            Spacer(Modifier.height(32.dp))
            Text(
                "Megathon X • Qualcomm",
                color = Navy,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Text("AI Incident Report Assistant", color = Ink, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            DemoBanner()
            Spacer(Modifier.height(20.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Surface),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Border),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    "Report an incident by speaking. The app records your description, transcribes it, extracts the report fields, and asks for anything missing before you review and submit. Runs on-device.",
                    color = Ink2, style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(Modifier.weight(1f, fill = true))
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onNew,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Accent)
            ) {
                Icon(Icons.Default.Mic, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("New Incident Report", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onHistory,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Ink)
            ) {
                Icon(Icons.Default.History, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("View submitted reports", style = MaterialTheme.typography.titleSmall)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Preview(showBackground = true) @Composable fun StartPreview() { MaterialTheme { } }
