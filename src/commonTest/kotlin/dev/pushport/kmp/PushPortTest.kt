package dev.pushport.kmp

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PushPortTest {
    private val appId = "6d632180-a023-4101-a4b1-9068404346f8"

    @Test fun operationsRequireInitializationAndPreserveBinding() = runTest {
        val driver = FakeDriver()
        val sdk = PushPort(driver)
        assertFalse(sdk.status().configured)
        assertFailsWith<IllegalStateException> { sdk.sync() }
        assertFailsWith<IllegalArgumentException> { sdk.initialize(PushPortConfiguration("wrong")) }
        sdk.initialize(PushPortConfiguration(appId))
        assertTrue(sdk.status().configured)
        sdk.setSubscribed(false)
        sdk.setLocale("uk-UA")
        sdk.requestPermission()
        sdk.sync()
        assertFalse(sdk.status().subscribed)
        assertEquals("uk-UA", sdk.status().locale)
        assertEquals(1, driver.permissionRequests)
        assertEquals(1, driver.syncs)
        assertFailsWith<IllegalArgumentException> { sdk.initialize(PushPortConfiguration(appId, "https://another.example")) }
    }

    @Test fun invalidLocaleNeverReachesTheNativeDriver() = runTest {
        val driver = FakeDriver()
        val sdk = PushPort(driver)
        sdk.initialize(PushPortConfiguration(appId))
        assertFailsWith<IllegalArgumentException> { sdk.setLocale("../../private") }
        assertFailsWith<IllegalArgumentException> { sdk.setLocale("und") }
        assertEquals(null, sdk.status().locale)
        sdk.setLocale("de-DE")
        sdk.setLocale(null)
        assertEquals(null, sdk.status().locale)
    }

    @Test fun appleBridgeStatusAllowsAdditiveFieldsAndDoesNotRequireTokens() {
        val state = BridgeStatus.decode("""{"configured":true,"subscribed":false,"hasToken":true,"notificationsEnabled":true,"futureField":"value"}""")
        assertTrue(state.hasToken)
        assertFalse(state.subscribed)
    }
}

private class FakeDriver : PushPortDriver {
    private var state = PushPortStatus()
    var permissionRequests = 0
    var syncs = 0
    override suspend fun initialize(configuration: PushPortConfiguration) { state = state.copy(configured = true) }
    override fun status() = state
    override fun sync() { syncs++ }
    override suspend fun setSubscribed(value: Boolean) { state = state.copy(subscribed = value) }
    override suspend fun setLocale(languageTag: String?) { state = state.copy(locale = languageTag) }
    override suspend fun requestPermission() { permissionRequests++ }
}
