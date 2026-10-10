package com.megathon.incidentreporter.contract

// Stable error codes shared by UI + AI bridge. Never silently convert to success.
object AIEngineErrorCode {
    const val STT_EMPTY = "STT_EMPTY"
    const val LLM_INVALID_JSON = "LLM_INVALID_JSON"
    const val MODEL_TIMEOUT = "MODEL_TIMEOUT"
    const val AUDIO_INVALID = "AUDIO_INVALID"
    const val MODEL_NOT_READY = "MODEL_NOT_READY"
    const val EXECUTION_FAILED = "EXECUTION_FAILED"
    const val CANCELLED = "CANCELLED"
}

class AIEngineException(val code: String, message: String, cause: Throwable? = null) : Exception(message, cause)

fun userMessageFor(code: String): String = when (code) {
    AIEngineErrorCode.STT_EMPTY -> "No speech was recognized. Please try recording again."
    AIEngineErrorCode.AUDIO_INVALID -> "The recording was too short or invalid. Please record again."
    AIEngineErrorCode.MODEL_TIMEOUT -> "The AI took too long. Please retry."
    AIEngineErrorCode.MODEL_NOT_READY -> "The on-device model is not ready yet. Please retry in a moment."
    AIEngineErrorCode.LLM_INVALID_JSON -> "The AI returned an unreadable result. Please retry."
    AIEngineErrorCode.CANCELLED -> "Cancelled."
    else -> "Something went wrong. Please retry."
}
