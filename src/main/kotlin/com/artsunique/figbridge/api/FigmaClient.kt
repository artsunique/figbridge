package com.artsunique.figbridge.api

import com.artsunique.figbridge.config.AuthMethod
import com.artsunique.figbridge.config.FigBridgeSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json

sealed class FigmaResult<out T> {
    data class Success<T>(val data: T) : FigmaResult<T>()
    data class Error(val code: Int, val message: String) : FigmaResult<Nothing>()
}

@Service(Service.Level.APP)
class FigmaClient {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(this@FigmaClient.json)
        }
        install(HttpTimeout) {
            connectTimeoutMillis = 10_000
            requestTimeoutMillis = 60_000
        }
        engine {
            maxConnectionsCount = 4
            endpoint {
                keepAliveTime = 30_000
                connectTimeout = 10_000
            }
        }
    }

    private fun getToken(): String {
        return FigmaAuth.getInstance().getToken()
            ?: throw IllegalStateException("Not authenticated")
    }

    suspend fun getMe(): FigmaResult<FigmaUser> {
        return request("v1/me")
    }

    suspend fun getFile(fileKey: String): FigmaResult<FigmaFileResponse> {
        // depth=2 = pages + top-level frames only (fast)
        // geometry=paths excluded to reduce payload
        // branch_data=false to skip branch metadata
        return request("v1/files/$fileKey?depth=2&branch_data=false")
    }

    suspend fun getNodes(fileKey: String, nodeIds: List<String>): FigmaResult<FigmaNodesResponse> {
        val ids = nodeIds.joinToString(",")
        return request("v1/files/$fileKey/nodes?ids=$ids")
    }

    suspend fun getImage(
        fileKey: String,
        nodeIds: List<String>,
        format: String = "png",
        scale: Int = 2,
    ): FigmaResult<FigmaImageResponse> {
        val ids = nodeIds.joinToString(",")
        return request("v1/images/$fileKey?ids=$ids&format=$format&scale=$scale")
    }

    suspend fun getTeamProjects(teamId: String): FigmaResult<FigmaTeamProjectsResponse> {
        return request("v1/teams/$teamId/projects")
    }

    suspend fun getProjectFiles(projectId: String): FigmaResult<FigmaProjectFilesResponse> {
        return request("v1/projects/$projectId/files")
    }

    suspend fun getVariables(fileKey: String): FigmaResult<FigmaVariablesResponse> {
        return request("v1/files/$fileKey/variables/local")
    }

    private suspend inline fun <reified T> request(path: String, retries: Int = 2): FigmaResult<T> {
        var lastError: FigmaResult.Error? = null
        repeat(retries + 1) { attempt ->
            try {
                val token = getToken()
                val authMethod = FigBridgeSettings.getInstance().authMethod
                val response: HttpResponse = httpClient.get("https://api.figma.com/$path") {
                    if (authMethod == AuthMethod.OAUTH) {
                        header("Authorization", "Bearer $token")
                    } else {
                        header("X-Figma-Token", token)
                    }
                }
                when (response.status) {
                    HttpStatusCode.OK -> return FigmaResult.Success(response.body<T>())
                    HttpStatusCode.Unauthorized -> return FigmaResult.Error(401, "Invalid or expired token")
                    HttpStatusCode.Forbidden -> return FigmaResult.Error(403, "Access denied: ${response.bodyAsText()}")
                    HttpStatusCode.NotFound -> return FigmaResult.Error(404, "Resource not found")
                    HttpStatusCode.TooManyRequests -> {
                        lastError = FigmaResult.Error(429, "Rate limited")
                        val waitSec = response.headers["Retry-After"]?.toLongOrNull() ?: (2L * (attempt + 1))
                        delay(waitSec * 1000)
                    }
                    else -> return FigmaResult.Error(response.status.value, response.bodyAsText())
                }
            } catch (e: IllegalStateException) {
                return FigmaResult.Error(0, e.message ?: "Not authenticated")
            } catch (e: Exception) {
                lastError = FigmaResult.Error(0, e.message ?: "Network error")
                if (attempt < retries) delay(1000L * (attempt + 1))
            }
        }
        return lastError ?: FigmaResult.Error(0, "Request failed")
    }

    companion object {
        fun getInstance(): FigmaClient =
            ApplicationManager.getApplication().getService(FigmaClient::class.java)
    }
}
