package top.niunaijun.blackbox.fake.delegate

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RegisteredReceiverDeliveryCompatTest {
    @Test
    fun android13KeepsLegacyReceiverDelivery() {
        assertFalse(RegisteredReceiverDeliveryCompat.mustUseFrameworkDispatcher(33))
    }

    @Test
    fun android14AndNewerUseFrameworkDispatcherDelivery() {
        assertTrue(RegisteredReceiverDeliveryCompat.mustUseFrameworkDispatcher(34))
        assertTrue(RegisteredReceiverDeliveryCompat.mustUseFrameworkDispatcher(35))
    }
}
