package com.example.nativeaccent.assessment

/** What the speech service needs to authenticate one request. */
sealed interface SpeechCredentials {
    val region: String

    /** A long-lived resource key. Fine for development; extractable from a shipped APK. */
    class SubscriptionKey(val key: String, override val region: String) : SpeechCredentials {
        override fun toString() = "SubscriptionKey(region=$region, key=<redacted>)"
    }

    /** A short-lived (~10 min) token minted by a backend that holds the real key. */
    class AuthorizationToken(val token: String, override val region: String) : SpeechCredentials {
        override fun toString() = "AuthorizationToken(region=$region, token=<redacted>)"
    }
}

/**
 * Where credentials come from. Swapping the embedded key for a backend token
 * endpoint means writing a new provider — no change to the Azure service or UI.
 *
 * TODO(backend-auth): add a TokenEndpointCredentialsProvider that calls our own
 *  server (which holds the key and calls Azure's issueToken endpoint), caches the
 *  token for ~9 minutes, and returns [SpeechCredentials.AuthorizationToken].
 */
interface SpeechCredentialsProvider {
    /** Returns null when no credentials are configured. */
    suspend fun credentials(): SpeechCredentials?
}

/** v1: key and region compiled in from local.properties via BuildConfig. */
class EmbeddedKeyCredentialsProvider(
    private val key: String,
    private val region: String,
) : SpeechCredentialsProvider {
    override suspend fun credentials(): SpeechCredentials? =
        if (key.isBlank() || region.isBlank()) null
        else SpeechCredentials.SubscriptionKey(key.trim(), region.trim())
}
