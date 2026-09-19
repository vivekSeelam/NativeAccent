package com.example.nativeaccent.assessment

import com.example.nativeaccent.assessment.azure.AzureAssessmentJsonParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AzureAssessmentJsonParserTest {

    /** Hand-written sample in the nested layout (scores under PronunciationAssessment). */
    private val scoredJson = """
        {
          "RecognitionStatus": "Success",
          "DisplayText": "I would like a cup of coffee.",
          "NBest": [{
            "Confidence": 0.93,
            "Display": "I would like a cup of coffee.",
            "PronunciationAssessment": {
              "AccuracyScore": 86.4, "FluencyScore": 91.0, "CompletenessScore": 100.0, "PronScore": 88.6
            },
            "Words": [
              { "Word": "i", "PronunciationAssessment": { "AccuracyScore": 100.0, "ErrorType": "None" },
                "Phonemes": [ { "Phoneme": "ay", "PronunciationAssessment": { "AccuracyScore": 100.0 } } ] },
              { "Word": "would", "PronunciationAssessment": { "AccuracyScore": 95.0, "ErrorType": "None" },
                "Phonemes": [ { "Phoneme": "w", "PronunciationAssessment": { "AccuracyScore": 98.0 } },
                              { "Phoneme": "uh", "PronunciationAssessment": { "AccuracyScore": 94.0 } },
                              { "Phoneme": "d", "PronunciationAssessment": { "AccuracyScore": 93.0 } } ] },
              { "Word": "like", "PronunciationAssessment": { "AccuracyScore": 90.0, "ErrorType": "None" }, "Phonemes": [] },
              { "Word": "a", "PronunciationAssessment": { "AccuracyScore": 88.0, "ErrorType": "None" }, "Phonemes": [] },
              { "Word": "cup", "PronunciationAssessment": { "AccuracyScore": 84.0, "ErrorType": "None" }, "Phonemes": [] },
              { "Word": "of", "PronunciationAssessment": { "AccuracyScore": 80.0, "ErrorType": "None" }, "Phonemes": [] },
              { "Word": "coffee", "PronunciationAssessment": { "AccuracyScore": 52.4, "ErrorType": "Mispronunciation" },
                "Phonemes": [ { "Phoneme": "k", "PronunciationAssessment": { "AccuracyScore": 97.0 } },
                              { "Phoneme": "ao", "PronunciationAssessment": { "AccuracyScore": 31.0 } },
                              { "Phoneme": "f", "PronunciationAssessment": { "AccuracyScore": 70.0 } },
                              { "Phoneme": "iy", "PronunciationAssessment": { "AccuracyScore": 88.0 } } ] }
            ]
          }]
        }
    """.trimIndent()

    @Test
    fun `parses overall, word and phoneme scores`() {
        val outcome = AzureAssessmentJsonParser.parse(scoredJson)

        assertTrue(outcome is AssessmentOutcome.Scored)
        val result = (outcome as AssessmentOutcome.Scored).result
        assertEquals(89, result.overallScore) // PronScore 88.6 rounds
        assertEquals(86, result.accuracyScore)
        assertEquals(91, result.fluencyScore)
        assertEquals(100, result.completenessScore)
        assertEquals(7, result.words.size)

        val coffee = result.words.last()
        assertEquals("coffee", coffee.word)
        assertEquals(52, coffee.accuracyScore)
        assertEquals(WordErrorType.MISPRONUNCIATION, coffee.errorType)
        assertEquals(listOf("k", "ao", "f", "iy"), coffee.phonemes.map { it.phoneme })
        assertEquals(PhonemeResult("ao", 31), coffee.weakestPhoneme)
    }

    @Test
    fun `weakest word prefers one with phoneme detail`() {
        val result = (AzureAssessmentJsonParser.parse(scoredJson) as AssessmentOutcome.Scored).result
        assertEquals(6, result.weakestWordIndex) // "coffee"
    }

    @Test
    fun `falls back to accuracy when PronScore is missing`() {
        val json = scoredJson.replace(", \"PronScore\": 88.6", "")
        val result = (AzureAssessmentJsonParser.parse(json) as AssessmentOutcome.Scored).result
        assertEquals(86, result.overallScore)
    }

    @Test
    fun `accepts the older flat layout without PronunciationAssessment nesting`() {
        val json = """
            { "RecognitionStatus": "Success",
              "NBest": [{ "AccuracyScore": 70.0, "FluencyScore": 60.0, "CompletenessScore": 90.0, "PronScore": 72.0,
                "Words": [{ "Word": "hello", "AccuracyScore": 70.0, "ErrorType": "None",
                            "Phonemes": [{ "Phoneme": "hh", "AccuracyScore": 40.0 }] }] }] }
        """.trimIndent()
        val result = (AzureAssessmentJsonParser.parse(json) as AssessmentOutcome.Scored).result
        assertEquals(72, result.overallScore)
        assertEquals(40, result.words.single().phonemes.single().accuracyScore)
    }

    @Test
    fun `insertions and omissions are classified`() {
        val json = """
            { "RecognitionStatus": "Success",
              "NBest": [{ "PronunciationAssessment": { "AccuracyScore": 50, "FluencyScore": 50, "CompletenessScore": 50, "PronScore": 50 },
                "Words": [
                  { "Word": "turn", "PronunciationAssessment": { "AccuracyScore": 90, "ErrorType": "None" } },
                  { "Word": "um", "PronunciationAssessment": { "ErrorType": "Insertion" } },
                  { "Word": "left", "PronunciationAssessment": { "ErrorType": "Omission" } }
                ] }] }
        """.trimIndent()
        val result = (AzureAssessmentJsonParser.parse(json) as AssessmentOutcome.Scored).result
        assertEquals(
            listOf(WordErrorType.NONE, WordErrorType.INSERTION, WordErrorType.OMISSION),
            result.words.map { it.errorType },
        )
        assertEquals(listOf(0, 2), result.referenceWordIndices)
        assertEquals(0, result.words[2].accuracyScore)
        assertTrue(result.words[2].wasSkipped)
    }

    @Test
    fun `empty NBest means no speech`() {
        val outcome = AzureAssessmentJsonParser.parse("""{ "RecognitionStatus": "Success", "NBest": [] }""")
        assertTrue(outcome is AssessmentOutcome.NoSpeech)
    }

    @Test
    fun `non-success status means no speech`() {
        val outcome = AzureAssessmentJsonParser.parse("""{ "RecognitionStatus": "InitialSilenceTimeout" }""")
        assertTrue(outcome is AssessmentOutcome.NoSpeech)
    }

    @Test
    fun `every reference word omitted means no speech`() {
        val json = """
            { "RecognitionStatus": "Success",
              "NBest": [{ "PronunciationAssessment": { "AccuracyScore": 0, "PronScore": 0 },
                "Words": [
                  { "Word": "where", "PronunciationAssessment": { "ErrorType": "Omission" } },
                  { "Word": "banana", "PronunciationAssessment": { "ErrorType": "Insertion" } }
                ] }] }
        """.trimIndent()
        assertTrue(AzureAssessmentJsonParser.parse(json) is AssessmentOutcome.NoSpeech)
    }

    @Test
    fun `garbage and empty input fail instead of throwing`() {
        assertTrue(AzureAssessmentJsonParser.parse("not json") is AssessmentOutcome.Failed)
        assertTrue(AzureAssessmentJsonParser.parse("") is AssessmentOutcome.Failed)
        assertTrue(AzureAssessmentJsonParser.parse(null) is AssessmentOutcome.Failed)
    }

    @Test
    fun `scores are clamped to 0-100`() {
        val json = scoredJson.replace("\"PronScore\": 88.6", "\"PronScore\": 140.2")
        val result = (AzureAssessmentJsonParser.parse(json) as AssessmentOutcome.Scored).result
        assertEquals(100, result.overallScore)
    }

    /**
     * A real response from the eastus endpoint (Comprehensive, miscue on) for a
     * device recording of "The enemy was hiding in the hills." It uses the flat
     * layout: scores sit directly on NBest[0] and on each word.
     */
    @Test
    fun `parses a real Azure response`() {
        val json = javaClass.classLoader!!.getResource("enemy_rest.json")!!.readText()
        val result = (AzureAssessmentJsonParser.parse(json) as AssessmentOutcome.Scored).result

        assertEquals(90, result.overallScore)       // PronScore 89.8
        assertEquals(92, result.accuracyScore)
        assertEquals(99, result.fluencyScore)
        assertEquals(86, result.completenessScore)
        assertEquals(listOf("the", "enemy", "was", "hiding", "in", "the", "hills"), result.words.map { it.word })

        val enemy = result.words[1]
        assertEquals(55, enemy.accuracyScore)
        assertEquals(WordErrorType.MISPRONUNCIATION, enemy.errorType)
        assertEquals(PhonemeResult("eh", 32), enemy.weakestPhoneme)
        assertEquals(1, result.weakestWordIndex)
        assertEquals(
            listOf("The", "enemy", "was", "hiding", "in", "the", "hills."),
            alignToReference("The enemy was hiding in the hills.", result).map { it.text },
        )
    }
}
