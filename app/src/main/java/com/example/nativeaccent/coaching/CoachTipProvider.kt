package com.example.nativeaccent.coaching

import com.example.nativeaccent.assessment.PronunciationResult
import com.example.nativeaccent.assessment.WordResult

data class CoachTip(val headline: String, val body: String)

/**
 * Turns scores into advice the learner can act on.
 *
 * v1 is [StaticCoachTipProvider], a lookup keyed on the weakest phoneme.
 *
 * TODO(llm-coach): add an LlmCoachTipProvider. Everything a model needs is in
 *  the arguments — the sentence, the focused word, and per-phoneme scores. Send
 *  those as structured input, ask for one or two sentences of mouth-position
 *  advice, and fall back to [StaticCoachTipProvider] on timeout or error. Keep
 *  the API key server-side, the same way as SpeechCredentialsProvider.
 */
interface CoachTipProvider {
    /**
     * @param referenceText the full sentence that was attempted
     * @param focusedWord the word the learner is looking at, or null for an overall tip
     */
    suspend fun tipFor(
        referenceText: String,
        focusedWord: WordResult?,
        result: PronunciationResult,
    ): CoachTip
}
