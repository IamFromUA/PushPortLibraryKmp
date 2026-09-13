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
    private fun initialized() { check(configuration != null) { "Initialize PushPort first" } }
}

internal object BridgeStatus {
    private val json = Json { ignoreUnknownKeys = true }
    fun decode(value: String): PushPortStatus = json.decodeFromString(value)
}
