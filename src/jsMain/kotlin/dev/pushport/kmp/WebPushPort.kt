package dev.pushport.kmp

import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Pass the imported @pushport/web-sdk module (or window.PushPortWeb for a script build).
 * The worker must be served from the same origin. This target is Kotlin/JS, not Wasm.
 */
public fun createWebPushPort(sdkModule: dynamic, serviceWorkerUrl: String = "/pushport-sw.js"): PushPort =
    PushPort(WebDriver(sdkModule, serviceWorkerUrl))

private class WebDriver(private val module: dynamic, private val worker: String) : PushPortDriver {
    private var client: dynamic = null
    override suspend fun initialize(configuration: PushPortConfiguration) {
        if (client != null) return
        val options = js("({})")
        options.appId = configuration.appId
        options.serverUrl = configuration.serverUrl
        options.serviceWorkerUrl = worker
        configuration.consentRequired?.let { options.consentRequired = it }
        configuration.consentGiven?.let { options.consentGiven = it }
        client = (module.PushPort.initialize(options) as Promise<dynamic>).await()
    }
    override fun status(): PushPortStatus {
        if (client == null) return PushPortStatus()
        val state = client.status()
        // Normalize Web's richer permission value to the common effective permission flag.
        state.notificationsEnabled = state.permission == "granted"
        return BridgeStatus.decode(JSON.stringify(state))
    }
    override fun sync() {
        check(client != null) { "Initialize PushPort first" }
        // The native SDK persists errors/retries; do not create an unhandled JS rejection.
        (client.sync() as Promise<dynamic>).catch { null }
    }
    override suspend fun setSubscribed(value: Boolean) { (client.setSubscribed(value) as Promise<dynamic>).await() }
    override suspend fun setLocale(languageTag: String?) { (client.setLocale(languageTag) as Promise<dynamic>).await() }
    override suspend fun requestPermission() { (client.requestPermission() as Promise<dynamic>).await() }
    override fun userJSON(): String? { val user = client?.user(); return if (user == null) null else JSON.stringify(user) }
    override suspend fun updateUserJSON(value: String) {
        val op = JSON.parse<dynamic>(value)
        val result = when (op.kind as String) {
            "tags" -> client.setTags(op.tags)
            "login" -> client.login(op.externalId, op.identityToken)
            "logout" -> client.logout()
            "email" -> client.setEmail(op.email)
            "phone" -> client.setPhoneNumber(op.phoneNumber)
            "location" -> client.setLocation(op.latitude, op.longitude)
            "event" -> client.trackEvent(op.eventName, op.eventProperties)
            else -> error("Invalid user operation")
        }
        (result as Promise<dynamic>).await()
    }
    override suspend fun setConsentRequired(value: Boolean) { (client.setConsentRequired(value) as Promise<dynamic>).await() }
    override suspend fun setConsentGiven(value: Boolean) { (client.setConsentGiven(value) as Promise<dynamic>).await() }
}
