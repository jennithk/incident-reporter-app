package com.megathon.incidentreporter.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.megathon.incidentreporter.contract.AIEngineErrorCode
import com.megathon.incidentreporter.contract.AIEngineException
import kotlinx.coroutines.*
import java.io.ByteArrayOutputStream
import java.io.File

// Independent mic recorder. Returns a validated WAV file in app private cache.
// All capture + file IO runs off the main thread. Caller validates duration policy.
class AudioRecorder {
    private var record: AudioRecord? = null
    private var job: Job? = null
    private var startMs: Long = 0L
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Volatile var isRecording: Boolean = false
        private set
    @Volatile var durationSec: Double = 0.0
        private set

    fun start(): Boolean {
        if (isRecording) return true
        return try {
            val minBuf = AudioRecord.getMinBufferSize(
                WavWriter.SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            if (minBuf <= 0) return false
            val rec = AudioRecord(
                MediaRecorder.AudioSource.MIC, WavWriter.SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuf * 2
            )
            if (rec.state != AudioRecord.STATE_INITIALIZED) { rec.release(); return false }
            record = rec
            startMs = System.currentTimeMillis()
            isRecording = true
            durationSec = 0.0
            pcmBuffer = ByteArrayOutputStream()
            rec.startRecording()
            job = scope.launch {
                val buf = ByteArray(minBuf)
                while (isActive && isRecording) {
                    val n = try { rec.read(buf, 0, buf.size) } catch (_: Exception) { -1 }
                    if (n > 0) {
                        synchronized(this@AudioRecorder) { pcmBuffer?.write(buf, 0, n) }
                        durationSec = (System.currentTimeMillis() - startMs) / 1000.0
                        if (durationSec >= WavWriter.MAX_DURATION_SEC) break // auto-stop at 60s
                    } else if (n < 0) break
                }
            }
            true
        } catch (_: SecurityException) { isRecording = false; false }
        catch (_: Exception) { isRecording = false; false }
    }

    @Volatile private var pcmBuffer: ByteArrayOutputStream? = null

    // Stops capture, writes WAV, validates, returns file. Throws AUDIO_INVALID on failure.
    suspend fun stopToFile(dest: File, minDurationSec: Double = 1.0): File = withContext(Dispatchers.IO) {
        isRecording = false
        try { job?.cancelAndJoin() } catch (_: Exception) { }
        try { record?.stop() } catch (_: Exception) { }
        try { record?.release() } catch (_: Exception) { }
        record = null
        val pcm = synchronized(this@AudioRecorder) { pcmBuffer?.toByteArray() } ?: ByteArray(0)
        pcmBuffer = null
        if (pcm.isEmpty()) throw AIEngineException(AIEngineErrorCode.AUDIO_INVALID, "Empty recording")
        WavWriter.write(pcm, dest)
        if (!WavWriter.isValid(dest, minDurationSec)) {
            try { dest.delete() } catch (_: Exception) { }
            throw AIEngineException(AIEngineErrorCode.AUDIO_INVALID, "Recording too short or invalid")
        }
        dest
    }

    fun discardTmp(file: File?) {
        try { job?.cancel() } catch (_: Exception) { }
        try { record?.stop() } catch (_: Exception) { }
        try { record?.release() } catch (_: Exception) { }
        record = null; isRecording = false
        try { file?.delete() } catch (_: Exception) { }
    }
}
