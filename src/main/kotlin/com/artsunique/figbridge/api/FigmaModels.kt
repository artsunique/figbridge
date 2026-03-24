package com.artsunique.figbridge.api

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class FigmaUser(
    val id: String,
    val handle: String,
    val email: String,
    @SerialName("img_url") val imgUrl: String = "",
)

@Serializable
data class FigmaFileResponse(
    val name: String,
    val document: FigmaDocument,
    val lastModified: String = "",
    val version: String = "",
    val thumbnailUrl: String = "",
    val styles: Map<String, FigmaStyle> = emptyMap(),
)

@Serializable
data class FigmaDocument(
    val id: String = "0:0",
    val name: String = "Document",
    val type: String = "DOCUMENT",
    val children: List<FigmaNode> = emptyList(),
)

@Serializable
data class FigmaNode(
    val id: String,
    val name: String,
    val type: String,
    val children: List<FigmaNode> = emptyList(),
    val absoluteBoundingBox: BoundingBox? = null,
    val fills: List<Paint> = emptyList(),
    val strokes: List<Paint> = emptyList(),
    val strokeWeight: Float? = null,
    val cornerRadius: Float? = null,
    val layoutMode: String? = null,
    val primaryAxisAlignItems: String? = null,
    val counterAxisAlignItems: String? = null,
    val paddingLeft: Float? = null,
    val paddingRight: Float? = null,
    val paddingTop: Float? = null,
    val paddingBottom: Float? = null,
    val itemSpacing: Float? = null,
    val characters: String? = null,
    val style: TypeStyle? = null,
    val effects: List<Effect> = emptyList(),
    val constraints: LayoutConstraint? = null,
    val clipsContent: Boolean? = null,
    val visible: Boolean = true,
    val opacity: Float? = null,
    val styles: Map<String, String>? = null, // e.g. {"fill": "1:2", "stroke": "3:4", "text": "5:6"}
    // Sizing constraints
    val minWidth: Float? = null,
    val maxWidth: Float? = null,
    val minHeight: Float? = null,
    val maxHeight: Float? = null,
    // Layout sizing mode: FIXED, HUG, FILL
    val layoutSizingHorizontal: String? = null,
    val layoutSizingVertical: String? = null,
    // Child layout properties
    val layoutAlign: String? = null, // INHERIT, STRETCH, MIN, CENTER, MAX
    val layoutGrow: Float? = null, // 0 or 1
    val layoutPositioning: String? = null, // AUTO, ABSOLUTE
    // Wrap mode (Figma auto layout wrap)
    val layoutWrap: String? = null, // NO_WRAP, WRAP
    // Counter axis spacing (cross-axis gap for wrapped layouts)
    val counterAxisSpacing: Float? = null,
    // Counter axis alignment for multi-line (wrap) layouts
    val counterAxisAlignContent: String? = null, // AUTO, SPACE_BETWEEN
)

@Serializable
data class BoundingBox(
    val x: Float = 0f,
    val y: Float = 0f,
    val width: Float = 0f,
    val height: Float = 0f,
)

@Serializable
data class Paint(
    val type: String = "SOLID",
    val color: Color? = null,
    val opacity: Float = 1f,
    val visible: Boolean = true,
    val imageRef: String? = null,
    val scaleMode: String? = null,
    val boundVariables: Map<String, VariableAlias>? = null,
)

@Serializable
data class VariableAlias(
    val type: String = "",
    val id: String = "",
)

@Serializable
data class Color(
    val r: Float = 0f,
    val g: Float = 0f,
    val b: Float = 0f,
    val a: Float = 1f,
)

@Serializable
data class TypeStyle(
    val fontFamily: String = "",
    val fontWeight: Int = 400,
    val fontSize: Float = 16f,
    val lineHeightPx: Float? = null,
    val letterSpacing: Float? = null,
    val textAlignHorizontal: String? = null,
    val textAlignVertical: String? = null,
)

@Serializable
data class Effect(
    val type: String = "",
    val visible: Boolean = true,
    val radius: Float? = null,
    val color: Color? = null,
    val offset: Vector? = null,
)

@Serializable
data class Vector(
    val x: Float = 0f,
    val y: Float = 0f,
)

@Serializable
data class LayoutConstraint(
    val vertical: String = "TOP",
    val horizontal: String = "LEFT",
)

@Serializable
data class FigmaStyle(
    val key: String = "",
    val name: String = "",
    val styleType: String = "", // FILL, TEXT, EFFECT, GRID
    val description: String = "",
)

// API response wrappers

@Serializable
data class FigmaNodesResponse(
    val nodes: Map<String, NodeData>,
)

@Serializable
data class NodeData(
    val document: FigmaNode,
)

@Serializable
data class FigmaImageResponse(
    val images: Map<String, String?>,
)

// Team/Project browsing

@Serializable
data class FigmaProject(
    val id: String,
    val name: String,
)

@Serializable
data class FigmaTeamProjectsResponse(
    val projects: List<FigmaProject>,
)

@Serializable
data class FigmaProjectFile(
    val key: String,
    val name: String,
    @SerialName("thumbnail_url") val thumbnailUrl: String = "",
    @SerialName("last_modified") val lastModified: String = "",
)

@Serializable
data class FigmaProjectFilesResponse(
    val files: List<FigmaProjectFile>,
)

// Variables API

@Serializable
data class FigmaVariablesResponse(
    val meta: VariablesMeta,
)

@Serializable
data class VariablesMeta(
    val variables: Map<String, FigmaVariable> = emptyMap(),
    val variableCollections: Map<String, FigmaVariableCollection> = emptyMap(),
)

@Serializable
data class FigmaVariable(
    val id: String,
    val name: String,
    val resolvedType: String = "",
    val variableCollectionId: String = "",
    val valuesByMode: Map<String, JsonElement> = emptyMap(),
)

@Serializable
data class FigmaVariableMode(
    val modeId: String,
    val name: String,
)

@Serializable
data class FigmaVariableCollection(
    val id: String,
    val name: String,
    val modes: List<FigmaVariableMode> = emptyList(),
    val defaultModeId: String = "",
)
