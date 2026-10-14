package dev.pushport.kmp

import android.app.Activity
import android.content.Context
import dev.pushport.sdk.PushPort as NativePushPort
import dev.pushport.sdk.PushPortConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Retains only applicationContext. The activity provider is called only when requesting permission;
 * supply the current Activity without capturing a destroyed one. The host owns its lifecycle.
 */
public fun createAndroidPushPort(context: Context, activity: () -> Activity?): PushPort =
    PushPort(AndroidDriver(context.applicationContext, activity))

private class AndroidDriver(private val context: Context, private val activity: () -> Activity?) : PushPortDriver {
    override suspend fun initialize(configuration: PushPortConfiguration) {
        configuration.consentRequired?.let { NativePushPort.setConsentRequired(context, it) }
        configuration.consentGiven?.let { NativePushPort.setConsentGiven(context, it) }
        NativePushPort.init(context, PushPortConfig(configuration.appId, configuration.serverUrl))
    }
    override fun status(): PushPortStatus {
        val status = NativePushPort.status(context)
        return PushPortStatus(status.configured, status.installationId, status.subscribed, status.fcmToken != null,
            status.notificationsEnabled, status.locale, status.lastSyncedAt, status.syncError)
    }
    override fun sync() = NativePushPort.sync(context)
    override fun userJSON(): String? = NativePushPort.user(context)?.let { user ->
        BridgeStatus.json.encodeToString(PushPortUser(user.userId, user.externalId, UserProperties(user.tags, user.email, user.phoneNumber, user.latitude, user.longitude), user.revision))
    }
    override suspend fun updateUserJSON(value: String) {
        val operation = BridgeStatus.json.decodeFromString<UserOperation>(value)
        when (operation.kind) {
            "tags" -> NativePushPort.setTags(context, operation.tags)
            "login" -> NativePushPort.login(context, requireNotNull(operation.externalId), requireNotNull(operation.identityToken))
            "logout" -> NativePushPort.logout(context)
            "email" -> NativePushPort.setEmail(context, operation.email)
            "phone" -> NativePushPort.setPhoneNumber(context, operation.phoneNumber)
            "location" -> NativePushPort.setLocation(context, operation.latitude, operation.longitude)
            "event" -> NativePushPort.trackEvent(context, requireNotNull(operation.eventName), operation.eventProperties)
            else -> error("Invalid user operation")
        }
    }
    override suspend fun setConsentRequired(value: Boolean) = NativePushPort.setConsentRequired(context, value)
    override suspend fun setConsentGiven(value: Boolean) = NativePushPort.setConsentGiven(context, value)
    override suspend fun setSubscribed(value: Boolean) = NativePushPort.setSubscribed(context, value)
    override suspend fun setLocale(languageTag: String?) = NativePushPort.setLocale(context, languageTag)
    override suspend fun requestPermission() = withContext(Dispatchers.Main.immediate) {
        val foreground = checkNotNull(activity()) { "A foreground Activity is required to request notification permission" }
        check(!foreground.isFinishing && !foreground.isDestroyed) { "The Activity is no longer active" }
        NativePushPort.requestNotificationPermission(foreground)
    }
}
