# UI Module Handoff (Module A — owned by UI/App team)

## What was built
Voice-first incident app: Start → Conversation (record/transcribe/extract/follow-up loop) → Review (10 editable fields) → Submit → History. Fake engine drives the full water-leak demo offline.

## Structure
- `contract/`: `IncidentReport.kt` (10 nullable fields), `ExtractionResult.kt`, `AIEngine.kt`, `AIEngineErrorCode.kt`
- `audio/`: `AudioRecorder.kt` (AudioRecord 16k/mono/16-bit, 60s max, duration, validation, cleanup), `WavWriter.kt` (header + validator), `TtsPlayer.kt` (offline voice check, no overlap, replay)
- `ai/FakeAiEngine.kt`: deterministic demo fixture only
- `data/`: `IncidentReportEntity.kt`, `IncidentReportDao.kt`, `IncidentDatabase.kt`, `ReportStore.kt` (mapper keeps contract Room-free)
- `viewmodel/`: `IncidentUiState.kt` (IDLE…ERROR), `IncidentViewModel.kt`
- `ui/`: `StartScreen.kt`, `ConversationScreen.kt`, `ReportReviewScreen.kt` (review+submit), `ReportHistoryScreen.kt`, `components/`, `theme/`

## Contract recap
- `transcribe(audioFile: File): String`, `extract(transcript, currentReport): ExtractionResult`
- missingFields ids: `incident_type, severity_level, incident_location, date_time, persons_involved, equipment_asset_involved, incident_description, immediate_actions_taken, witness_information, recommended_follow_up_action`
- WAV: RIFF/WAVE, 16000 Hz, mono, 16-bit; initial 2s+ enforced in VM (5–60s policy), follow-ups 1s+
- Mandatory for submit: Incident Type, Incident Location, Date & Time (checked on draft, not stale AI list)
- Errors: `STT_EMPTY, LLM_INVALID_JSON, MODEL_TIMEOUT, AUDIO_INVALID, MODEL_NOT_READY, EXECUTION_FAILED, CANCELLED` (+ UI-only `REQUIRED_BLANK`)

## Water-leak fixture
Initial transcript → 4 fields (Water leak / Conference Room 2 / Around 10 AM verbatim / isolated area), missing `[severity_level]`, Q: "What was the severity level of the water leak?". File named `followup_*.wav` transcribes to "The severity was high." → severity `High`, preserved rest, no follow-up.

## DI point
`MainActivity.engine()`: return `FakeAiEngine()` now; production adapter later. UI only calls the two interface methods, off-main.

## Build & test
- `./gradlew :app:assembleDebug`
- `./gradlew :app:testDebugUnitTest` (FakeAiEngineTest, WavWriterTest, ReportMappingTest)
- Manifest: only `RECORD_AUDIO` (+MODIFY_AUDIO_SETTINGS); no INTERNET.
- Banner: "DEMO MODE — simulated AI responses" shown on Start + Conversation.

## Verified / not verified
- Unit tests + assembleDebug verified here. Device mic/TTS/Room-relaunch need QIDK run.
