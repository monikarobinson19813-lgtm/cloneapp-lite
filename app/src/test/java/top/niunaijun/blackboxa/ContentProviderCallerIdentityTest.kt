package top.niunaijun.blackboxa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.fake.service.context.providers.ContentProviderCallerIdentity

class ContentProviderCallerIdentityTest {
    @Test
    fun legacyCallingPackageOnlyRewritesFirstString() {
        val args = arrayOf<Any?>("guest.pkg", "application/pdf", "r")

        assertTrue(
            ContentProviderCallerIdentity.rewriteLegacyCallingPackage(
                args,
                "com.cloneapp.lite"
            )
        )
        assertEquals("com.cloneapp.lite", args[0])
        assertEquals("application/pdf", args[1])
        assertEquals("r", args[2])
    }

    @Test
    fun attributionSourceStyleArgsPreserveMimeType() {
        val attributionSource = Any()
        val args = arrayOf<Any?>(attributionSource, "content://guest/file.pdf", "application/pdf")

        assertFalse(
            ContentProviderCallerIdentity.rewriteLegacyCallingPackage(
                args,
                "com.cloneapp.lite"
            )
        )
        assertEquals(attributionSource, args[0])
        assertEquals("content://guest/file.pdf", args[1])
        assertEquals("application/pdf", args[2])
    }
}
