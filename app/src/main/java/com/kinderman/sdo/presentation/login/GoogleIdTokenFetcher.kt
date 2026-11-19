package com.kinderman.sdo.presentation.login

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential

object GoogleIdTokenFetcher {
    suspend fun fetch(activity: Activity): String {
        val resourceId = activity.resources.getIdentifier("default_web_client_id", "string", activity.packageName)
        require(resourceId != 0) { "Configuração do Google Login não encontrada." }
        val option = GetSignInWithGoogleOption.Builder(activity.getString(resourceId)).build()
        val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
        val credential = CredentialManager.create(activity).getCredential(activity, request).credential
        return GoogleIdTokenCredential.createFrom(credential.data).idToken
    }
}
