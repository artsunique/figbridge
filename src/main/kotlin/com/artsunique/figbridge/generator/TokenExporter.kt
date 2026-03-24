package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.FigmaVariable
import com.artsunique.figbridge.api.FigmaVariableCollection
import kotlinx.serialization.json.*

object TokenExporter {

    fun generateTokensCss(
        variables: Map<String, FigmaVariable>,
        collections: Map<String, FigmaVariableCollection>,
    ): String {
        if (variables.isEmpty()) return "/* No design tokens found */\n"

        // Group variables by collection
        val grouped = variables.values.groupBy { it.variableCollectionId }

        return buildString {
            var first = true
            for ((collectionId, vars) in grouped) {
                val collection = collections[collectionId]
                val collectionName = collection?.name ?: "Unknown"
                val defaultModeId = resolveDefaultMode(collection)

                if (!first) append("\n")
                first = false

                appendLine("/* Collection: $collectionName */")
                appendLine(":root {")

                for (variable in vars.sortedBy { it.name }) {
                    val value = resolveValue(variable, defaultModeId)
                    if (value != null) {
                        val cssName = toCssName(collectionName, variable.name)
                        appendLine("  --$cssName: $value;")
                    }
                }

                appendLine("}")
            }
        }
    }

    private fun resolveDefaultMode(collection: FigmaVariableCollection?): String {
        if (collection == null) return ""
        if (collection.defaultModeId.isNotEmpty()) return collection.defaultModeId
        return collection.modes.firstOrNull()?.modeId ?: ""
    }

    internal fun resolveValue(variable: FigmaVariable, modeId: String): String? {
        val value = variable.valuesByMode[modeId]
            ?: variable.valuesByMode.values.firstOrNull()
            ?: return null

        return when {
            // Object: could be color or alias
            value is JsonObject -> {
                val obj = value.jsonObject
                if (obj.containsKey("type") && obj["type"]?.jsonPrimitive?.content == "VARIABLE_ALIAS") {
                    return null // Skip aliases
                }
                // Color object: { r, g, b, a }
                if (obj.containsKey("r") && obj.containsKey("g") && obj.containsKey("b")) {
                    val r = (obj["r"]!!.jsonPrimitive.float * 255).toInt().coerceIn(0, 255)
                    val g = (obj["g"]!!.jsonPrimitive.float * 255).toInt().coerceIn(0, 255)
                    val b = (obj["b"]!!.jsonPrimitive.float * 255).toInt().coerceIn(0, 255)
                    val a = obj["a"]?.jsonPrimitive?.float ?: 1f
                    if (a < 1f) {
                        "rgba($r, $g, $b, ${formatFloat(a)})"
                    } else {
                        "#${r.hex()}${g.hex()}${b.hex()}"
                    }
                } else null
            }
            // Number (FLOAT)
            value is JsonPrimitive && value.floatOrNull != null -> {
                val f = value.float
                when (variable.resolvedType) {
                    "FLOAT" -> "${formatFloat(f)}px"
                    else -> formatFloat(f)
                }
            }
            // String
            value is JsonPrimitive && value.isString -> {
                "\"${value.content}\""
            }
            // Boolean
            value is JsonPrimitive -> value.content
            else -> null
        }
    }

    internal fun toCssName(collectionName: String, variableName: String): String {
        // Convert "Colors/Primary" → "colors-primary"
        val full = "$collectionName/$variableName"
        return full
            .replace(Regex("[\\s/]+"), "-")
            .replace(Regex("[^a-zA-Z0-9-]"), "")
            .lowercase()
            .replace(Regex("-+"), "-")
            .trim('-')
    }

    private fun formatFloat(f: Float): String {
        return if (f == f.toLong().toFloat()) {
            f.toLong().toString()
        } else {
            "%.2f".format(f).trimEnd('0').trimEnd('.')
        }
    }

    private fun Int.hex(): String = "%02x".format(this)

    private val JsonPrimitive.floatOrNull: Float?
        get() = try { if (!isString) float else null } catch (_: Exception) { null }
}
