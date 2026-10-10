package com.megathon.incidentreporter.audio

import java.io.File
import java.io.FileOutputStream

// WAV container: RIFF/WAVE, 16kHz, mono, signed 16-bit PCM.
object WavWriter {
    const val SAMPLE_RATE = 16000
    const val CHANNELS = 1
    const val BITS_PER_SAMPLE = 16
    const val MAX_DURATION_SEC = 60

    fun write(pcmBytes: ByteArray, wavFile: File) {
        FileOutputStream(wavFile).use { f ->
            fun wstr(s: String) { f.write(s.toByteArray()) }
            fun w32(v: Int) {
                f.write(byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte(), ((v shr 16) and 0xFF).toByte(), ((v shr 24) and 0xFF).toByte()))
            }
            fun w16(v: Int) { f.write(byteArrayOf((v and 0xFF).toByte(), ((v shr 8) and 0xFF).toByte())) }
            val byteRate = SAMPLE_RATE * CHANNELS * BITS_PER_SAMPLE / 8
            wstr("RIFF"); w32(36 + pcmBytes.size); wstr("WAVE")
            wstr("fmt "); w32(16); w16(1); w16(CHANNELS); w32(SAMPLE_RATE); w32(byteRate); w16(CHANNELS * BITS_PER_SAMPLE / 8); w16(BITS_PER_SAMPLE)
            wstr("data"); w32(pcmBytes.size); f.write(pcmBytes)
        }
    }

    data class Info(val sampleRate: Int, val channels: Int, val bits: Int, val durationSec: Double)

    fun readInfo(wav: File): Info? {
        return try {
            val b = wav.readBytes()
            if (b.size < 44) return null
            fun strAt(o: Int, n: Int) = b.slice(o until o + n).map { it.toInt().toChar() }.joinToString("")
            if (strAt(0, 4) != "RIFF" || strAt(8, 4) != "WAVE" || strAt(12, 4) != "fmt ") return null
            fun i16(o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8)
            fun i32(o: Int) = (b[o].toInt() and 0xFF) or ((b[o + 1].toInt() and 0xFF) shl 8) or ((b[o + 2].toInt() and 0xFF) shl 16) or ((b[o + 3].toInt() and 0xFF) shl 24)
            val channels = i16(22); val sr = i32(24); val bits = i16(34); val dataSize = i32(40)
            val dur = dataSize.toDouble() / (sr * channels * bits / 8)
            Info(sr, channels, bits, dur)
        } catch (_: Exception) { null }
    }

    fun isValid(wav: File, minDurationSec: Double = 1.0): Boolean {
        val info = readInfo(wav) ?: return false
        if (info.sampleRate != SAMPLE_RATE || info.channels != CHANNELS || info.bits != BITS_PER_SAMPLE) return false
        if (info.durationSec < minDurationSec) return false
        if (info.durationSec > MAX_DURATION_SEC + 1) return false
        return true
    }
}
