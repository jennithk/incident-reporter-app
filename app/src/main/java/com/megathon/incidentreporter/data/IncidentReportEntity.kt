package com.megathon.incidentreporter.data

import androidx.room.Entity
import androidx.room.PrimaryKey

// Local-only entity. The shared contract IncidentReport stays Room-free.
@Entity(tableName = "incident_reports")
data class IncidentReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val incidentType: String?,
    val severityLevel: String?,
    val incidentLocation: String?,
    val dateTime: String?,
    val personsInvolved: String?,
    val equipmentAssetInvolved: String?,
    val incidentDescription: String?,
    val immediateActionsTaken: String?,
    val witnessInformation: String?,
    val recommendedFollowUpAction: String?,
    val transcript: String?,
    val createdAt: Long,
    val submittedAt: Long
)
