// SPDX-FileCopyrightText: 2026 Oleh Yurkov
// SPDX-License-Identifier: Apache-2.0
package dev.pushport.kmp

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** APNs environment must match the application's signing entitlement. */
public enum class AppleEnvironment { SANDBOX, PRODUCTION }

/** The native SDK persists and validates this binding. One installation per application/origin. */
public data class PushPortConfiguration(
    public val appId: String,
    public val serverUrl: String = "https://pushport.dev",
    public val appleEnvironment: AppleEnvironment = AppleEnvironment.PRODUCTION,
    public val consentRequired: Boolean? = null,
    public val consentGiven: Boolean? = null,
) {
    internal fun validate() {
        require(Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}").matches(appId)) {
            "Use the canonical App ID from PushPort"
        }
        require(serverUrl.startsWith("https://") && serverUrl.length <= 2048) { "Use an HTTPS PushPort server URL" }
    }
}

/** Credentials and raw platform push tokens are intentionally absent from the shared status. */
@Serializable
public data class PushPortStatus(
    public val configured: Boolean = false,
    public val installationId: String? = null,
    public val subscribed: Boolean = true,
    public val hasToken: Boolean = false,
    public val notificationsEnabled: Boolean = false,
    public val locale: String? = null,
    public val lastSyncedAt: Long? = null,
    public val syncError: String? = null,
)

/** Native adapter boundary. Applications normally use the platform factory functions. */
public interface PushPortDriver {
    public suspend fun initialize(configuration: PushPortConfiguration)
    public fun status(): PushPortStatus
    public fun sync()
    public suspend fun setSubscribed(value: Boolean)
    public suspend fun setLocale(languageTag: String?)
    public suspend fun requestPermission()
    public fun userJSON(): String? = null
    public suspend fun updateUserJSON(value: String): Unit = throw UnsupportedOperationException("Update the native PushPort SDK")
    public suspend fun setConsentRequired(value: Boolean): Unit = throw UnsupportedOperationException("Update the native PushPort SDK")
    public suspend fun setConsentGiven(value: Boolean): Unit = throw UnsupportedOperationException("Update the native PushPort SDK")
}

/**
 * Common Kotlin facade. Native SDKs own permission handling, storage, networking and delivery.
 * Call from the UI/main thread on Apple/Android. Successful mutations confirm local scheduling;
 * inspect [status] for the last acknowledged server synchronization.
 */
public class PushPort(private val driver: PushPortDriver) {
    private var configuration: PushPortConfiguration? = null
    private val initialization = Mutex()

    public suspend fun initialize(configuration: PushPortConfiguration) {
        initialization.withLock {
            configuration.validate()
            require(this.configuration == null || this.configuration == configuration) { "PushPort is already bound to another configuration" }
            driver.initialize(configuration)
            this.configuration = configuration
        }
    }
    public fun status(): PushPortStatus = driver.status()
    public fun sync() { initialized(); driver.sync() }
    public suspend fun setSubscribed(value: Boolean) { initialized(); driver.setSubscribed(value) }
    public suspend fun setLocale(languageTag: String?) {
        initialized()
        require(languageTag == null || (languageTag.length <= 100 && languageTag.lowercase() != "und" &&
            Regex("[A-Za-z]{2,8}(-[A-Za-z0-9]{1,8})*").matches(languageTag))) { "Use a BCP-47 locale such as de-DE" }
        driver.setLocale(languageTag)
    }
    /** Must be invoked from a user action. Android needs the current foreground Activity. */
    public suspend fun requestPermission() { initialized(); driver.requestPermission() }
    public fun user(): PushPortUser? = driver.userJSON()?.let { BridgeStatus.json.decodeFromString(it) }
    public suspend fun setConsentRequired(value: Boolean) { initialized(); driver.setConsentRequired(value) }
    public suspend fun setConsentGiven(value: Boolean) { initialized(); driver.setConsentGiven(value) }
    public suspend fun setTags(tags: Map<String, String?>) {
        require(tags.size <= 50 && tags.all { (key, value) -> key.isNotBlank() && key.length <= 64 && key.none(Char::isISOControl) && (value?.length ?: 0) <= 256 }) { "Invalid tags" }
        mutate(UserOperation(kind = "tags", tags = tags.toMap()))
    }
    public suspend fun login(externalId: String, identityToken: String) {
        require(externalId.isNotBlank() && externalId == externalId.trim() && externalId.length <= 128 && externalId.none(Char::isISOControl) && Regex("[A-Za-z0-9_-]{43}").matches(identityToken)) { "Use an External ID and a short-lived identity proof" }
        mutate(UserOperation(kind = "login", externalId = externalId, identityToken = identityToken))
    }
    public suspend fun logout(): Unit = mutate(UserOperation(kind = "logout"))
    public suspend fun setEmail(email: String?) {
        require(email == null || (email.length <= 254 && Regex("[^\\s@]+@[^\\s@]+\\.[^\\s@]+").matches(email))) { "Invalid email" }
        mutate(UserOperation(kind = "email", email = email))
    }
    public suspend fun setPhoneNumber(phoneNumber: String?) {
        require(phoneNumber == null || Regex("\\+[1-9][0-9]{6,14}").matches(phoneNumber)) { "Use E.164 phone format" }
        mutate(UserOperation(kind = "phone", phoneNumber = phoneNumber))
    }
    public suspend fun setLocation(latitude: Double?, longitude: Double?) {
        require((latitude == null) == (longitude == null) && (latitude == null || (latitude.isFinite() && latitude in -90.0..90.0)) && (longitude == null || (longitude.isFinite() && longitude in -180.0..180.0))) { "Invalid coordinates" }
        mutate(UserOperation(kind = "location", latitude = latitude, longitude = longitude))
    }
    public suspend fun trackEvent(name: String, properties: Map<String, String> = emptyMap()) {
        require(Regex("[A-Za-z][A-Za-z0-9_.-]{0,63}").matches(name) && properties.size <= 20 && properties.all { (key, value) -> key.isNotBlank() && key.length <= 64 && key.none(Char::isISOControl) && value.length <= 256 }) { "Invalid event" }
        mutate(UserOperation(kind = "event", eventName = name, eventProperties = properties.toMap()))
    }
    private suspend fun mutate(operation: UserOperation) { initialized(); driver.updateUserJSON(BridgeStatus.json.encodeToString(operation)) }
    private fun initialized() { check(configuration != null) { "Initialize PushPort first" } }
}

internal object BridgeStatus {
    val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    fun decode(value: String): PushPortStatus = json.decodeFromString(value)
}

@Serializable
public data class PushPortUser(public val userId: String, public val externalId: String? = null,
    public val properties: UserProperties = UserProperties(), public val revision: Long = 0)
@Serializable
public data class UserProperties(public val tags: Map<String, String> = emptyMap(), public val email: String? = null,
    public val phoneNumber: String? = null, public val latitude: Double? = null, public val longitude: Double? = null)
@Serializable
internal data class UserOperation(val revision: Long = 0, val kind: String, val externalId: String? = null,
    val identityToken: String? = null, val tags: Map<String, String?> = emptyMap(), val email: String? = null,
    val phoneNumber: String? = null, val latitude: Double? = null, val longitude: Double? = null,
    val eventName: String? = null, val eventProperties: Map<String, String> = emptyMap())
