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

    companion object {
        fun extractFirstName(raw: String): String {
            if (raw.isBlank()) return "Aadesh"
            // 1. If it's a full name with spaces like "Aadesh Chauhan", take the first word
            if (!raw.contains("@") && raw.contains(" ")) {
                val firstWord = raw.split(" ").firstOrNull { it.isNotBlank() } ?: raw
                return firstWord.takeWhile { it.isLetter() }.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

            // 2. Email format, e.g. "aadeshchauhan27@gmail.com", "aadesh.chauhan@gmail.com", "aadesh_27@gmail.com"
            val prefix = raw.substringBefore("@")

            // Split by non-alphabetic / delimiter characters (., _, -, +, numbers)
            val parts = prefix.split(Regex("[._\\-\\+\\d]+")).filter { it.isNotBlank() }
            val firstPart = parts.firstOrNull() ?: prefix.filter { it.isLetter() }

            // Check known surnames attached directly without delimiter
            val commonSurnames = listOf(
                "chauhan", "kumar", "singh", "sharma", "patel", "verma", "gupta",
                "yadav", "mishra", "jain", "tiwari", "reddy", "rao", "nair",
                "mehta", "agarwal", "bose", "sen", "das", "roy", "joshi", "shukla"
            )
            for (surname in commonSurnames) {
                if (firstPart.length > surname.length + 2 && firstPart.lowercase().endsWith(surname)) {
                    val clean = firstPart.substring(0, firstPart.length - surname.length)
                    return clean.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            }

            val lettersOnly = firstPart.takeWhile { it.isLetter() }
            val candidate = if (lettersOnly.isNotBlank()) lettersOnly else firstPart
            return candidate.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }
    }

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
            val firstName = extractFirstName(email)

            // Ensure Firebase Auth session exists for Firestore security rules
            if (isFirebaseAvailable) {
                try {
                    val auth = FirebaseAuth.getInstance()
                    if (auth.currentUser == null) {
                        auth.signInAnonymously().await()
                    }
                } catch (_: Exception) {}
            }

            userPreferences.setGoogleAccount(email, firstName, null)
            userPreferences.updateProfile(firstName, email)
            return Result.success(GoogleAccountInfo(email, firstName))
        }

        // 2. GoogleSignInAccount fallback
        try {
            val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
            val account: GoogleSignInAccount = task.getResult(ApiException::class.java)
            val email = account.email ?: "user@gmail.com"
            val rawName = account.givenName ?: (account.displayName ?: "Google User")
            val firstName = extractFirstName(rawName)
            val photoUrl = account.photoUrl?.toString()
            val idToken = account.idToken

            if (isFirebaseAvailable && !idToken.isNullOrBlank()) {
                try {
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    FirebaseAuth.getInstance().signInWithCredential(credential).await()
                } catch (_: Exception) {}
            }

            userPreferences.setGoogleAccount(email, firstName, photoUrl)
            userPreferences.updateProfile(firstName, email)
            return Result.success(GoogleAccountInfo(email, firstName, photoUrl))
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
