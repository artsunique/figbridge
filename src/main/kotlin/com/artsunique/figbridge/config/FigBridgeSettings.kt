package com.artsunique.figbridge.config

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

enum class CodeMode { TAILWIND, CUSTOM_CSS }
enum class AuthMethod { NONE, PAT, OAUTH }

data class RecentFile(
    var fileKey: String = "",
    var name: String = "",
    var thumbnailUrl: String = "",
    var lastOpened: Long = 0,
)

@Service(Service.Level.APP)
@State(name = "FigBridgeSettings", storages = [Storage("FigBridgeSettings.xml")])
class FigBridgeSettings : PersistentStateComponent<FigBridgeSettings.State> {

    data class State(
        var authMethod: AuthMethod = AuthMethod.NONE,
        var codeMode: CodeMode = CodeMode.CUSTOM_CSS,
        var assetDir: String = "images",
        var iconDir: String = "icons",
        var tokenFile: String = "tokens.css",
        var lastFileKey: String = "",
        var recentFiles: MutableList<RecentFile> = mutableListOf(),
        var oauthClientId: String = DEFAULT_CLIENT_ID,
        var oauthClientSecret: String = DEFAULT_CLIENT_SECRET,
        var trialStart: String = "",
        var lastTrialNotifyDay: Int = -1,
    )

    private var state = State()

    override fun getState(): State = state

    override fun loadState(state: State) {
        this.state = state
    }

    var authMethod: AuthMethod
        get() = state.authMethod
        set(value) { state.authMethod = value }

    var codeMode: CodeMode
        get() = state.codeMode
        set(value) { state.codeMode = value }

    fun removeRecentFile(fileKey: String) {
        state.recentFiles.removeAll { it.fileKey == fileKey }
    }

    fun addRecentFile(fileKey: String, name: String, thumbnailUrl: String) {
        val recent = state.recentFiles
        recent.removeAll { it.fileKey == fileKey }
        recent.add(0, RecentFile(fileKey, name, thumbnailUrl, System.currentTimeMillis()))
        if (recent.size > 20) {
            recent.subList(20, recent.size).clear()
        }
    }

    companion object {
        const val DEFAULT_CLIENT_ID = "rPEaoZIoMKFo89oENin7LH"
        const val DEFAULT_CLIENT_SECRET = "fmbQT5yzLE67pBcRgiDl3PSmd6N7YX"

        fun getInstance(): FigBridgeSettings =
            ApplicationManager.getApplication().getService(FigBridgeSettings::class.java)
    }
}
