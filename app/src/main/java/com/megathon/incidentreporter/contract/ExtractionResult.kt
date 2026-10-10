package com.megathon.incidentreporter.contract

data class ExtractionResult(
    val report: IncidentReport,
    val missingFields: List<String>,
    val followUpQuestion: String?,
    val transcript: String
)
