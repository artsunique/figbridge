package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.FigmaVariable
import com.artsunique.figbridge.api.FigmaVariableCollection
import com.artsunique.figbridge.api.FigmaVariableMode
import kotlinx.serialization.json.*

object TokenImporter {

    data class ImportResult(
        val variables: Map<String, FigmaVariable>,
        val collections: Map<String, FigmaVariableCollection>,
    )

    fun parseJson(jsonString: String): ImportResult {
        val json = Json { ignoreUnknownKeys = true; isLenient = true }
        val root = json.parseToJsonElement(jsonString).jsonObject

        // Format 1: Figma REST API format
        if (root.containsKey("meta")) {
            return parseRestApiFormat(root)
        }

        // Format 2: Collections array
        if (root.containsKey("collections")) {
            return parseCollectionsFormat(root)
        }

        // Format 3: Oppermann Design Tokens plugin / nested / flat
        return parseNestedFormat(root)
    }

    private fun parseRestApiFormat(root: JsonObject): ImportResult {
        val meta = root["meta"]?.jsonObject ?: return empty()
        val varsObj = meta["variables"]?.jsonObject ?: return empty()
        val collectionsObj = meta["variableCollections"]?.jsonObject ?: return empty()

        val variables = mutableMapOf<String, FigmaVariable>()
        val collections = mutableMapOf<String, FigmaVariableCollection>()

        for ((id, collJson) in collectionsObj) {
            val obj = collJson.jsonObject
            val modes = obj["modes"]?.jsonArray?.map { m ->
                val mObj = m.jsonObject
                FigmaVariableMode(
                    modeId = mObj["modeId"]?.jsonPrimitive?.content ?: "",
                    name = mObj["name"]?.jsonPrimitive?.content ?: "",
                )
            } ?: emptyList()
            collections[id] = FigmaVariableCollection(
                id = id,
                name = obj["name"]?.jsonPrimitive?.content ?: id,
                modes = modes,
                defaultModeId = obj["defaultModeId"]?.jsonPrimitive?.content ?: "",
            )
        }

        for ((id, varJson) in varsObj) {
            val obj = varJson.jsonObject
            val valuesByMode = obj["valuesByMode"]?.jsonObject?.toMap() ?: emptyMap()
            variables[id] = FigmaVariable(
                id = id,
                name = obj["name"]?.jsonPrimitive?.content ?: id,
                resolvedType = obj["resolvedType"]?.jsonPrimitive?.content ?: "",
                variableCollectionId = obj["variableCollectionId"]?.jsonPrimitive?.content ?: "",
                valuesByMode = valuesByMode,
            )
        }

        return ImportResult(variables, collections)
    }

    private fun parseCollectionsFormat(root: JsonObject): ImportResult {
        val collectionsArray = root["collections"]?.jsonArray ?: return empty()
        val variables = mutableMapOf<String, FigmaVariable>()
        val collections = mutableMapOf<String, FigmaVariableCollection>()

        for ((idx, collElement) in collectionsArray.withIndex()) {
            val collObj = collElement.jsonObject
            val collId = "c$idx"
            val collName = collObj["name"]?.jsonPrimitive?.content ?: "Collection $idx"
            val modeId = "mode0"

            collections[collId] = FigmaVariableCollection(
                id = collId, name = collName,
                modes = listOf(FigmaVariableMode(modeId, "Default")),
                defaultModeId = modeId,
            )

            val vars = collObj["variables"]?.jsonArray ?: continue
            for ((vIdx, varElement) in vars.withIndex()) {
                val varObj = varElement.jsonObject
                val varId = "v${idx}_$vIdx"
                val varName = varObj["name"]?.jsonPrimitive?.content ?: "var$vIdx"
                val varType = varObj["type"]?.jsonPrimitive?.content ?: detectType(varObj["value"])

                val value = varObj["value"] ?: continue
                val valuesByMode = mapOf(modeId to value)

                variables[varId] = FigmaVariable(
                    id = varId, name = varName,
                    resolvedType = varType, variableCollectionId = collId,
                    valuesByMode = valuesByMode,
                )
            }
        }

        return ImportResult(variables, collections)
    }

    /**
     * Parses Lukas Oppermann Design Tokens plugin format AND simple flat format.
     *
     * Oppermann format:
     * {
     *   "tw colors": {          // ← collection (top-level key)
     *     "slate": {            // ← group
     *       "50": { "type": "color", "value": "#f8fafcff" },  // ← token
     *       "100": { "type": "color", "value": "#f1f5f9ff" }
     *     }
     *   },
     *   "tw spacing": {
     *     "spacing": {
     *       "0": { "type": "dimension", "value": 0 }
     *     }
     *   }
     * }
     *
     * Flat format:
     * { "Colors": { "Primary": "#3b82f6" }, "Spacing": { "sm": 8 } }
     */
    private fun parseNestedFormat(root: JsonObject): ImportResult {
        val variables = mutableMapOf<String, FigmaVariable>()
        val collections = mutableMapOf<String, FigmaVariableCollection>()
        val modeId = "mode0"
        var varCounter = 0

        for ((collIdx, entry) in root.entries.withIndex()) {
            val (collName, collValue) = entry
            if (collValue !is JsonObject) continue

            val collId = "c$collIdx"
            collections[collId] = FigmaVariableCollection(
                id = collId, name = collName,
                modes = listOf(FigmaVariableMode(modeId, "Default")),
                defaultModeId = modeId,
            )

            // Recursively walk the nested structure to find tokens
            walkTokens(collValue.jsonObject, "", collId, modeId, variables, varCounter)
                .also { varCounter = it }
        }

        return ImportResult(variables, collections)
    }

    /**
     * Recursively walks a JSON object tree to find tokens.
     * A token is identified by having a "type" and "value" field.
     * Returns updated varCounter.
     */
    private fun walkTokens(
        obj: JsonObject,
        prefix: String,
        collectionId: String,
        modeId: String,
        variables: MutableMap<String, FigmaVariable>,
        startCounter: Int,
    ): Int {
        var counter = startCounter

        for ((key, element) in obj) {
            if (key == "description" || key == "extensions") continue
            if (element !is JsonObject) {
                // Flat value (e.g. "Primary": "#3b82f6" or "sm": 8)
                val varName = if (prefix.isEmpty()) key else "$prefix/$key"
                val value = normalizeValue(element)
                val varType = detectType(value)
                val varId = "v$counter"
                counter++

                variables[varId] = FigmaVariable(
                    id = varId, name = varName,
                    resolvedType = mapResolvedType(varType),
                    variableCollectionId = collectionId,
                    valuesByMode = mapOf(modeId to value),
                )
                continue
            }

            val childObj = element.jsonObject

            // Check if this is a token node (has "type" + "value")
            if (childObj.containsKey("type") && childObj.containsKey("value")) {
                val tokenType = childObj["type"]?.jsonPrimitive?.content ?: ""
                val rawValue = childObj["value"] ?: continue

                // Skip complex custom types we can't represent as CSS
                if (tokenType.startsWith("custom-") && tokenType != "custom-shadow") {
                    // Extract useful sub-values from custom types
                    val extracted = extractCustomTypeValues(tokenType, rawValue, prefix, key, collectionId, modeId, counter)
                    if (extracted != null) {
                        variables.putAll(extracted.first)
                        counter = extracted.second
                    }
                    continue
                }

                val varName = if (prefix.isEmpty()) key else "$prefix/$key"
                val value = normalizeTokenValue(tokenType, rawValue)
                val varId = "v$counter"
                counter++

                variables[varId] = FigmaVariable(
                    id = varId, name = varName,
                    resolvedType = mapResolvedType(tokenType),
                    variableCollectionId = collectionId,
                    valuesByMode = mapOf(modeId to value),
                )
            } else {
                // It's a group — recurse deeper
                val newPrefix = if (prefix.isEmpty()) key else "$prefix/$key"
                counter = walkTokens(childObj, newPrefix, collectionId, modeId, variables, counter)
            }
        }

        return counter
    }

    /**
     * Extracts useful sub-values from custom types like custom-fontStyle.
     */
    private fun extractCustomTypeValues(
        tokenType: String,
        rawValue: JsonElement,
        prefix: String,
        key: String,
        collectionId: String,
        modeId: String,
        startCounter: Int,
    ): Pair<Map<String, FigmaVariable>, Int>? {
        if (rawValue !is JsonObject) return null
        val vars = mutableMapOf<String, FigmaVariable>()
        var counter = startCounter
        val baseName = if (prefix.isEmpty()) key else "$prefix/$key"

        when (tokenType) {
            "custom-fontStyle" -> {
                val valObj = rawValue.jsonObject
                val pairs = listOfNotNull(
                    valObj["fontSize"]?.let { "font-size" to it },
                    valObj["lineHeight"]?.let { "line-height" to it },
                    valObj["fontWeight"]?.let { "font-weight" to it },
                    valObj["letterSpacing"]?.let { "letter-spacing" to it },
                    valObj["fontFamily"]?.let { "font-family" to it },
                )
                for ((subName, subValue) in pairs) {
                    val varId = "v$counter"
                    counter++
                    val value = normalizeValue(subValue)
                    val type = if (subName == "font-family") "STRING"
                        else if (subName == "font-weight") "FLOAT"
                        else "FLOAT"
                    vars[varId] = FigmaVariable(
                        id = varId, name = "$baseName/$subName",
                        resolvedType = type, variableCollectionId = collectionId,
                        valuesByMode = mapOf(modeId to value),
                    )
                }
            }
        }

        return if (vars.isEmpty()) null else vars to counter
    }

    private fun normalizeTokenValue(tokenType: String, rawValue: JsonElement): JsonElement {
        return when (tokenType) {
            "color" -> normalizeValue(rawValue)
            "dimension" -> {
                // dimension values can be numbers or strings like "16px"
                if (rawValue is JsonPrimitive) {
                    if (!rawValue.isString) rawValue
                    else {
                        val str = rawValue.content.replace("px", "").replace("rem", "")
                        str.toFloatOrNull()?.let { JsonPrimitive(it) } ?: rawValue
                    }
                } else rawValue
            }
            else -> normalizeValue(rawValue)
        }
    }

    private fun normalizeValue(element: JsonElement): JsonElement {
        if (element is JsonPrimitive && element.isString) {
            val str = element.content
            // Hex color "#3b82f6" or "#3b82f6ff"
            val hexMatch = Regex("^#([0-9a-fA-F]{6})([0-9a-fA-F]{2})?$").find(str)
            if (hexMatch != null) {
                val hex = hexMatch.groupValues[1]
                val r = Integer.parseInt(hex.substring(0, 2), 16) / 255f
                val g = Integer.parseInt(hex.substring(2, 4), 16) / 255f
                val b = Integer.parseInt(hex.substring(4, 6), 16) / 255f
                val a = if (hexMatch.groupValues[2].isNotEmpty()) {
                    Integer.parseInt(hexMatch.groupValues[2], 16) / 255f
                } else 1f
                return buildJsonObject {
                    put("r", r); put("g", g); put("b", b); put("a", a)
                }
            }
            // "16px" → 16
            val pxMatch = Regex("^([0-9.]+)px$").find(str)
            if (pxMatch != null) {
                pxMatch.groupValues[1].toFloatOrNull()?.let { return JsonPrimitive(it) }
            }
        }
        return element
    }

    private fun mapResolvedType(tokenType: String): String {
        return when (tokenType.lowercase()) {
            "color" -> "COLOR"
            "dimension", "number", "float" -> "FLOAT"
            "string" -> "STRING"
            else -> "STRING"
        }
    }

    private fun detectType(element: JsonElement?): String {
        return when {
            element == null -> "STRING"
            element is JsonObject && element.containsKey("r") -> "COLOR"
            element is JsonPrimitive && !element.isString -> {
                try { element.float; "FLOAT" } catch (_: Exception) { "STRING" }
            }
            element is JsonPrimitive && element.isString -> {
                if (element.content.startsWith("#")) "COLOR" else "STRING"
            }
            else -> "STRING"
        }
    }

    private fun empty() = ImportResult(emptyMap(), emptyMap())
}
