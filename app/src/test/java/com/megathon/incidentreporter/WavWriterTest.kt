package com.megathon.incidentreporter

import com.megathon.incidentreporter.audio.WavWriter
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class WavWriterTest {
    @Test fun `wav header valid with correct metadata`() {
        val pcm = ByteArray(16000 * 2) // 1s of silence
        val wav = File.createTempFile("wav_", ".wav")
        WavWriter.write(pcm, wav)
        val info = WavWriter.readInfo(wav)!!
        assertEquals(16000, info.sampleRate)
        assertEquals(1, info.channels)
        assertEquals(16, info.bits)
        assertEquals(1.0, info.durationSec, 0.05)
        assertTrue(WavWriter.isValid(wav, 0.5))
        wav.delete()
    }

    @Test fun `short recording rejected`() {
        val pcm = ByteArray(1600) // 0.05s
        val wav = File.createTempFile("wav_", ".wav")
        WavWriter.write(pcm, wav)
        assertFalse(WavWriter.isValid(wav, 1.0))
        wav.delete()
    }

    @Test fun `garbage file rejected`() {
        val f = File.createTempFile("wav_", ".wav").apply { writeBytes(ByteArray(100)) }
        assertFalse(WavWriter.isValid(f))
        f.delete()
    }
}

