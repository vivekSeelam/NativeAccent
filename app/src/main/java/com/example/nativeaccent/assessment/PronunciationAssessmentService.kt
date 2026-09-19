package com.example.nativeaccent.assessment

/**
 * Scores a recorded attempt against the sentence the learner was asked to say.
 *
 * The ViewModel only ever sees this interface. [com.example.nativeaccent.assessment.azure.AzurePronunciationAssessmentService]
 * is the production implementation; tests and previews can supply their own.
 */
interface PronunciationAssessmentService {
    /** [audioFilePath] must be a 16 kHz mono 16-bit PCM WAV file. */
    suspend fun assess(audioFilePath: String, referenceText: String): AssessmentOutcome
}

sealed interface AssessmentOutcome {
    data class Scored(val result: PronunciationResult) : AssessmentOutcome

    /**
     * The service could not find the sentence in the audio — silence, mumbling,
     * or something else entirely. The fix is a new take, not a retry.
     */
    data class NoSpeech(val message: String) : AssessmentOutcome

    data class Failed(val error: AssessmentError, val detail: String? = null) : AssessmentOutcome
}

/**
 * Why scoring failed, phrased for the learner. [retryable] means sending the
 * same recording again might work; otherwise they need a new take or a fix.
 */
enum class AssessmentError(val userMessage: String, val retryable: Boolean) {
    NOT_CONFIGURED(
        "Scoring isn't set up in this build. Add AZURE_SPEECH_KEY and AZURE_SPEECH_REGION to local.properties.",
        retryable = false,
    ),
    NETWORK("Couldn't reach the scoring service. Check your connection and try again.", retryable = true),
    AUTH("The scoring service rejected this app's credentials.", retryable = false),
    QUOTA("The scoring service is busy right now. Wait a moment and try again.", retryable = true),
    TIMEOUT("Scoring took too long. Try again.", retryable = true),
    BAD_AUDIO("That recording couldn't be read. Please record it again.", retryable = false),
    UNSUPPORTED_DEVICE("Scoring isn't supported on this device.", retryable = false),
    UNKNOWN("Something went wrong while scoring. Try again.", retryable = true),
}
