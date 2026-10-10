package com.megathon.incidentreporter.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface IncidentReportDao {
    @Insert suspend fun insert(e: IncidentReportEntity): Long
    @Query("SELECT * FROM incident_reports ORDER BY submittedAt DESC") suspend fun all(): List<IncidentReportEntity>
    @Query("SELECT * FROM incident_reports WHERE id = :id") suspend fun byId(id: Long): IncidentReportEntity?
}
