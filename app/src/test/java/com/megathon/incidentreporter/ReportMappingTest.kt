package com.megathon.incidentreporter

import com.megathon.incidentreporter.contract.IncidentReport
import com.megathon.incidentreporter.data.toEntity
import com.megathon.incidentreporter.data.toReport
import org.junit.Assert.*
import org.junit.Test

class ReportMappingTest {
    @Test fun `all ten fields survive entity mapping`() {
        val r = IncidentReport(
            "Water leak", "High", "Conference Room 2", "Around 10 AM", "Maintenance",
            "Pipe", "Leak desc", "Isolated", "Guard", "Inspect"
        )
        val e = r.toEntity("transcript", 123L)
        val back = e.toReport()
        assertEquals(r, back)
        assertEquals("transcript", e.transcript)
    }

    @Test fun `nulls stay null, mandatory check works`() {
        val r = IncidentReport()
        assertEquals(3, r.missingMandatoryLabels().size)
        val e = r.toEntity(null, 1L).toReport()
        assertNull(e.incidentType)
    }
}
