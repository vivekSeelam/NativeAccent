package com.example.nativeaccent.assessment.azure

import android.util.Log
import com.example.nativeaccent.assessment.AssessmentError
import com.example.nativeaccent.assessment.AssessmentOutcome
import com.example.nativeaccent.assessment.PronunciationAssessmentService
import com.example.nativeaccent.assessment.SpeechCredentials
import com.example.nativeaccent.assessment.SpeechCredentialsProvider
import com.microsoft.cognitiveservices.speech.CancellationErrorCode
import com.microsoft.cognitiveservices.speech.CancellationReason
import com.microsoft.cognitiveservices.speech.CancellationDetails
import com.microsoft.cognitiveservices.speech.PronunciationAssessmentConfig
import com.microsoft.cognitiveservices.speech.PropertyId
import com.microsoft.cognitiveservices.speech.ResultReason
import com.microsoft.cognitiveservices.speech.SpeechConfig
import com.microsoft.cognitiveservices.speech.SpeechRecognitionResult
import com.microsoft.cognitiveservices.speech.SpeechRecognizer
import com.microsoft.cognitiveservices.speech.audio.AudioConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.io.File

/**
 * Pronunciation assessment backed by the Azure Speech SDK.
 *
 * Every call builds and closes its own SDK objects — they wrap native handles
 * and are cheap next to the network round trip — so there is nothing to release
 * when the app goes away.
 */
class AzurePronunciationAssessmentService(
    private val credentialsProvider: SpeechCredentialsProvider,
    private val language: String = "en-US",
    private val timeoutMs: Long = 30_000L,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) : PronunciationAssessmentService {

    override suspend fun assess(audioFilePath: String, referenceText: String): AssessmentOutcome {
        val credentials = credentialsProvider.credentials()
            ?: return AssessmentOutcome.Failed(AssessmentError.NOT_CONFIGURED)

        val audio = File(audioFilePath)
        if (!audio.isFile || audio.length() <= HEADER_ONLY_WAV_BYTES) {
            return AssessmentOutcome.Failed(AssessmentError.BAD_AUDIO, "missing or empty: $audioFilePath")
        }

        return try {
            withTimeout(timeoutMs) {
                // recognizeOnceAsync().get() blocks; runInterruptible lets cancellation
                // (the learner re-records or leaves) interrupt the wait.
                runInterruptible(ioDispatcher) { recognize(credentials, audioFilePath, referenceText) }
            }
        } catch (e: TimeoutCancellationException) {
            AssessmentOutcome.Failed(AssessmentError.TIMEOUT)
        } catch (e: CancellationException) {
            throw e
        } catch (e: LinkageError) {
            // The SDK ships native code for arm64-v8a, armeabi-v7a and x86_64 only.
            Log.e(TAG, "Speech SDK native library unavailable", e)
            AssessmentOutcome.Failed(AssessmentError.UNSUPPORTED_DEVICE, e.message)
        } catch (e: Exception) {
            Log.e(TAG, "assessment failed", e)
            AssessmentOutcome.Failed(AssessmentError.UNKNOWN, e.message)
        }
    }

    private fun recognize(
        credentials: SpeechCredentials,
        audioFilePath: String,
        referenceText: String,
    ): AssessmentOutcome =
        speechConfigFor(credentials).use { speechConfig ->
            speechConfig.setSpeechRecognitionLanguage(language)
            // Learners pause mid-sentence; don't end the utterance on a short gap.
            speechConfig.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, SEGMENTATION_SILENCE_MS)

            AudioConfig.fromWavFileInput(audioFilePath).use { audioConfig ->
                SpeechRecognizer(speechConfig, audioConfig).use { recognizer ->
                    PronunciationAssessmentConfig.fromJson(assessmentJson(referenceText)).use { assessmentConfig ->
                        assessmentConfig.applyTo(recognizer)
                        val result = recognizer.recognizeOnceAsync().get()
                        try {
                            interpret(result)
                        } finally {
                            result.close()
                        }
                    }
                }
            }
        }

    /**
     * HundredMark grading, phoneme granularity and miscue detection, as the
     * constructor would set — plus an explicit "Comprehensive" dimension. With
     * "Basic", the service returns accuracy only: no fluency, completeness,
     * overall PronScore or omission/insertion flags. (Verified against the live
     * REST endpoint.) Stating it here removes any reliance on the SDK default.
     */
    private fun assessmentJson(referenceText: String): String = JSONObject()
        .put("referenceText", referenceText)
        .put("gradingSystem", "HundredMark")
        .put("granularity", "Phoneme")
        .put("dimension", "Comprehensive")
        .put("enableMiscue", true)
        .toString()

    private fun speechConfigFor(credentials: SpeechCredentials): SpeechConfig = when (credentials) {
        is SpeechCredentials.SubscriptionKey ->
            SpeechConfig.fromSubscription(credentials.key, credentials.region)
        is SpeechCredentials.AuthorizationToken ->
            SpeechConfig.fromAuthorizationToken(credentials.token, credentials.region)
    }

    private fun interpret(result: SpeechRecognitionResult): AssessmentOutcome = when (result.reason) {
        ResultReason.RecognizedSpeech ->
            AzureAssessmentJsonParser.parse(
                result.properties.getProperty(PropertyId.SpeechServiceResponse_JsonResult)
            )

        ResultReason.NoMatch ->
            AssessmentOutcome.NoSpeech("We couldn't make out any speech. Try again, a little louder.")

        ResultReason.Canceled -> {
            // Has close() but isn't AutoCloseable, so no use {}.
            val details = CancellationDetails.fromResult(result)
            try {
                interpretCancellation(details)
            } finally {
                details.close()
            }
        }

        else -> AssessmentOutcome.Failed(AssessmentError.UNKNOWN, "unexpected result: ${result.reason}")
    }

    private fun interpretCancellation(details: CancellationDetails): AssessmentOutcome {
        // With file input, reaching the end of the file without a result just
        // means no speech was found.
        if (details.reason == CancellationReason.EndOfStream) {
            return AssessmentOutcome.NoSpeech("We couldn't hear the sentence in that take. Try again.")
        }
        Log.w(TAG, "canceled: ${details.errorCode} ${details.errorDetails}")
        val error = when (details.errorCode) {
            CancellationErrorCode.ConnectionFailure,
            CancellationErrorCode.ServiceUnavailable,
            CancellationErrorCode.ServiceRedirectTemporary,
            CancellationErrorCode.ServiceRedirectPermanent -> AssessmentError.NETWORK

            CancellationErrorCode.AuthenticationFailure,
            CancellationErrorCode.Forbidden -> AssessmentError.AUTH

            CancellationErrorCode.TooManyRequests -> AssessmentError.QUOTA
            CancellationErrorCode.ServiceTimeout -> AssessmentError.TIMEOUT
            CancellationErrorCode.BadRequest -> AssessmentError.BAD_AUDIO
            else -> AssessmentError.UNKNOWN
        }
        return AssessmentOutcome.Failed(error, details.errorDetails)
    }

    private companion object {
        const val TAG = "AzureAssessment"
        const val SEGMENTATION_SILENCE_MS = "1500"

        /** A WAV with only its 44-byte header has no audio worth sending. */
        const val HEADER_ONLY_WAV_BYTES = 44L
    }
}
