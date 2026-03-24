package com.artsunique.figbridge.generator

import com.artsunique.figbridge.api.FigmaClient
import com.artsunique.figbridge.api.FigmaResult
import java.io.File
import java.net.URI

object AssetExporter {

    data class ExportResult(
        val saved: Int,
        val skipped: Int,
        val errors: List<String>,
    )

    suspend fun export(
        fileKey: String,
        assets: List<AssetInfo>,
        projectDir: File,
    ): ExportResult {
        if (assets.isEmpty()) return ExportResult(0, 0, emptyList())

        var saved = 0
        var skipped = 0
        val errors = mutableListOf<String>()

        // Group by format (one API call per format)
        val byFormat = assets.groupBy { it.format }

        for ((format, group) in byFormat) {
            // Skip assets that already exist
            val toExport = group.filter { asset ->
                val file = File(projectDir, asset.relativePath)
                if (file.exists()) {
                    skipped++
                    false
                } else {
                    true
                }
            }
            if (toExport.isEmpty()) continue

            // Batch API call: get download URLs for all nodes of this format
            val nodeIds = toExport.map { it.nodeId }
            val result = FigmaClient.getInstance().getImage(
                fileKey, nodeIds, format = format, scale = 2,
            )

            when (result) {
                is FigmaResult.Success -> {
                    for (asset in toExport) {
                        val url = result.data.images[asset.nodeId]
                        if (url == null) {
                            errors += "No URL for ${asset.fileName}"
                            continue
                        }
                        try {
                            val outFile = File(projectDir, asset.relativePath)
                            outFile.parentFile.mkdirs()
                            URI(url).toURL().openStream().use { input ->
                                outFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            saved++
                        } catch (e: Exception) {
                            errors += "${asset.fileName}: ${e.message}"
                        }
                    }
                }
                is FigmaResult.Error -> {
                    errors += "API error ($format): ${result.message}"
                }
            }
        }

        return ExportResult(saved, skipped, errors)
    }
}
