package com.maxeydev.picklejot.ui.share

import kotlinx.serialization.json.Json

object CardDataSerializer {
    private val json =
        Json {
            encodeDefaults = true
            explicitNulls = true
        }

    fun toJson(data: CardData): String = escapeForScript(json.encodeToString(CardData.serializer(), data))

    fun renderCall(
        data: CardData,
        token: String,
    ): String {
        require(token.all { it.isLetterOrDigit() || it == '-' }) { "A render token must be a plain identifier." }
        return "window.renderCard(${toJson(data)}, \"$token\");"
    }

    fun decode(encoded: String): CardData = json.decodeFromString(CardData.serializer(), encoded)

    private fun escapeForScript(encodedJson: String): String =
        buildString(encodedJson.length) {
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
