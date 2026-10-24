package top.niunaijun.blackbox.fake.service

import android.app.Application
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.niunaijun.blackbox.entity.AppConfig

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GuestTelephonyIdentityRobolectricTest {
    private val hostPackage = "com.cloneapp.lite"

    @Test
    fun distinctVirtualUsersNeverShareTheirSyntheticIds() {
        val ids = (0..4).map { GuestTelephonyIdentity.fromGuestConfig(hostPackage, guest(it)) }
        assertEquals(5, ids.toSet().size)
        assertNotEquals(ids[0], ids[1])
        assertNotEquals(ids[1], ids[2])
    }

    @Test
    fun sameVirtualUserRetainsIdentityAcrossDifferentAppConfigInstances() {
        val before = GuestTelephonyIdentity.fromGuestConfig(hostPackage, guest(2))
        val afterRestart = GuestTelephonyIdentity.fromGuestConfig(hostPackage, guest(2))
        assertEquals(before, afterRestart)
        assertEquals(before, GuestTelephonyIdentity.fromGuestConfig(hostPackage, guest(2)))
    }

    @Test
    fun syntheticIdPreservesExistingMd5HexReturnFormat() {
        val id = GuestTelephonyIdentity.fromGuestConfig(hostPackage, guest(0))
        assertTrue(id.matches(Regex("[0-9a-f]{32}")))
    }

    @Test
    fun hostPackageIsPartOfTheStableDerivation() {
        val guest = guest(1)
        assertNotEquals(
            GuestTelephonyIdentity.fromGuestConfig(hostPackage, guest),
            GuestTelephonyIdentity.fromGuestConfig("com.example.other.container", guest)
        )
    }

    @Test
    fun missingAppConfigFailsClosedInsteadOfReturningImplicitUserZero() {
        assertFailsClosed {
            GuestTelephonyIdentity.fromGuestConfig(hostPackage, null)
        }
    }

    @Test
    fun invalidGuestConfigurationFailsClosed() {
        assertFailsClosed { GuestTelephonyIdentity.fromGuestConfig(hostPackage, guest(-1)) }
        assertFailsClosed { GuestTelephonyIdentity.fromGuestConfig(hostPackage, AppConfig()) }
        assertFailsClosed { GuestTelephonyIdentity.fromGuestConfig("", guest(0)) }
        assertFailsClosed { GuestTelephonyIdentity.fromGuestConfig(null, guest(0)) }
    }

    @Test
    fun neverReturnsARealDeviceIdentifierOrSubscriberNumber() {
        // Deliberately provide only host-package and virtual-user data.
        // No hardware identifier or original TelephonyManager value is used.
        val realLikeImei = "356938035643809"
        val realLikeSubscriberId = "404100123456789"
        for (virtualUser in 0..4) {
            val id = GuestTelephonyIdentity.fromGuestConfig(hostPackage, guest(virtualUser))
            assertNotEquals(realLikeImei, id)
            assertNotEquals(realLikeSubscriberId, id)
            assertFalse(id.contains(realLikeImei))
        }
    }

    private fun guest(userId: Int): AppConfig = AppConfig().apply {
        this.userId = userId
        packageName = "com.whatsapp"
        processName = "com.whatsapp"
    }

    private fun assertFailsClosed(action: () -> Unit) {
        try {
            action()
            fail("Expected rejection without a verified virtual user")
        } catch (expected: IllegalStateException) {
            // Safe failure: do not query the physical TelephonyManager.
        }
    }
}
