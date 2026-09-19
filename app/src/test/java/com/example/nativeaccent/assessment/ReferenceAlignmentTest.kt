package com.example.nativeaccent.assessment

import org.junit.Assert.assertEquals
import org.junit.Test

class ReferenceAlignmentTest {

    private fun word(text: String, score: Int = 90, type: WordErrorType = WordErrorType.NONE) =
        WordResult(text, score, emptyList(), type)

    private fun result(vararg words: WordResult) =
        PronunciationResult(80, 80, 80, 80, words.toList())

    @Test
    fun `keeps reference casing and punctuation and skips insertions`() {
        val result = result(
            word("can"), word("i"), word("uh", type = WordErrorType.INSERTION), word("get"),
            word("the"), word("check"), word("please"),
        )
        val aligned = alignToReference("Can I get the check, please?", result)

        assertEquals(listOf("Can", "I", "get", "the", "check,", "please?"), aligned.map { it.text })
        // Index 2 is the insertion and must be skipped.
        assertEquals(listOf(0, 1, 3, 4, 5, 6), aligned.map { it.resultIndex })
    }

    @Test
    fun `omitted words stay in place`() {
        val result = result(word("turn"), word("left", 0, WordErrorType.OMISSION), word("here"))
        val aligned = alignToReference("Turn left here.", result)
        assertEquals(listOf("Turn", "left", "here."), aligned.map { it.text })
        assertEquals(1, aligned[1].resultIndex)
    }

    @Test
    fun `falls back to service words when token counts disagree`() {
        // e.g. the service split a token the reference kept whole
        val result = result(word("it"), word("is"), word("nice"))
        val aligned = alignToReference("It's nice.", result)
        assertEquals(listOf("it", "is", "nice"), aligned.map { it.text })
        assertEquals(listOf(0, 1, 2), aligned.map { it.resultIndex })
    }

    @Test
    fun `unscored words have no result index`() {
        val words = unscoredWords("  Where are   you from? ")
        assertEquals(listOf("Where", "are", "you", "from?"), words.map { it.text })
        assertEquals(listOf(null, null, null, null), words.map { it.resultIndex })
    }

    @Test
    fun `score bands`() {
        assertEquals(ScoreBand.GOOD, ScoreBand.of(100))
        assertEquals(ScoreBand.GOOD, ScoreBand.of(80))
        assertEquals(ScoreBand.FAIR, ScoreBand.of(79))
        assertEquals(ScoreBand.FAIR, ScoreBand.of(60))
        assertEquals(ScoreBand.POOR, ScoreBand.of(59))
        assertEquals(ScoreBand.POOR, ScoreBand.of(0))
    }
}
