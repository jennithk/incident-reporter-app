package com.megathon.incidentreporter.contract

// Shared AI-facing model. Independent of Compose, Room, ViewModel, QAIRT.
// Null = not supplied/extracted. UI renders null as empty, never "null".
data class IncidentReport(
    val incidentType: String? = null,
    val severityLevel: String? = null,
    val incidentLocation: String? = null,
    val dateTime: String? = null,
    val personsInvolved: String? = null,
    val equipmentAssetInvolved: String? = null,
    val incidentDescription: String? = null,
    val immediateActionsTaken: String? = null,
    val witnessInformation: String? = null,
    val recommendedFollowUpAction: String? = null
) {
    /** UI-only check of the 3 mandatory fields. No semantic extraction here. */
    fun missingMandatoryLabels(): List<String> = buildList {
        if (incidentType.isNullOrBlank()) add("Incident Type")
        if (incidentLocation.isNullOrBlank()) add("Incident Location")
        if (dateTime.isNullOrBlank()) add("Date & Time")
    }

    fun isSubmittable(): Boolean = missingMandatoryLabels().isEmpty()
}

// Stable machine-readable identifiers for ExtractionResult.missingFields.
object ReportFields {
    const val INCIDENT_TYPE = "incident_type"
    const val SEVERITY_LEVEL = "severity_level"
    const val INCIDENT_LOCATION = "incident_location"
    const val DATE_TIME = "date_time"
    const val PERSONS_INVOLVED = "persons_involved"
    const val EQUIPMENT_ASSET_INVOLVED = "equipment_asset_involved"
    const val INCIDENT_DESCRIPTION = "incident_description"
    const val IMMEDIATE_ACTIONS_TAKEN = "immediate_actions_taken"
    const val WITNESS_INFORMATION = "witness_information"
    const val RECOMMENDED_FOLLOW_UP_ACTION = "recommended_follow_up_action"

    val ALL = listOf(
        INCIDENT_TYPE, SEVERITY_LEVEL, INCIDENT_LOCATION, DATE_TIME,
        PERSONS_INVOLVED, EQUIPMENT_ASSET_INVOLVED, INCIDENT_DESCRIPTION,
        IMMEDIATE_ACTIONS_TAKEN, WITNESS_INFORMATION, RECOMMENDED_FOLLOW_UP_ACTION
    )
}
