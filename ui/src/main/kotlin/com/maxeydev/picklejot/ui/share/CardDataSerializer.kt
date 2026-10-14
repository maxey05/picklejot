package com.maxeydev.picklejot.ui.share

import kotlinx.serialization.json.Json

private const val RENDER_CALL_OVERHEAD = 64

object CardDataSerializer {
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = true
        }

    fun toJson(data: CardData): String = buildString { appendEscaped(json.encodeToString(CardData.serializer(), data)) }

    // The photo rides along as a base64 data URI, so this can run to megabytes: build the script in one
    // pass (no second copy of the JSON) and call this off the main thread.
    fun renderCall(
        data: CardData,
        token: String,
    ): String {
        require(token.all { it.isLetterOrDigit() || it == '-' }) { "A render token must be a plain identifier." }
        val encoded = json.encodeToString(CardData.serializer(), data)
        return buildString(encoded.length + RENDER_CALL_OVERHEAD) {
            append("window.renderCard(")
            appendEscaped(encoded)
            append(", \"").append(token).append("\");")
        }
    }

    fun decode(encoded: String): CardData = json.decodeFromString(CardData.serializer(), encoded)

    private fun StringBuilder.appendEscaped(encodedJson: String) {
        encodedJson.forEach { character ->
            when (character) {
                '<' -> append("\\u003c")
                '>' -> append("\\u003e")
                '&' -> append("\\u0026")
                ' ' -> append("\\u2028")
                ' ' -> append("\\u2029")
                else -> append(character)
            }
        }
    }
}
