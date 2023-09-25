package com.github.inxilpro.intellijalpine

import com.intellij.lang.javascript.psi.JSInheritedLanguagesHelper
import com.intellij.lang.javascript.psi.JSObjectLiteralExpression
import com.intellij.psi.util.elementType
import com.intellij.psi.xml.XmlAttribute
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object AlpineAttributeInjectionHeader {

    @Serializable
    data class DataIndicesHeader(
            @SerialName("aso") val attributeStartOffset: Int,
            @SerialName("tso") val tagStartOffset: Int,
            @SerialName("e") val expression: String,
    )

    @Serializable
    data class Header(
            @SerialName("ns") val namespace: String,
            @SerialName("so") val startOffset: String,
            @SerialName("do") val dataOffsets: List<DataIndicesHeader>,
    )

    fun deserialize(header: String): Header {
        return Json.decodeFromString<Header>(header)
    }

    fun serialize(startOffset: Int, data: List<XmlAttribute>): String {
        val mappedData = data.flatMap { attribute ->
            val expression = JSInheritedLanguagesHelper.createExpressionFromText(attribute.value!!, attribute) as JSObjectLiteralExpression
            expression.properties.map { DataIndicesHeader(attribute.textOffset, attribute.parent?.textOffset!!, it.name.toString()) }
        }
        val json = Json.encodeToString(Header(Alpine.NAMESPACE, Int.MAX_VALUE.toString(), mappedData))
        return patchStartOffset(json, startOffset)
    }

    private fun patchStartOffset(json: String, startOffset: Int): String {
        return json.replace("\"so\":\"${Int.MAX_VALUE}\"", "\"so\":\"${startOffset.toString().padStart(Int.MAX_VALUE.toString().length, '0')}\"")
    }
}