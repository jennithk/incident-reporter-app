package com.megathon.incidentreporter.data

import com.megathon.incidentreporter.contract.IncidentReport

fun IncidentReport.toEntity(transcript: String?, now: Long): IncidentReportEntity = IncidentReportEntity(
    incidentType = incidentType,
    severityLevel = severityLevel,
    incidentLocation = incidentLocation,
    dateTime = dateTime,
    personsInvolved = personsInvolved,
    equipmentAssetInvolved = equipmentAssetInvolved,
    incidentDescription = incidentDescription,
    immediateActionsTaken = immediateActionsTaken,
    witnessInformation = witnessInformation,
    recommendedFollowUpAction = recommendedFollowUpAction,
    transcript = transcript,
    createdAt = now,
    submittedAt = now
)

fun IncidentReportEntity.toReport(): IncidentReport = IncidentReport(
    incidentType = incidentType,
    severityLevel = severityLevel,
    incidentLocation = incidentLocation,
    dateTime = dateTime,
    personsInvolved = personsInvolved,
    equipmentAssetInvolved = equipmentAssetInvolved,
    incidentDescription = incidentDescription,
    immediateActionsTaken = immediateActionsTaken,
    witnessInformation = witnessInformation,
    recommendedFollowUpAction = recommendedFollowUpAction
)

// Minimal store used by the ViewModel. No cloud, no sync.
class ReportStore(private val dao: IncidentReportDao) {
    suspend fun save(report: IncidentReport, transcript: String?): Long {
        val now = System.currentTimeMillis()
        return dao.insert(report.toEntity(transcript, now))
    }
    suspend fun list(): List<IncidentReportEntity> = dao.all()
    suspend fun get(id: Long): IncidentReportEntity? = dao.byId(id)
}
