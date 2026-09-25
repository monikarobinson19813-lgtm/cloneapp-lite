package top.niunaijun.blackboxa

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.fake.service.ExternalContentBridgePolicy

class ExternalContentBridgePolicyTest {
    @Test
    fun bridgesOnlyExternalPdfViews() {
        assertTrue(ExternalContentBridgePolicy.shouldBridge(
            "android.intent.action.VIEW", "content", "application/pdf"))
        assertFalse(ExternalContentBridgePolicy.shouldBridge(
            "android.intent.action.SEND", "content", "application/pdf"))
        assertFalse(ExternalContentBridgePolicy.shouldBridge(
            "android.intent.action.VIEW", "file", "application/pdf"))
        assertFalse(ExternalContentBridgePolicy.shouldBridge(
            "android.intent.action.VIEW", "content", "image/jpeg"))
    }
}
