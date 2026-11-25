package consumer

import dev.pushport.kmp.AppleEnvironment
import dev.pushport.kmp.PushPortConfiguration
import dev.pushport.kmp.createApplePushPort

/** Construct on the iOS main thread. Keep one client for the life of the application. */
class AppleDemoClient {
    private val client = createApplePushPort()

    @Throws(Exception::class)
    suspend fun initialize(appId: String, serverUrl: String, requireConsent: Boolean) {
        client.initialize(PushPortConfiguration(
            appId = appId,
            serverUrl = serverUrl,
            appleEnvironment = AppleEnvironment.SANDBOX,
            consentRequired = if (requireConsent) true else null,
        ))
    }

    @Throws(Exception::class)
    suspend fun requestPermission() = client.requestPermission()

    fun appProfileId(): String = client.appProfile().id

    fun isConfigured(): Boolean = client.status().configured
}
