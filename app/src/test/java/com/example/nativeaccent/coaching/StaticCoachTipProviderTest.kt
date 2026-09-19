package com.example.nativeaccent.coaching

import com.example.nativeaccent.assessment.PhonemeResult
import com.example.nativeaccent.assessment.PronunciationResult
import com.example.nativeaccent.assessment.WordErrorType
import com.example.nativeaccent.assessment.WordResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaticCoachTipProviderTest {

    private val provider = StaticCoachTipProvider()

    private fun result(vararg words: WordResult, completeness: Int = 100, fluency: Int = 100) =
        PronunciationResult(70, 70, fluency, completeness, words.toList())

    private fun tip(word: WordResult?, result: PronunciationResult = result()) =
        runBlocking { provider.tipFor("I'll have a glass of water.", word, result) }

    @Test
    fun `keys on the weakest phoneme of the focused word`() {
        val water = WordResult(
            "water", 55,
            listOf(PhonemeResult("w", 95), PhonemeResult("ao", 80), PhonemeResult("t", 20), PhonemeResult("er", 70)),
        )
        val tip = tip(water, result(water))
        assertEquals("Sharpen your T", tip.headline)
        assertTrue(tip.body.contains("“water”"))
        assertTrue(tip.body.contains("/T/"))
        assertTrue(tip.body.contains("20"))
    }

    @Test
    fun `vowels without bespoke advice get an example word`() {
        val word = WordResult("go", 40, listOf(PhonemeResult("g", 90), PhonemeResult("ow", 30)))
        val tip = tip(word)
        assertEquals("Shape the vowel", tip.headline)
        assertTrue(tip.body.contains("“go”"))
    }

    @Test
    fun `skipped word gets its own tip`() {
        val skipped = WordResult("glass", 0, emptyList(), WordErrorType.OMISSION)
        assertTrue(tip(skipped).headline.contains("skip"))
    }

    @Test
    fun `strong word gets praise, not a correction`() {
        val word = WordResult("have", 95, listOf(PhonemeResult("hh", 92), PhonemeResult("ae", 96), PhonemeResult("v", 90)))
        assertTrue(tip(word).headline.contains("sounded good"))
    }

    @Test
    fun `no focused word falls back to sentence-level advice`() {
        assertEquals("Say the whole sentence", tip(null, result(completeness = 60)).headline)
        assertEquals("Keep it flowing", tip(null, result(fluency = 50)).headline)
    }
}
