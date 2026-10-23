package com.kadiyya.engreader

import android.content.Context

/** Stores which levels are unlocked and the best result for each level. */
class Progress(context: Context) {
    private val prefs = context.getSharedPreferences("progress", Context.MODE_PRIVATE)

    /** Highest level number the user may play. */
    var unlockedLevel: Int
        get() = prefs.getInt("unlocked", 1)
        private set(value) = prefs.edit().putInt("unlocked", value).apply()

    fun isCompleted(level: Int): Boolean = prefs.getBoolean("done_$level", false)

    fun stars(level: Int): Int = prefs.getInt("stars_$level", 0)

    fun completedCount(): Int = Levels.all.count { isCompleted(it.number) }

    fun saveResult(level: Int, stars: Int) {
        prefs.edit()
            .putBoolean("done_$level", true)
            .putInt("stars_$level", maxOf(stars, stars(level)))
            .apply()
        if (level >= unlockedLevel && level < Levels.all.size) {
            unlockedLevel = level + 1
        }
    }
}
