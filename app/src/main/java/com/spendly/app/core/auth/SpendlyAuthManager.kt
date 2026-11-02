package com.spendly.app.core.auth

import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.spendly.app.core.preferences.UserPreferences
import kotlinx.coroutines.tasks.await

data class GoogleAccountInfo(
    val email: String,
    val displayName: String,
    val photoUrl: String? = null
)

class SpendlyAuthManager(
    private val context: Context,
    private val userPreferences: UserPreferences
) {

    private val isFirebaseAvailable: Boolean
        get() = try {
            FirebaseApp.getApps(context).isNotEmpty()
        } catch (_: Exception) {
            false
        }

    fun getGoogleSignInIntent(): Intent {
        // Native Android Google Account Picker - works reliably without OAuth developer error 10
        return AccountManager.newChooseAccountIntent(
            null,
            null,
            arrayOf("com.google"),
            null,
            null,
            null,
            null
        )
    }

    suspend fun handleAccountPickerResult(intent: Intent?): Result<GoogleAccountInfo> {
        if (intent == null) {
            return Result.failure(Exception("Sign-in cancelled"))
        }

        // 1. AccountManager native Google Account result
        val accountName = intent.getStringExtra(AccountManager.KEY_ACCOUNT_NAME)
        if (!accountName.isNullOrBlank()) {
            val email = accountName.trim()
            val cleanName = email.substringBefore("@")
                .replace(".", " ")
                .replace("_", " ")
                .replace("-", " ")
                .split(" ")
                .filter { it.isNotBlank() }
                .joinToString(" ") { part ->
                    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                .ifBlank { "Google User" }

            // Ensure Firebase Auth session exists for Firestore security rules
            if (isFirebaseAvailable) {
                try {
                    val auth = FirebaseAuth.getInstance()
                    if (auth.currentUser == null) {
                        auth.signInAnonymously().await()
                    }
                } catch (_: Exception) {}
            }

            userPreferences.setGoogleAccount(email, cleanName, null)
            return Result.success(GoogleAccountInfo(email, cleanName))
        }

        // 2. GoogleSignInAccount fallback
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = account.email ?: "user@gmail.com"
            val name = account.displayName ?: (account.givenName ?: "Google User")
            val photoUrl = account.photoUrl?.toString()
            val idToken = account.idToken

            if (isFirebaseAvailable && !idToken.isNullOrBlank()) {
                try {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).await()
                } catch (_: Exception) {}
            }

            userPreferences.setGoogleAccount(email, name, photoUrl)
            return Result.success(GoogleAccountInfo(email, name, photoUrl))
        } catch (_: Exception) {}

        return Result.failure(Exception("Could not retrieve Google account"))
    }

    suspend fun signOut() {
        if (isFirebaseAvailable) {
            try {
                FirebaseAuth.getInstance().signOut()
            } catch (_: Exception) {}
        }
        userPreferences.signOutGoogle()
    }
}
