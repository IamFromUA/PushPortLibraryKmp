@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package dev.pushport.kmp

import dev.pushport.kmp.bridge.PPPushPortBridge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import platform.Foundation.NSThread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** The iOS host must link the PushPort Swift package and forward APNs registration callbacks. */
public fun createApplePushPort(): PushPort {
    check(NSThread.isMainThread) { "Create PushPort on the main thread" }
    return PushPort(AppleDriver(PPPushPortBridge()))
}

private class AppleDriver(private val bridge: PPPushPortBridge) : PushPortDriver {
    override suspend fun initialize(configuration: PushPortConfiguration): Unit = onMain { completion ->
        bridge.initializeWithAppId(configuration.appId, configuration.serverUrl, configuration.appleEnvironment.name.lowercase(), completion)
    }
    override fun status(): PushPortStatus {
        check(NSThread.isMainThread) { "Read PushPort status on the main thread" }
        return BridgeStatus.decode(bridge.statusJSON())
    }
    override fun sync() { check(NSThread.isMainThread) { "Sync PushPort on the main thread" }; bridge.sync() }
    override suspend fun setSubscribed(value: Boolean): Unit = onMain { bridge.setSubscribed(value, it) }
    override suspend fun setLocale(languageTag: String?): Unit = onMain { bridge.setLocale(languageTag, it) }
    override suspend fun requestPermission(): Unit = onMain { bridge.requestPermissionWithCompletion(it) }
    private suspend fun onMain(action: ((String?) -> Unit) -> Unit): Unit = withContext(Dispatchers.Main.immediate) {
        suspendCancellableCoroutine { continuation ->
            action { error ->
                if (continuation.isActive) {
                    if (error == null) continuation.resume(Unit) else continuation.resumeWithException(IllegalStateException(error))
                }
            }
        }
    }
}
