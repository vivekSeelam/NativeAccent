package com.example.nativeaccent.data

/**
 * One thing the learner is asked to say.
 *
 * [text] is the full reference sentence, sent verbatim to pronunciation
 * assessment. [id] is stable and is what recordings on disk are keyed by, so
 * reordering or rewording the deck never makes an old file collide with a new item.
 */
data class PracticeItem(
    val id: String,
    val text: String,
    val hint: String? = null,
)
