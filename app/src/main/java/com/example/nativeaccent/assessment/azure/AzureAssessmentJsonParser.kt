package com.example.nativeaccent.assessment.azure

import com.example.nativeaccent.assessment.AssessmentError
import com.example.nativeaccent.assessment.AssessmentOutcome
import com.example.nativeaccent.assessment.PhonemeResult
import com.example.nativeaccent.assessment.PronunciationResult
import com.example.nativeaccent.assessment.WordErrorType
import com.example.nativeaccent.assessment.WordResult
import org.json.JSONException
import org.json.JSONObject
import kotlin.math.roundToInt

/**
 * Turns Azure's detailed recognition JSON into a [PronunciationResult].
 *
 * Shape (abridged):
 * ```
 * { "RecognitionStatus": "Success",
 *   "NBest": [{
 *     "PronunciationAssessment": { "AccuracyScore", "FluencyScore", "CompletenessScore", "PronScore" },
 *     "Words": [{
 *       "Word": "coffee",
 *       "PronunciationAssessment": { "AccuracyScore": 91.0, "ErrorType": "None" },
 *       "Phonemes": [{ "Phoneme": "k", "PronunciationAssessment": { "AccuracyScore": 100.0 } }, ...]
 *     }, ...]
 *   }] }
 * ```
 * Older service versions put `AccuracyScore` / `ErrorType` directly on the word
 * or phoneme rather than under `PronunciationAssessment`; both are accepted.
 */
object AzureAssessmentJsonParser {

    private const val NO_SPEECH_MESSAGE =
        "We couldn't hear the sentence in that take. Try again, a little closer to the mic."

    fun parse(json: String?): AssessmentOutcome {
        if (json.isNullOrBlank()) {
            return AssessmentOutcome.Failed(AssessmentError.UNKNOWN, "empty response")
        }
        return try {
            parseOrThrow(JSONObject(json))
        } catch (e: JSONException) {
            AssessmentOutcome.Failed(AssessmentError.UNKNOWN, "unreadable response: ${e.message}")
        }
    }

    private fun parseOrThrow(root: JSONObject): AssessmentOutcome {
        val status = root.optString("RecognitionStatus", "Success")
        if (status != "Success") return AssessmentOutcome.NoSpeech(NO_SPEECH_MESSAGE)

        val best = root.optJSONArray("NBest")?.optJSONObject(0)
            ?: return AssessmentOutcome.NoSpeech(NO_SPEECH_MESSAGE)

        val words = best.optJSONArray("Words")?.let { array ->
            (0 until array.length()).mapNotNull { array.optJSONObject(it)?.let(::parseWord) }
        }.orEmpty()

        val spokenReferenceWords = words.filter {
            it.errorType != WordErrorType.INSERTION && it.errorType != WordErrorType.OMISSION
        }
        if (spokenReferenceWords.isEmpty()) return AssessmentOutcome.NoSpeech(NO_SPEECH_MESSAGE)

        val scores = best.optJSONObject("PronunciationAssessment") ?: best
        val accuracy = scores.score("AccuracyScore")
        return AssessmentOutcome.Scored(
            PronunciationResult(
                // PronScore is Azure's weighted overall; fall back to accuracy if absent.
                overallScore = scores.scoreOrNull("PronScore") ?: accuracy,
                accuracyScore = accuracy,
                fluencyScore = scores.score("FluencyScore"),
                completenessScore = scores.score("CompletenessScore"),
                words = words,
                recognizedText = best.optString("Display", root.optString("DisplayText")),
            )
        )
    }

    private fun parseWord(word: JSONObject): WordResult {
        val assessment = word.optJSONObject("PronunciationAssessment") ?: word
        val phonemes = word.optJSONArray("Phonemes")?.let { array ->
            (0 until array.length()).mapNotNull { index ->
                val phoneme = array.optJSONObject(index) ?: return@mapNotNull null
                val phonemeScores = phoneme.optJSONObject("PronunciationAssessment") ?: phoneme
                PhonemeResult(
                    phoneme = phoneme.optString("Phoneme"),
                    accuracyScore = phonemeScores.score("AccuracyScore"),
                )
            }
        }.orEmpty()

        return WordResult(
            word = word.optString("Word"),
            accuracyScore = assessment.score("AccuracyScore"),
            phonemes = phonemes,
            errorType = WordErrorType.fromAzure(assessment.optString("ErrorType", "None")),
        )
    }

    private fun JSONObject.scoreOrNull(key: String): Int? {
        val value = optDouble(key)
        return if (value.isNaN()) null else value.roundToInt().coerceIn(0, 100)
    }

    private fun JSONObject.score(key: String): Int = scoreOrNull(key) ?: 0
}
