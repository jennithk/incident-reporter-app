# Team 1 — UI/App (Incident Reporter, QIDK)

## Goal
SPEAK → UNDERSTAND → EXTRACT → REVIEW → SUBMIT, all on-device.

## Open in Android Studio
1. Open folder `incident-reporter-app/` → Sync Gradle → Run on QIDK via ADB:
   `adb devices` → Run ▶ (or `adb install app-debug.apk`)
2. Grant mic permission. Pre-download offline speech pack for airplane-mode demo.

## 4 screens (NavHost in MainActivity.kt)
- `speak` → SpeakScreen: mic + Waveform + live transcript + typed fallback
- `chat` → ConversationScreen: User said / AI asked missing field
- `form` → ReportFormScreen: 10 editable fields, Submit disabled until mandatory present
- `done` → SubmittedScreen: success + offline TTS readback "Report created..."

## Contract with AI Team (Team 2)
Code ONLY against `ai/IncidentExtractor`:
```kotlin
suspend fun extract(transcript: String, current: IncidentReport): IncidentReport
fun nextMissingQuestion(report: IncidentReport): String?
```
- Today: `FakeOnDeviceExtractor` (regex demo, no cloud).
- AI team: add `QaIrtExtractor` (QAIRT SDK / Genie, Snapdragon NPU) + swap in `ReportViewModel()` — zero UI changes.
- Demo input: "There was a water leak near Conference Room 2 around 10 AM. Maintenance has already isolated the area."
  → Incident Type=Water Leak, Location=Conference Room 2, Time=10 AM, Action=Area Isolated.

## Storage
Room `reports.db` + `IncidentReport` (10 fields). History via `ReportDao.all()`.

## Judges checklist
- [ ] Airplane mode ON → mic → form fills → TTS speaks
- [ ] No cloud APIs (only Room + on-device STT/TTS + QAIRT model)
- [ ] Missing-field loop visible in Conversation screen
