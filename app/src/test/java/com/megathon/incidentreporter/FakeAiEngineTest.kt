package com.megathon.incidentreporter

import com.megathon.incidentreporter.ai.FakeAiEngine
import com.megathon.incidentreporter.contract.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class FakeAiEngineTest {
    private val engine = FakeAiEngine()

    @Test fun `transcribe returns exact water-leak transcript`() = runTest {
        val f = File.createTempFile("incident_", ".wav").apply { writeBytes(ByteArray(100)) }
        assertEquals(FakeAiEngine.INITIAL_TRANSCRIPT, engine.transcribe(f))
        f.delete()
    }

    @Test fun `followup file returns severity answer transcript`() = runTest {
        val dir = System.getProperty("java.io.tmpdir")!!
        val f = File(dir, "followup_${System.currentTimeMillis()}.wav").apply { writeBytes(ByteArray(100)) }
        assertEquals(FakeAiEngine.FOLLOWUP_TRANSCRIPT, engine.transcribe(f))
        f.delete()
    }

    @Test fun `initial extraction populates exactly four fields`() = runTest {
        val res = engine.extract(FakeAiEngine.INITIAL_TRANSCRIPT, IncidentReport())
        assertEquals("Water leak", res.report.incidentType)
        assertEquals("Conference Room 2", res.report.incidentLocation)
        assertEquals("Around 10 AM", res.report.dateTime) // no fabrication
        assertEquals("Maintenance has already isolated the area", res.report.immediateActionsTaken)
        assertNull(res.report.severityLevel)
        assertNull(res.report.personsInvolved)
        assertNull(res.report.equipmentAssetInvolved)
        assertNull(res.report.incidentDescription)
        assertNull(res.report.witnessInformation)
        assertNull(res.report.recommendedFollowUpAction)
        assertEquals(listOf(ReportFields.SEVERITY_LEVEL), res.missingFields)
        assertEquals(FakeAiEngine.SEVERITY_QUESTION, res.followUpQuestion)
    }

    @Test fun `severity answer preserves previous values`() = runTest {
        val first = engine.extract(FakeAiEngine.INITIAL_TRANSCRIPT, IncidentReport())
        val second = engine.extract("The severity was high.", first.report)
        assertEquals("High", second.report.severityLevel)
        assertEquals("Water leak", second.report.incidentType)
        assertEquals("Conference Room 2", second.report.incidentLocation)
        assertEquals("Around 10 AM", second.report.dateTime)
        assertEquals("Maintenance has already isolated the area", second.report.immediateActionsTaken)
        assertNull(second.followUpQuestion)
        assertFalse(second.missingFields.contains(ReportFields.SEVERITY_LEVEL))
    }

    @Test fun `missingFields use snake_case identifiers`() = runTest {
        val res = engine.extract(FakeAiEngine.INITIAL_TRANSCRIPT, IncidentReport())
        assertTrue(res.missingFields.all { it in ReportFields.ALL && !it.contains(" ") && it == it.lowercase() })
    }
}
