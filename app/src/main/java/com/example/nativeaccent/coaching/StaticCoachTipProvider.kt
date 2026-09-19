package com.example.nativeaccent.coaching

import com.example.nativeaccent.assessment.PronunciationResult
import com.example.nativeaccent.assessment.WordResult

/**
 * Canned advice keyed on the weakest phoneme of the focused word.
 *
 * Phoneme names are Azure's default en-US SAPI set ("t", "th", "dh", "ih", "er", ...).
 */
class StaticCoachTipProvider : CoachTipProvider {

    override suspend fun tipFor(
        referenceText: String,
        focusedWord: WordResult?,
        result: PronunciationResult,
    ): CoachTip {
        val word = focusedWord ?: return overallTip(result)

        if (word.wasSkipped) {
            return CoachTip(
                headline = "Don't skip “${word.word}”",
                body = "It sounded like “${word.word}” was left out. Say every word, even the small ones — " +
                    "listen to the coach at 0.5x to hear where it goes.",
            )
        }

        val weakest = word.weakestPhoneme
            ?: return CoachTip(
                headline = "Match the coach",
                body = "Play the coach at 0.5x, then say “${word.word}” slowly, copying each sound.",
            )

        if (weakest.accuracyScore >= GOOD_ENOUGH) {
            return CoachTip(
                headline = "“${word.word}” sounded good",
                body = "Every sound in this word scored ${GOOD_ENOUGH}+. Tap a word in another colour to work on that one.",
            )
        }

        val advice = ADVICE[weakest.phoneme.lowercase()] ?: VOWELS[weakest.phoneme.lowercase()]?.let(::vowelAdvice)
            ?: GENERIC
        return CoachTip(
            headline = advice.first,
            body = "In “${word.word}”, the /${weakest.phoneme.uppercase()}/ sound scored ${weakest.accuracyScore}. ${advice.second}",
        )
    }

    private fun overallTip(result: PronunciationResult): CoachTip = when {
        result.completenessScore < 80 -> CoachTip(
            "Say the whole sentence",
            "Some words were missing. Try it once at 0.5x with the coach, then say every word.",
        )
        result.fluencyScore < 70 -> CoachTip(
            "Keep it flowing",
            "There were some long pauses. Try saying the sentence in one breath, linking the words together.",
        )
        else -> CoachTip(
            "Nice work",
            "Tap any word to see how each of its sounds scored.",
        )
    }

    private fun vowelAdvice(example: String) = "Shape the vowel" to
        "Listen to the coach at 0.5x and copy the mouth shape — it's the vowel in “$example”."

    private companion object {
        const val GOOD_ENOUGH = 80

        val GENERIC = "Slow it down" to
            "Play the coach at 0.5x, then say the word slowly, matching each sound."

        /** headline to advice. */
        val ADVICE: Map<String, Pair<String, String>> = mapOf(
            "t" to ("Sharpen your T" to
                "Your T sound was too soft. Tap the tongue tip firmly behind your top teeth. " +
                "Between vowels (water, later) American English uses a quick flap, like a light D."),
            "d" to ("Give the D some voice" to
                "Tap the tongue tip behind your top teeth and hum at the same time — a D is a voiced T."),
            "th" to ("Tongue between the teeth" to
                "For the “th” in think, rest your tongue tip lightly between your teeth and blow — " +
                "don't let it turn into T, S or F."),
            "dh" to ("Hum the TH" to
                "For the “th” in this or the, put your tongue between your teeth and hum. It shouldn't sound like D or Z."),
            "r" to ("Round into the R" to
                "Pull your tongue back without touching the roof of your mouth, and round your lips a little. " +
                "The tongue never taps for an American R."),
            "l" to ("Touch for the L" to
                "Press your tongue tip on the ridge just behind your top teeth and let the sound flow around its sides."),
            "v" to ("Teeth on lip for V" to
                "Rest your top teeth on your bottom lip and hum. If your lips touch each other, it becomes a B or W."),
            "w" to ("Round your lips for W" to
                "Push your lips into a small circle with no teeth contact, then open into the next vowel."),
            "f" to ("Teeth on lip for F" to
                "Rest your top teeth on your bottom lip and blow — no voice, just air."),
            "s" to ("Keep the S crisp" to
                "Keep your tongue tip just behind your top teeth, not touching them, and push a thin stream of air."),
            "z" to ("Buzz the Z" to
                "Same tongue position as S, but add your voice — you should feel a buzz."),
            "sh" to ("Round for SH" to
                "Pull your tongue a little further back than for S and push your lips forward."),
            "zh" to ("Voice the ZH" to
                "Like SH with your voice on — the middle sound in “measure”."),
            "ch" to ("Pop the CH" to
                "Start with a T, then release straight into SH: t-sh."),
            "jh" to ("Pop the J" to
                "Start with a D, then release into the “measure” sound: d-zh."),
            "ng" to ("Keep NG at the back" to
                "Lift the back of your tongue to your soft palate and hum through your nose. Don't add a G at the end."),
            "n" to ("Close for N" to
                "Tongue tip on the ridge behind your top teeth, sound through the nose."),
            "m" to ("Close your lips for M" to
                "Press your lips together and hum through your nose."),
            "h" to ("Breathe the H" to
                "An H is just a breath before the vowel — let the air out, don't swallow it."),
            "p" to ("Release a puff for P" to
                "At the start of a stressed syllable, a P needs a small puff of air."),
            "k" to ("Release a puff for K" to
                "Lift the back of your tongue, stop the air, then release it with a small puff."),
            "b" to ("Voice the B" to "Close your lips and hum as you release them."),
            "g" to ("Voice the G" to "Same place as K, but with your voice on."),
            "y" to ("Glide the Y" to "Start with your tongue high, as for “ee”, and slide into the next vowel."),
            "ih" to ("Relax the short I" to
                "The vowel in “sit” is shorter and looser than the “ee” in “seat”. Don't smile as much."),
            "iy" to ("Stretch the long E" to
                "For the vowel in “seat”, spread your lips and hold it a little longer than “sit”."),
            "ae" to ("Open up the A" to
                "For the vowel in “cat”, drop your jaw and spread your lips — wider than “bet”."),
            "ah" to ("Relax the UH" to
                "The vowel in “cup” is short and relaxed, with your mouth half open."),
            "ax" to ("Keep it light" to
                "This is an unstressed “uh”. Say it quickly and lightly — don't give it full weight."),
            "er" to ("Hold the ER" to
                "The vowel in “bird” is one sound: tongue pulled back, lips slightly rounded. There's no separate vowel before the R."),
        )

        /** Vowels without bespoke advice, mapped to an example word. */
        val VOWELS: Map<String, String> = mapOf(
            "aa" to "father", "ao" to "thought", "aw" to "now", "ay" to "my", "eh" to "bed",
            "ey" to "day", "ow" to "go", "oy" to "boy", "uh" to "book", "uw" to "food",
        )
    }
}
