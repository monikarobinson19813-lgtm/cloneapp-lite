package top.niunaijun.blackboxa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.core.system.ProxySlotDiagnostics

class ProxySlotDiagnosticsTest {

    @Test
    fun parsesRegularAndTransparentProxySlots() {
        assertEquals(
            0,
            ProxySlotDiagnostics.parseProxySlot(
                "top.niunaijun.blackbox.proxy.ProxyActivity\$P0"
            )
        )
        assertEquals(
            7,
            ProxySlotDiagnostics.parseProxySlot(
                "top.niunaijun.blackbox.proxy.TransparentProxyActivity\$P7"
            )
        )
    }

    @Test
    fun rejectsNonProxyComponents() {
        assertEquals(
            -1,
            ProxySlotDiagnostics.parseProxySlot("com.whatsapp.Main")
        )
        assertEquals(
            -1,
            ProxySlotDiagnostics.parseProxySlot(
                "top.niunaijun.blackbox.proxy.ProxyActivity\$PX"
            )
        )
    }

    @Test
    fun detectsStaleRecentTaskCollisionForCandidateSlot() {
        val recentSlots = ProxySlotDiagnostics.collectProxySlots(
            listOf(
                "top.niunaijun.blackbox.proxy.ProxyActivity\$P0",
                "top.niunaijun.blackbox.proxy.TransparentProxyActivity\$P3"
            )
        )

        assertTrue(ProxySlotDiagnostics.hasRecentTaskCollision(0, recentSlots))
        assertTrue(ProxySlotDiagnostics.hasRecentTaskCollision(3, recentSlots))
        assertFalse(ProxySlotDiagnostics.hasRecentTaskCollision(1, recentSlots))
    }
}
