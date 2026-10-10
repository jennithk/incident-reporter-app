package com.megathon.incidentreporter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.megathon.incidentreporter.audio.AudioRecorder
import com.megathon.incidentreporter.contract.*
import com.megathon.incidentreporter.data.ReportStore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

// SINGLE DI POINT: pass FakeAiEngine() for demo, production adapter for Module B.
// MainActivity wires the engine here; UI code never imports the concrete class.
class IncidentViewModel(
    private val engine: AIEngine,
    private val store: ReportStore,
    private val cacheDir: File
) : ViewModel() {

    private val _s = MutableStateFlow(IncidentUiState())
    val s: StateFlow<IncidentUiState> = _s.asStateFlow()
    private val recorder = AudioRecorder()
    private var pendingFollowUp = false
    private var recordFile: File? = null
    private var ticker: Job? = null

    fun newReport() {
        cleanupTmp()
        pendingFollowUp = false
        _s.value = IncidentUiState(stage = Stage.IDLE)
    }

    // ---- Recording (voice-first) ----
    fun startRecording(followUp: Boolean) {
        val st = _s.value
        if (st.busy || st.stage == Stage.SUBMITTING) return
        pendingFollowUp = followUp
        recordFile = File(cacheDir, (if (followUp) "followup_" else "incident_") + System.currentTimeMillis() + ".wav")
        if (!recorder.start()) {
            _s.update { it.copy(stage = Stage.ERROR, errorCode = AIEngineErrorCode.AUDIO_INVALID, errorMessage = userMessageFor(AIEngineErrorCode.AUDIO_INVALID)) }
            return
        }
        _s.update { it.copy(stage = Stage.LISTENING, isRecording = true, recordingSec = 0.0, errorCode = null, errorMessage = null) }
        ticker?.cancel()
        ticker = viewModelScope.launch {
            while (recorder.isRecording) {
                _s.update { it.copy(recordingSec = recorder.durationSec) }
                if (recorder.durationSec >= 60.0) { stopAndProcess(); break } // auto-stop
                delay(200)
            }
        }
    }

    fun discardRecording() {
        ticker?.cancel()
        recorder.discardTmp(recordFile)
        recordFile = null
        _s.update { it.copy(stage = if (pendingFollowUp) Stage.FOLLOW_UP else Stage.IDLE, isRecording = false, recordingSec = 0.0) }
    }

    fun stopAndProcess() {
        val st = _s.value
        if (st.busy || (!st.isRecording && st.stage != Stage.LISTENING)) return
        viewModelScope.launch {
            _s.update { it.copy(busy = true, isRecording = false, stage = Stage.TRANSCRIBING, processingLabel = "Transcribing voice…") }
            ticker?.cancel()
            try {
                val dest = recordFile ?: throw AIEngineException(AIEngineErrorCode.AUDIO_INVALID, "No recording")
                // Initial recordings require 5s; follow-up answers allow short natural replies.
                val minSec = if (pendingFollowUp) 1.0 else 2.0
                val wav = withContext(Dispatchers.IO) { recorder.stopToFile(dest, minSec) }
                val transcript = withContext(Dispatchers.IO) { engine.transcribe(wav) }
                if (transcript.isBlank()) throw AIEngineException(AIEngineErrorCode.STT_EMPTY, "Empty transcript")
                _s.update { it.copy(stage = Stage.EXTRACTING, processingLabel = "Extracting details…", lastTranscript = transcript, lastAudioPath = wav.absolutePath) }
                val current = _s.value.partial
                val result = withContext(Dispatchers.Default) { engine.extract(transcript, current) }
                val conv = _s.value.conversation +
                    ConversationEntry(Speaker.USER, result.transcript) +
                    (result.followUpQuestion?.let { listOf(ConversationEntry(Speaker.ASSISTANT, it)) } ?: emptyList())
                _s.update {
                    it.copy(
                        busy = false, partial = result.report, draft = result.report,
                        missingFields = result.missingFields, conversation = conv,
                        followUpQuestion = result.followUpQuestion,
                        stage = if (result.followUpQuestion != null) Stage.FOLLOW_UP else Stage.REVIEW,
                        processingLabel = null
                    )
                }
            } catch (e: AIEngineException) {
                cleanupTmp()
                _s.update { it.copy(busy = false, stage = Stage.ERROR, errorCode = e.code, errorMessage = userMessageFor(e.code), processingLabel = null) }
            } catch (_: Exception) {
                cleanupTmp()
                _s.update { it.copy(busy = false, stage = Stage.ERROR, errorCode = AIEngineErrorCode.EXECUTION_FAILED, errorMessage = userMessageFor(AIEngineErrorCode.EXECUTION_FAILED), processingLabel = null) }
            }
        }
    }

    // Optional typed fallback. Never required for the demo.
    fun answerWithText(text: String) {
        if (_s.value.busy || text.isBlank()) return
        viewModelScope.launch {
            _s.update { it.copy(busy = true, stage = Stage.EXTRACTING, processingLabel = "Extracting details…") }
            try {
                val result = withContext(Dispatchers.Default) { engine.extract(text.trim(), _s.value.partial) }
                val conv = _s.value.conversation +
                    ConversationEntry(Speaker.USER, text.trim()) +
                    (result.followUpQuestion?.let { listOf(ConversationEntry(Speaker.ASSISTANT, it)) } ?: emptyList())
                _s.update {
                    it.copy(busy = false, partial = result.report, draft = result.report, missingFields = result.missingFields,
                        conversation = conv, followUpQuestion = result.followUpQuestion, lastTranscript = text.trim(),
                        stage = if (result.followUpQuestion != null) Stage.FOLLOW_UP else Stage.REVIEW, processingLabel = null)
                }
            } catch (e: AIEngineException) {
                _s.update { it.copy(busy = false, stage = Stage.ERROR, errorCode = e.code, errorMessage = userMessageFor(e.code), processingLabel = null) }
            }
        }
    }

    fun retryFromError() {
        _s.update { it.copy(stage = if (it.followUpQuestion != null) Stage.FOLLOW_UP else if (it.conversation.isEmpty()) Stage.IDLE else Stage.FOLLOW_UP, errorCode = null, errorMessage = null, busy = false, processingLabel = null) }
    }

    fun goReview() {
        _s.update { it.copy(stage = Stage.REVIEW, draft = it.partial, errorCode = null, errorMessage = null) }
    }

    // Manual edits: preserved, no AI call on keystroke.
    fun editDraft(map: (IncidentReport) -> IncidentReport) {
        _s.update { it.copy(draft = map(it.draft)) }
    }

    fun submit() {
        val st = _s.value
        if (st.busy || st.stage == Stage.SUBMITTING) return
        val missing = st.draft.missingMandatoryLabels()
        if (missing.isNotEmpty()) {
            _s.update { it.copy(stage = Stage.ERROR, errorCode = "REQUIRED_BLANK", errorMessage = "Please fill required: ${missing.joinToString()}") }
            return
        }
        viewModelScope.launch {
            _s.update { it.copy(busy = true, stage = Stage.SUBMITTING) }
            try {
                val id = withContext(Dispatchers.IO) { store.save(st.draft, st.lastTranscript) }
                cleanupTmp()
                _s.update { it.copy(busy = false, savedId = id, stage = Stage.SUBMITTED) }
            } catch (_: Exception) {
                _s.update { it.copy(busy = false, stage = Stage.ERROR, errorCode = AIEngineErrorCode.EXECUTION_FAILED, errorMessage = "Could not save locally. Please retry.") }
            }
        }
    }

    private fun cleanupTmp() {
        try { recordFile?.let { if (it.exists()) it.delete() } } catch (_: Exception) { }
        recordFile = null
    }

    override fun onCleared() { cleanupTmp() }
}

class IncidentVmFactory(private val engine: AIEngine, private val store: ReportStore, private val cacheDir: File) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = IncidentViewModel(engine, store, cacheDir) as T
}
