package com.example.nativeaccent.data

/**
 * Source of practice content.
 *
 * v1 ships a hardcoded deck; a later version can back this with Room or a
 * network catalogue without touching the ViewModel or the UI.
 */
interface PracticeRepository {
    val items: List<PracticeItem>
}

class LocalPracticeRepository : PracticeRepository {
    // Every item is a full sentence, because scoring works on connected speech.
    // The first 30 each carry one commonly mispronounced word, named in the hint.
    // Recordings are keyed by id, so ids stay stable even when the text changes.
    override val items: List<PracticeItem> = listOf(
        // ---- Sentences built around a tricky word (30) ----
        PracticeItem("enemy", "The enemy was hiding in the hills.", "enemy: EN-uh-mee — stress the first syllable"),
        PracticeItem("committee", "The committee will meet on Monday.", "committee: kuh-MIT-ee"),
        PracticeItem("comfortable", "This chair is really comfortable.", "comfortable: KUMF-ter-bul — the 'or' almost disappears"),
        PracticeItem("thorough", "She did a thorough job on the report.", "thorough: THUR-oh — not 'through'"),
        PracticeItem("rural", "They grew up in a rural town.", "rural: ROOR-ul — two r's, one breath"),
        PracticeItem("entrepreneur", "He wants to be an entrepreneur.", "entrepreneur: on-truh-pruh-NUR"),
        PracticeItem("schedule", "Let me check my schedule.", "schedule: SKEJ-ool in US English"),
        PracticeItem("especially", "I love fruit, especially mangoes.", "especially: ih-SPESH-uh-lee — no 'x' sound"),
        PracticeItem("february", "My exam is in February.", "February: FEB-roo-air-ee — FEB-yoo-air-ee is fine too"),
        PracticeItem("library", "I'll meet you at the library.", "library: LY-brair-ee — keep both r's"),
        PracticeItem("vegetable", "Add one more vegetable to the soup.", "vegetable: VEJ-tuh-bul — three syllables, not four"),
        PracticeItem("wednesday", "The meeting is on Wednesday.", "Wednesday: WENZ-day — the first 'd' is silent"),
        PracticeItem("specific", "Can you be more specific?", "specific: spuh-SIF-ik — starts with 'sp', not 'pa'"),
        PracticeItem("pronunciation", "Her pronunciation is getting better.", "pronunciation: pruh-nun-see-AY-shun — 'nun', not 'noun'"),
        PracticeItem("debt", "He paid off his debt last year.", "debt: DET — silent 'b'"),
        PracticeItem("island", "We spent a week on the island.", "island: EYE-lund — silent 's'"),
        PracticeItem("salmon", "I'll have the grilled salmon, please.", "salmon: SAM-un — silent 'l'"),
        PracticeItem("colonel", "The colonel gave the order.", "colonel: KER-nul — sounds like 'kernel'"),
        PracticeItem("choir", "She sings in the church choir.", "choir: KWY-er — rhymes with 'wire'"),
        PracticeItem("recipe", "This recipe needs three eggs.", "recipe: RES-uh-pee — three syllables"),
        PracticeItem("genre", "What's your favorite music genre?", "genre: ZHAHN-ruh — soft 'zh' as in 'measure'"),
        PracticeItem("epitome", "He is the epitome of calm.", "epitome: ih-PIT-uh-mee — four syllables"),
        PracticeItem("clothes", "I need to wash my clothes.", "clothes: KLOHZ — usually one syllable"),
        PracticeItem("world", "She wants to travel the world.", "world: WURLD — let the 'r' flow into the 'l'"),
        PracticeItem("squirrel", "A squirrel ran up the tree.", "squirrel: SKWUR-ul — two syllables in US English"),
        PracticeItem("sixth", "He finished in sixth place.", "sixth: SIKSTH — 'ks' straight into 'th'"),
        PracticeItem("mischievous", "The puppy was very mischievous.", "mischievous: MIS-chuh-vus — three syllables"),
        PracticeItem("subtle", "There's a subtle difference.", "subtle: SUT-ul — silent 'b'"),
        PracticeItem("height", "What's the height of that wall?", "height: HYTE — ends in 't', not 'th'"),
        PracticeItem("often", "I often walk to work.", "often: AW-fun — the 't' is usually silent"),

        // ---- Everyday sentences (20) ----
        PracticeItem("coffee", "I would like a cup of coffee.", "Link 'cup of' into 'cup-uh'"),
        PracticeItem("weather", "The weather is lovely today.", "Soft 'th', flat 'a' in weather"),
        PracticeItem("directions", "Could you tell me the way to the station?", "Rising tone at the end"),
        PracticeItem("seashells", "She sells seashells by the seashore.", "Switch cleanly between 's' and 'sh'"),
        PracticeItem("check-please", "Can I get the check, please?", "'Can I' links into 'ca-nai'"),
        PracticeItem("meet-you", "It's nice to meet you.", "'meet you' blends into 'MEE-choo'"),
        PracticeItem("repeat-that", "Could you repeat that, please?", "'Could you' blends into 'COO-juh'"),
        PracticeItem("water", "I'll have a glass of water.", "US 'water' uses a soft flap: WAH-der"),
        PracticeItem("grab-a-bite", "Let's grab a bite later.", "Flap the 't' in 'later': LAY-der"),
        PracticeItem("call-you-later", "I'm going to call you later.", "Casual speech: 'going to call' becomes 'gonna call'"),
        PracticeItem("very-well", "Very well, thank you.", "Teeth on lip for 'v', rounded lips for 'w'"),
        PracticeItem("how-much", "How much does it cost?", "'does it' links into 'DUZ-it'"),
        PracticeItem("third-thursday", "I think this is the third Thursday.", "Tongue between the teeth for every 'th'"),
        PracticeItem("red-barn", "The rabbit ran around the red barn.", "Round the lips for each 'r'; the tongue never taps"),
        PracticeItem("turn-left", "Turn left at the next light.", "Stress the content words: TURN LEFT at the NEXT LIGHT"),
        PracticeItem("seat-taken", "Excuse me, is this seat taken?", "Pitch rises at the end of a yes/no question"),
        PracticeItem("where-from", "Where are you from?", "Pitch falls at the end — it's not a yes/no question"),
        PracticeItem("early-bird", "The early bird catches the worm.", "'early', 'bird' and 'worm' share the same 'ur' vowel"),
        PracticeItem("what-to-do", "What do you want to do?", "Relaxed: 'whaddaya wanna do?'"),
        PracticeItem("birthday", "My birthday is in September.", "Stress: sep-TEM-ber"),
    )
}
