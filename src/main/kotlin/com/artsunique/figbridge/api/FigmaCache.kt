package com.artsunique.figbridge.api

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import java.awt.image.BufferedImage
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.APP)
class FigmaCache {

    private val ttlMs = 5 * 60 * 1000L // 5 minutes

    private val fileCache = ConcurrentHashMap<String, Pair<FigmaFileResponse, Long>>()
    private val thumbnailCache = ConcurrentHashMap<String, BufferedImage>()
    private val variablesCache = ConcurrentHashMap<String, Pair<Map<String, FigmaVariable>, Long>>()

    fun getCachedFile(fileKey: String): FigmaFileResponse? {
        val (data, timestamp) = fileCache[fileKey] ?: return null
        if (System.currentTimeMillis() - timestamp > ttlMs) {
            fileCache.remove(fileKey)
            return null
        }
        return data
    }

    fun cacheFile(fileKey: String, data: FigmaFileResponse) {
        fileCache[fileKey] = Pair(data, System.currentTimeMillis())
    }

    fun getCachedThumbnail(nodeId: String): BufferedImage? {
        return thumbnailCache[nodeId]
    }

    fun cacheThumbnail(nodeId: String, image: BufferedImage) {
        thumbnailCache[nodeId] = image
    }

    fun getCachedVariables(fileKey: String): Map<String, FigmaVariable>? {
        val (data, timestamp) = variablesCache[fileKey] ?: return null
        if (System.currentTimeMillis() - timestamp > ttlMs) {
            variablesCache.remove(fileKey)
            return null
        }
        return data
    }

    fun cacheVariables(fileKey: String, data: Map<String, FigmaVariable>) {
        variablesCache[fileKey] = Pair(data, System.currentTimeMillis())
    }

    fun invalidate() {
        fileCache.clear()
        thumbnailCache.clear()
        variablesCache.clear()
    }

    companion object {
        fun getInstance(): FigmaCache =
            ApplicationManager.getApplication().getService(FigmaCache::class.java)
    }
}
