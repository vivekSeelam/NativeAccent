package com.example.nativeaccent.data

/**
 * A single thing the learner is asked to say.
 *
 * [id] is stable and is what recordings on disk are keyed by, so reordering or
 * growing the deck never makes an old file collide with a new item.
 */
data class PracticeItem(
    val id: String,
    val text: String,
    val hint: String? = null,
)
