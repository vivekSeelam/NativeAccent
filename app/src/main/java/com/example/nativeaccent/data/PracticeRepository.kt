package com.example.nativeaccent.data

/**
 * Source of practice content.
 *
 * v0 ships a hardcoded deck; a later version can back this with Room or a
 * network catalogue without touching the ViewModel or the UI.
 */
interface PracticeRepository {
    val items: List<PracticeItem>
}

class LocalPracticeRepository : PracticeRepository {
    // Words first, then sentences. Order is free to change: recordings are keyed
    // by id, not position.
    override val items: List<PracticeItem> = listOf(
        // ---- Words (30) ----
        PracticeItem("enemy", "enemy", "EN-uh-mee — three syllables, stress the first"),
        PracticeItem("committee", "committee", "kuh-MIT-ee — double the 't' sound"),
        PracticeItem("comfortable", "comfortable", "KUMF-ter-bul — the 'or' almost disappears"),
        PracticeItem("thorough", "thorough", "THUR-oh — not 'through'"),
        PracticeItem("rural", "rural", "ROOR-ul — two r's, one breath"),
        PracticeItem("entrepreneur", "entrepreneur", "on-truh-pruh-NUR"),
        PracticeItem("schedule", "schedule", "SKEJ-ool in US English"),
        PracticeItem("especially", "especially", "ih-SPESH-uh-lee — no 'x' sound"),
        PracticeItem("february", "February", "FEB-roo-air-ee — FEB-yoo-air-ee is fine too"),
        PracticeItem("library", "library", "LY-brair-ee — keep both r's"),
        PracticeItem("vegetable", "vegetable", "VEJ-tuh-bul — three syllables, not four"),
        PracticeItem("wednesday", "Wednesday", "WENZ-day — the first 'd' is silent"),
        PracticeItem("specific", "specific", "spuh-SIF-ik — starts with 'sp', not 'pa'"),
        PracticeItem("pronunciation", "pronunciation", "pruh-nun-see-AY-shun — 'nun', not 'noun'"),
        PracticeItem("debt", "debt", "DET — silent 'b'"),
        PracticeItem("island", "island", "EYE-lund — silent 's'"),
        PracticeItem("salmon", "salmon", "SAM-un — silent 'l'"),
        PracticeItem("colonel", "colonel", "KER-nul — sounds like 'kernel'"),
        PracticeItem("choir", "choir", "KWY-er — rhymes with 'wire'"),
        PracticeItem("recipe", "recipe", "RES-uh-pee — three syllables"),
        PracticeItem("genre", "genre", "ZHAHN-ruh — soft 'zh' as in 'measure'"),
        PracticeItem("epitome", "epitome", "ih-PIT-uh-mee — four syllables"),
        PracticeItem("clothes", "clothes", "KLOHZ — usually one syllable"),
        PracticeItem("world", "world", "WURLD — let the 'r' flow into the 'l'"),
        PracticeItem("squirrel", "squirrel", "SKWUR-ul — two syllables in US English"),
        PracticeItem("sixth", "sixth", "SIKSTH — 'ks' straight into 'th'"),
        PracticeItem("mischievous", "mischievous", "MIS-chuh-vus — three syllables, no 'ee-us'"),
        PracticeItem("subtle", "subtle", "SUT-ul — silent 'b'"),
        PracticeItem("height", "height", "HYTE — ends in 't', not 'th'"),
        PracticeItem("often", "often", "AW-fun — the 't' is usually silent"),

        // ---- Sentences (20) ----
        PracticeItem("coffee", "I would like a cup of coffee", "Link 'cup of' into 'cup-uh'"),
        PracticeItem("weather", "The weather is lovely today", "Soft 'th', flat 'a' in weather"),
        PracticeItem("directions", "Could you tell me the way to the station?", "Rising tone at the end"),
        PracticeItem("seashells", "She sells seashells by the seashore", "Switch cleanly between 's' and 'sh'"),
        PracticeItem("check-please", "Can I get the check, please?", "'Can I' links into 'ca-nai'"),
        PracticeItem("meet-you", "It's nice to meet you", "'meet you' blends into 'MEE-choo'"),
        PracticeItem("repeat-that", "Could you repeat that, please?", "'Could you' blends into 'COO-juh'"),
        PracticeItem("water", "I'll have a glass of water", "US 'water' uses a soft flap: WAH-der"),
        PracticeItem("grab-a-bite", "Let's grab a bite later", "Flap the 't' in 'later': LAY-der"),
        PracticeItem("call-you-later", "I'm going to call you later", "Casual speech: 'going to call' becomes 'gonna call'"),
        PracticeItem("very-well", "Very well, thank you", "Teeth on lip for 'v', rounded lips for 'w'"),
        PracticeItem("how-much", "How much does it cost?", "'does it' links into 'DUZ-it'"),
        PracticeItem("third-thursday", "I think this is the third Thursday", "Tongue between the teeth for every 'th'"),
        PracticeItem("red-barn", "The rabbit ran around the red barn", "Round the lips for each 'r'; the tongue never taps"),
        PracticeItem("turn-left", "Turn left at the next light", "Stress the content words: TURN LEFT at the NEXT LIGHT"),
        PracticeItem("seat-taken", "Excuse me, is this seat taken?", "Pitch rises at the end of a yes/no question"),
        PracticeItem("where-from", "Where are you from?", "Pitch falls at the end — it's not a yes/no question"),
        PracticeItem("early-bird", "The early bird catches the worm", "'early', 'bird' and 'worm' share the same 'ur' vowel"),
        PracticeItem("what-to-do", "What do you want to do?", "Relaxed: 'whaddaya wanna do?'"),
        PracticeItem("birthday", "My birthday is in September", "Stress: sep-TEM-ber"),
    )
}
