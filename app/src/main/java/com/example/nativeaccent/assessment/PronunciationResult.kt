package com.example.nativeaccent.assessment

/** One sound within a word. Scores are 0..100. */
data class PhonemeResult(
    val phoneme: String,
    val accuracyScore: Int,
)

/** How the service classified a word relative to the reference text (miscue detection). */
enum class WordErrorType {
    NONE,
    MISPRONUNCIATION,
    /** In the reference text but not heard. */
    OMISSION,
    /** Heard but not in the reference text. */
    INSERTION,
    UNEXPECTED_BREAK,
    MISSING_BREAK,
    MONOTONE,
    OTHER;

    companion object {
        fun fromAzure(value: String?): WordErrorType = when (value) {
            null, "", "None" -> NONE
            "Mispronunciation" -> MISPRONUNCIATION
            "Omission" -> OMISSION
            "Insertion" -> INSERTION
            "UnexpectedBreak" -> UNEXPECTED_BREAK
            "MissingBreak" -> MISSING_BREAK
            "Monotone" -> MONOTONE
            else -> OTHER
        }
    }
}

data class WordResult(
    val word: String,
    val accuracyScore: Int,
    val phonemes: List<PhonemeResult>,
    val errorType: WordErrorType = WordErrorType.NONE,
) {
    val wasSkipped: Boolean get() = errorType == WordErrorType.OMISSION

    /** The sound most worth practising, or null if there is no phoneme detail. */
    val weakestPhoneme: PhonemeResult? get() = phonemes.minByOrNull { it.accuracyScore }
}

/** Service-agnostic scoring of one attempt. All scores are 0..100. */
data class PronunciationResult(
    val overallScore: Int,
    val accuracyScore: Int,
    val fluencyScore: Int,
    val completenessScore: Int,
    /** Every word the service reported, in spoken order, including insertions. */
    val words: List<WordResult>,
    val recognizedText: String = "",
) {
    /** Indices into [words] for the words the learner was asked to say. */
    val referenceWordIndices: List<Int>
        get() = words.indices.filter { words[it].errorType != WordErrorType.INSERTION }

    /**
     * The word to open the breakdown on: the lowest-scoring reference word that
     * has phoneme detail, falling back to the lowest-scoring one overall.
     */
    val weakestWordIndex: Int?
        get() {
            val candidates = referenceWordIndices
            return candidates.filter { words[it].phonemes.isNotEmpty() }
                .minByOrNull { words[it].accuracyScore }
                ?: candidates.minByOrNull { words[it].accuracyScore }
        }
}

/** Colour band for any 0..100 score: green >= 80, yellow 60..79, red < 60. */
enum class ScoreBand {
    GOOD,
    FAIR,
    POOR;

    companion object {
        fun of(score: Int): ScoreBand = when {
            score >= 80 -> GOOD
            score >= 60 -> FAIR
            else -> POOR
        }
    }
}
