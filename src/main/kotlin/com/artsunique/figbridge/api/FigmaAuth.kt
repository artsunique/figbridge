package com.artsunique.figbridge.api

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service

@Service(Service.Level.APP)
class FigmaAuth {

    private val credentialAttributes = CredentialAttributes(
        generateServiceName("FigBridge", "figmaAccessToken")
    )

    fun getToken(): String? {
        return PasswordSafe.instance.getPassword(credentialAttributes)
    }

    fun storeToken(token: String) {
        PasswordSafe.instance.set(credentialAttributes, Credentials("figma", token))
    }

    fun clearToken() {
        PasswordSafe.instance.set(credentialAttributes, null)
    }

    fun isAuthenticated(): Boolean {
        return !getToken().isNullOrBlank()
    }

    companion object {
        fun getInstance(): FigmaAuth =
            ApplicationManager.getApplication().getService(FigmaAuth::class.java)
    }
}
