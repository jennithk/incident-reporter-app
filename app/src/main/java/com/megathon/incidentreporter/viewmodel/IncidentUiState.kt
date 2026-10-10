package com.megathon.incidentreporter.viewmodel

import com.megathon.incidentreporter.contract.IncidentReport

enum class Stage {
    IDLE, LISTENING, TRANSCRIBING, EXTRACTING, FOLLOW_UP, REVIEW, SUBMITTING, SUBMITTED, ERROR
}

enum class Speaker { USER, ASSISTANT }

data class ConversationEntry(
    val speaker: Speaker,
    val text: String,
    val timestampMs: Long = System.currentTimeMillis()
)

data class IncidentUiState(
    val stage: Stage = Stage.IDLE,
    val isRecording: Boolean = false,
    val recordingSec: Double = 0.0,
    val processingLabel: String? = null,
    val conversation: List<ConversationEntry> = emptyList(),
    val partial: IncidentReport = IncidentReport(),
    val missingFields: List<String> = emptyList(),
    val followUpQuestion: String? = null,
    val draft: IncidentReport = IncidentReport(), // editable copy for review screen
    val lastTranscript: String? = null,
    val lastAudioPath: String? = null,
    val savedId: Long? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    val busy: Boolean = false // guards duplicate AI ops / double submit
)
