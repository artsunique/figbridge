package com.artsunique.figbridge.api

import com.artsunique.figbridge.config.FigBridgeSettings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.InetAddress
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.util.Base64

@Serializable
data class OAuthTokenResponse(
    val access_token: String,
    val expires_in: Long = 0,
    val refresh_token: String = "",
)

object FigmaOAuth {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) { json(json) }
    }

    private const val PORT = 9876
    private const val REDIRECT_URI = "http://localhost:$PORT/callback"

    fun isConfigured(): Boolean {
        val state = FigBridgeSettings.getInstance().state
        return state.oauthClientId.isNotBlank()
    }

    suspend fun startOAuthFlow(
        onSuccess: (token: String) -> Unit,
        onError: (String) -> Unit,
    ) {
        val settings = FigBridgeSettings.getInstance().state
        val clientId = settings.oauthClientId
        val clientSecret = settings.oauthClientSecret

        if (clientId.isBlank()) {
            onError("OAuth Client ID not configured. Set it in Settings.")
            return
        }

        withContext(Dispatchers.IO) {
            var server: ServerSocket? = null
            try {
                // Bind to loopback
                val loopback = InetAddress.getLoopbackAddress()
                server = ServerSocket(PORT, 1, loopback)
                server.soTimeout = 120_000

                // Build OAuth URL — NO manual URL encoding, let the OS handle it
                val authUrl = "https://www.figma.com/oauth" +
                    "?client_id=$clientId" +
                    "&redirect_uri=$REDIRECT_URI" +
                    "&scope=file_content:read,file_metadata:read,current_user:read" +
                    "&response_type=code" +
                    "&state=figbridge"

                // Open browser via OS command (avoids Java URI parsing issues)
                openUrl(authUrl)

                // Wait for callback
                val code = try {
                    waitForCallback(server)
                } catch (_: SocketTimeoutException) {
                    null
                }

                if (code == null) {
                    onError("Timeout — no response from Figma. Try using a Personal Access Token instead.")
                    return@withContext
                }

                // Exchange code for token (uses HTTP Basic Auth per Figma docs)
                val token = exchangeCode(code, clientId, clientSecret)
                if (token != null) {
                    onSuccess(token)
                } else {
                    onError("Failed to exchange authorization code for token.")
                }
            } catch (e: java.net.BindException) {
                onError("Port $PORT is in use. Close other applications and try again.")
            } catch (e: Exception) {
                onError(e.message ?: "OAuth flow failed")
            } finally {
                server?.close()
            }
        }
    }

    private suspend fun exchangeCode(
        code: String,
        clientId: String,
        clientSecret: String,
    ): String? {
        return try {
            // Figma requires HTTP Basic Auth: Base64(client_id:client_secret)
            val credentials = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())

            val response = httpClient.post("https://api.figma.com/v1/oauth/token") {
                header("Authorization", "Basic $credentials")
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(FormDataContent(Parameters.build {
                    append("redirect_uri", REDIRECT_URI)
                    append("code", code)
                    append("grant_type", "authorization_code")
                }))
            }
            if (response.status == HttpStatusCode.OK) {
                response.body<OAuthTokenResponse>().access_token
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun waitForCallback(server: ServerSocket): String? {
        val socket = server.accept()
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
        val requestLine = reader.readLine() ?: ""
        val code = extractCode(requestLine)

        val writer = PrintWriter(socket.getOutputStream(), true)
        val body = if (code != null) {
            """<html><body style="font-family:system-ui;text-align:center;padding:60px">
               <h2 style="color:#2EA043">&#10003; FigBridge connected!</h2>
               <p style="color:#666">You can close this tab.</p></body></html>"""
        } else {
            """<html><body style="font-family:system-ui;text-align:center;padding:60px">
               <h2 style="color:#c00">Connection failed</h2>
               <p>No authorization code received.</p></body></html>"""
        }
        writer.println("HTTP/1.1 200 OK")
        writer.println("Content-Type: text/html; charset=utf-8")
        writer.println("Content-Length: ${body.toByteArray().size}")
        writer.println("Connection: close")
        writer.println()
        writer.print(body)
        writer.flush()
        socket.close()

        return code
    }

    private fun extractCode(requestLine: String): String? {
        val match = Regex("code=([^&\\s]+)").find(requestLine)
        return match?.groupValues?.get(1)
    }

    /**
     * Open URL via OS command — bypasses Java's URI class which can
     * re-encode characters differently than the browser expects.
     */
    private fun openUrl(url: String) {
        val os = System.getProperty("os.name").lowercase()
        val command = when {
            os.contains("mac") -> arrayOf("open", url)
            os.contains("win") -> arrayOf("rundll32", "url.dll,FileProtocolHandler", url)
            else -> arrayOf("xdg-open", url)
        }
        Runtime.getRuntime().exec(command)
    }
}
