package com.example.nativeaccent.scoring

import com.example.nativeaccent.data.PracticeItem

/** Per-phoneme accuracy, 0f..1f. */
data class PhonemeScore(
    val symbol: String,
    val accuracy: Float,
)

/** Whole-attempt result. [overallPercent] is 0..100. */
data class PronunciationScore(
    val overallPercent: Int,
    val phonemes: List<PhonemeScore> = emptyList(),
)

/**
 * The seam the v1 scoring module plugs into.
 *
 * Screen 2 already asks for a score after every attempt and renders the panel
 * only when one comes back, so shipping real scoring means providing another
 * implementation here and handing it to [com.example.nativeaccent.viewmodel.PracticeViewModel] —
 * no UI or audio code changes.
 */
interface PronunciationScorer {
    /** Returns null when this build cannot score (the v0 default). */
    suspend fun score(item: PracticeItem, recordingPath: String): PronunciationScore?
}

/** v0: record-and-compare only, no judgement. */
object NoScoringYet : PronunciationScorer {
    override suspend fun score(item: PracticeItem, recordingPath: String): PronunciationScore? = null
}
