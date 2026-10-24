package com.kadiyya.engreader

import android.content.Context
import java.time.LocalDate

/** Stars, best scores, unlocked level and the daily streak, stored on the device. */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("progress_v2", Context.MODE_PRIVATE)
    private val levelCount = Worlds.levels(context).size

    /** Highest level number the user may play. */
    val unlockedLevel: Int get() = prefs.getInt("unlocked", 1)

    fun stars(level: Int): Int = prefs.getInt("stars_$level", 0)

    fun bestScore(level: Int): Int = prefs.getInt("score_$level", 0)

    fun totalStars(): Int = (1..levelCount).sumOf { stars(it) }

    fun totalScore(): Int = (1..levelCount).sumOf { bestScore(it) }

    /** Days in a row with at least one passed level; 0 once a day is missed. */
    fun streak(): Int {
        val last = prefs.getLong("streak_day", -1)
        val today = LocalDate.now().toEpochDay()
        return if (last >= today - 1) prefs.getInt("streak", 0) else 0
    }

    /** Records a passed level. Returns true when the score is a new personal best. */
    fun savePass(level: Int, stars: Int, score: Int): Boolean {
        val isRecord = score > bestScore(level)
        val edit = prefs.edit()
            .putInt("stars_$level", maxOf(stars, stars(level)))
            .putInt("score_$level", maxOf(score, bestScore(level)))
        if (level >= unlockedLevel && level < levelCount) edit.putInt("unlocked", level + 1)

        val today = LocalDate.now().toEpochDay()
        val last = prefs.getLong("streak_day", -1)
        if (last != today) {
            edit.putInt("streak", if (last == today - 1) streak() + 1 else 1)
            edit.putLong("streak_day", today)
        }
        edit.apply()
        return isRecord
    }
}
