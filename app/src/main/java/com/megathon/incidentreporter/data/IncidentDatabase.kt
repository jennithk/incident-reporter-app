package com.megathon.incidentreporter.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [IncidentReportEntity::class], version = 1, exportSchema = false)
abstract class IncidentDatabase : RoomDatabase() {
    abstract fun dao(): IncidentReportDao
}
