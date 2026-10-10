# AI Integration Handoff (Module B — On-Device AI team)

## Implement this interface (no UI imports)
`com.megathon.incidentreporter.contract.AIEngine`:
```kotlin
suspend fun transcribe(audioFile: File): String
suspend fun extract(transcript: String, currentReport: IncidentReport): ExtractionResult
```

## Input guarantees from UI
- `audioFile`: valid WAV (RIFF/WAVE, 16kHz, mono, 16-bit PCM), stored in app cache; initial ≥2s, follow-ups ≥1s. Never empty/invalid (validated pre-call).
- `transcript` + `currentReport` (partial preserved by UI; merge new fields, never drop old ones).
- Called off-main; add your own timeout/cancellation; map failures to codes: `STT_EMPTY, LLM_INVALID_JSON, MODEL_TIMEOUT, AUDIO_INVALID, MODEL_NOT_READY, EXECUTION_FAILED, CANCELLED`.

## Return contract
- Always return best partial `report` (10 nullable fields; null = unknown; preserve "Around 10 AM" verbatim, no date fabrication).
- `missingFields`: snake_case ids only (see UI_MODULE_HANDOFF).
- `followUpQuestion`: single next question or null when ready for review. UI displays + speaks it, then records the answer and re-calls `extract` with the same partial.
- Never return success on error; never import QAIRT/QNN into UI module.

## Swap-in
One line in `MainActivity.engine()`: replace `FakeAiEngine()` with your adapter (background execution, path translation, timeouts, error mapping). Keep adapter in your own package; depend only on `contract/`.

## Must verify on Snapdragon (QIDK)
Whisper transcription of real mic WAVs, LLM extraction accuracy, mandatory-field behavior, airplane-mode run, latency, and that demo banner is removed/disabled when the real engine is active.
