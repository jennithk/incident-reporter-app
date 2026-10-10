package com.megathon.incidentreporter.contract

import java.io.File

// Single shared interface. FakeAiEngine and the production (Module B) adapter
// implement exactly this. UI depends only on this interface.
interface AIEngine {
    suspend fun transcribe(audioFile: File): String
    suspend fun extract(transcript: String, currentReport: IncidentReport): ExtractionResult
}
