package com.gambitdrame.app.data

import android.content.Context

class ProgressStore(context: Context) {
    private val prefs = context.getSharedPreferences("gambitdrame_progress", Context.MODE_PRIVATE)

    val totalAttempts: Int
        get() = prefs.getInt(KEY_ATTEMPTS, 0)

    val totalCorrect: Int
        get() = prefs.getInt(KEY_CORRECT, 0)

    fun record(correct: Boolean) {
        val attempts = totalAttempts + 1
        val correctCount = totalCorrect + if (correct) 1 else 0
        prefs.edit()
            .putInt(KEY_ATTEMPTS, attempts)
            .putInt(KEY_CORRECT, correctCount)
            .apply()
    }

    fun reset() {
        prefs.edit().clear().apply()
    }

    companion object {
        private const val KEY_ATTEMPTS = "attempts"
        private const val KEY_CORRECT = "correct"
    }
}
