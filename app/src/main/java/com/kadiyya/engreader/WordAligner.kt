package com.kadiyya.engreader

/** A word of the prompter text and where it sits in the displayed string. */
class Token(val word: String, val start: Int, val end: Int)

fun tokenize(text: String): List<Token> =
    Regex("[A-Za-z0-9'’]+").findAll(text).mapNotNull { m ->
        val word = m.value.lowercase().replace('’', '\'').trim('\'')
        if (word.isEmpty()) null else Token(word, m.range.first, m.range.last + 1)
    }.toList()

/**
 * Matches spoken words to the prompter text in reading order.
 *
 * Each spoken word is looked up a few words ahead of the last match, and never
 * far behind the line currently on the reading band, so a skipped passage does
 * not leave the reader stuck behind the text.
 */
class WordAligner(private val tokens: List<Token>) {
    private val committed = BooleanArray(tokens.size)
    private var committedPointer = 0

    /** Committed matches plus the tentative matches of the phrase still being spoken. */
    var matched: BooleanArray = committed.copyOf()
        private set

    val matchedCount: Int get() = matched.count { it }

    fun reset() {
        committed.fill(false)
        committedPointer = 0
        matched = committed.copyOf()
    }

    fun preview(words: List<String>, bandIndex: Int) {
        val state = committed.copyOf()
        align(words, bandIndex, state, committedPointer)
        matched = state
    }

    fun commit(words: List<String>, bandIndex: Int) {
        committedPointer = align(words, bandIndex, committed, committedPointer)
        matched = committed.copyOf()
    }

    private fun align(words: List<String>, bandIndex: Int, state: BooleanArray, startPointer: Int): Int {
        var pointer = startPointer
        for (word in words) {
            val from = maxOf(pointer, bandIndex - BEHIND_BAND)
            val to = minOf(tokens.size, maxOf(pointer + LOOK_AHEAD, bandIndex + AHEAD_OF_BAND))
            for (i in from until to) {
                if (!state[i] && tokens[i].word == word) {
                    state[i] = true
                    pointer = i + 1
                    break
                }
            }
        }
        return pointer
    }

    private companion object {
        const val LOOK_AHEAD = 6
        const val BEHIND_BAND = 30
        const val AHEAD_OF_BAND = 15
    }
}
