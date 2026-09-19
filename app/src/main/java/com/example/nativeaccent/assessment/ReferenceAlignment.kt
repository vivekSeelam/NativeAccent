package com.example.nativeaccent.assessment

/**
 * One word as shown to the learner. [text] keeps the reference sentence's own
 * casing and punctuation ("coffee." rather than Azure's "coffee");
 * [resultIndex] points into [PronunciationResult.words], or is null when the
 * word has no score.
 */
data class DisplayWord(val text: String, val resultIndex: Int?)

/**
 * Pairs the reference sentence's tokens with the scored words.
 *
 * With miscue detection on, Azure reports every reference word in order (as
 * scored or Omission) with Insertions mixed in. Dropping insertions normally
 * leaves a one-to-one match with the whitespace-separated reference tokens. If
 * the counts disagree (unusual tokenisation), this falls back to Azure's own
 * word list rather than risk colouring the wrong word.
 */
fun alignToReference(referenceText: String, result: PronunciationResult): List<DisplayWord> {
    val tokens = referenceText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val scored = result.referenceWordIndices

    if (tokens.size == scored.size) {
        return tokens.mapIndexed { i, token -> DisplayWord(token, scored[i]) }
    }
    return scored.map { DisplayWord(result.words[it].word, it) }
}

/** Plain tokens with no scores, for showing the sentence before or without a result. */
fun unscoredWords(referenceText: String): List<DisplayWord> =
    referenceText.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }.map { DisplayWord(it, null) }
