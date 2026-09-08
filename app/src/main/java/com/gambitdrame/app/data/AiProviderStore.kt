package com.gambitdrame.app.data

import android.content.Context

/**
 * Persistent choice for the external AI used by GambitDrame.
 *
 * The handoff intentionally stays provider-agnostic: GambitDrame copies the
 * pedagogical context to the clipboard, then opens the selected web entry
 * point. No API key is required.
 */
data class AiProviderChoice(
    val id: String,
    val name: String,
    val url: String,
    val custom: Boolean = false
)

class AiProviderStore(context: Context) {
    private val prefs = context.getSharedPreferences("gambitdrame_ai_provider", Context.MODE_PRIVATE)

    val builtIns: List<AiProviderChoice> = listOf(
        AiProviderChoice("chatgpt", "ChatGPT", "https://chatgpt.com/"),
        AiProviderChoice("gemini", "Gemini", "https://gemini.google.com/"),
        AiProviderChoice("claude", "Claude", "https://claude.ai/"),
        AiProviderChoice("perplexity", "Perplexity", "https://www.perplexity.ai/")
    )

    var selected: AiProviderChoice
        get() {
            val id = prefs.getString(KEY_ID, "chatgpt") ?: "chatgpt"
            val builtIn = builtIns.firstOrNull { it.id == id }
            if (builtIn != null) return builtIn

            return AiProviderChoice(
                id = "custom",
                name = prefs.getString(KEY_NAME, "Mon IA")?.ifBlank { "Mon IA" } ?: "Mon IA",
                url = normaliseUrl(
                    prefs.getString(KEY_URL, "https://chatgpt.com/") ?: "https://chatgpt.com/"
                ),
                custom = true
            )
        }
        private set(value) {
            prefs.edit()
                .putString(KEY_ID, value.id)
                .putString(KEY_NAME, value.name)
                .putString(KEY_URL, value.url)
                .apply()
        }

    fun selectBuiltIn(id: String) {
        builtIns.firstOrNull { it.id == id }?.let { selected = it }
    }

    fun selectCustom(name: String, url: String) {
        selected = AiProviderChoice(
            id = "custom",
            name = name.trim().ifBlank { "Mon IA" },
            url = normaliseUrl(url),
            custom = true
        )
    }

    private fun normaliseUrl(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isBlank()) return "https://chatgpt.com/"
        return if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            trimmed
        } else {
            "https://$trimmed"
        }
    }

    companion object {
        private const val KEY_ID = "id"
        private const val KEY_NAME = "name"
        private const val KEY_URL = "url"
    }
}
