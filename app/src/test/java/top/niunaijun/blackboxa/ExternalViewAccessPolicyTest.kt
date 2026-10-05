package top.niunaijun.blackboxa

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.fake.provider.ExternalViewAccessPolicy

class ExternalViewAccessPolicyTest {
    @Test
    fun externalGrantedViewerIsAllowed() {
        assertTrue(ExternalViewAccessPolicy.isAllowed(false, -1, 1))
    }

    @Test
    fun sameVirtualUserIsAllowed() {
        assertTrue(ExternalViewAccessPolicy.isAllowed(true, 1, 1))
    }

    @Test
    fun differentVirtualUserIsDenied() {
        assertFalse(ExternalViewAccessPolicy.isAllowed(true, 2, 1))
    }

    @Test
    fun unknownSameUidCallerIsDenied() {
        assertFalse(ExternalViewAccessPolicy.isAllowed(true, -1, 1))
    }
}
