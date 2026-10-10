package com.megathon.incidentreporter.ai

import com.megathon.incidentreporter.contract.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// Deterministic demo engine. No models, no network, no QAIRT/QNN imports.
// DEMO MODE — simulated AI responses.
class FakeAiEngine : AIEngine {

    companion object {
        const val INITIAL_TRANSCRIPT =
            "There was a water leak near Conference Room 2 around 10 AM. Maintenance has already isolated the area."
        const val FOLLOWUP_TRANSCRIPT = "The severity was high."
        const val SEVERITY_QUESTION = "What was the severity level of the water leak?"
    }

    // Returns canned transcript after validating the file exists and is non-empty.
    // Follow-up recordings are named followup_*.wav by the ViewModel.
    override suspend fun transcribe(audioFile: File): String = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L)
            throw AIEngineException(AIEngineErrorCode.AUDIO_INVALID, "Invalid audio file")
        if (audioFile.name.startsWith("followup_")) FOLLOWUP_TRANSCRIPT else INITIAL_TRANSCRIPT
    }

    override suspend fun extract(transcript: String, currentReport: IncidentReport): ExtractionResult =
        withContext(Dispatchers.Default) {
            if (transcript.isBlank())
                throw AIEngineException(AIEngineErrorCode.STT_EMPTY, "Empty transcript")
            val t = transcript.trim()
            // Follow-up answer: preserve everything, set severity High.
            if (t.contains("severity was high", ignoreCase = true)) {
                val updated = currentReport.copy(severityLevel = "High")
                return@withContext ExtractionResult(
                    report = updated,
                    missingFields = missingIds(updated).filter { it != ReportFields.SEVERITY_LEVEL },
                    followUpQuestion = null,
                    transcript = t
                )
            }
            // Initial incident: exactly 4 fields, preserve "Around 10 AM" verbatim.
            val base = IncidentReport(
                incidentType = currentReport.incidentType ?: "Water leak",
                severityLevel = currentReport.severityLevel,
                incidentLocation = currentReport.incidentLocation ?: "Conference Room 2",
                dateTime = currentReport.dateTime ?: "Around 10 AM",
                personsInvolved = currentReport.personsInvolved,
                equipmentAssetInvolved = currentReport.equipmentAssetInvolved,
                incidentDescription = currentReport.incidentDescription,
                immediateActionsTaken = currentReport.immediateActionsTaken
                    ?: "Maintenance has already isolated the area",
                witnessInformation = currentReport.witnessInformation,
                recommendedFollowUpAction = currentReport.recommendedFollowUpAction
            )
            ExtractionResult(
                report = base,
                missingFields = listOf(ReportFields.SEVERITY_LEVEL),
                followUpQuestion = SEVERITY_QUESTION,
                transcript = INITIAL_TRANSCRIPT
            )
        }

    private fun missingIds(r: IncidentReport): List<String> = buildList {
        if (r.incidentType.isNullOrBlank()) add(ReportFields.INCIDENT_TYPE)
        if (r.severityLevel.isNullOrBlank()) add(ReportFields.SEVERITY_LEVEL)
        if (r.incidentLocation.isNullOrBlank()) add(ReportFields.INCIDENT_LOCATION)
        if (r.dateTime.isNullOrBlank()) add(ReportFields.DATE_TIME)
        if (r.personsInvolved.isNullOrBlank()) add(ReportFields.PERSONS_INVOLVED)
        if (r.equipmentAssetInvolved.isNullOrBlank()) add(ReportFields.EQUIPMENT_ASSET_INVOLVED)
        if (r.incidentDescription.isNullOrBlank()) add(ReportFields.INCIDENT_DESCRIPTION)
        if (r.immediateActionsTaken.isNullOrBlank()) add(ReportFields.IMMEDIATE_ACTIONS_TAKEN)
        if (r.witnessInformation.isNullOrBlank()) add(ReportFields.WITNESS_INFORMATION)
        if (r.recommendedFollowUpAction.isNullOrBlank()) add(ReportFields.RECOMMENDED_FOLLOW_UP_ACTION)
    }
}
