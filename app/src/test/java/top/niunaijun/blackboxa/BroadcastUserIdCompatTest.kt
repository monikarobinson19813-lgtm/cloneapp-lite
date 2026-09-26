package top.niunaijun.blackboxa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.fake.service.BroadcastUserIdCompat

class BroadcastUserIdCompatTest {
    @Test
    fun rewritesOnlyAndroid14FrameworkUserSlot() {
        // Mirrors the Android 14 broadcast tail: appOp may also be -1,
        // while the final Integer is the framework userId.
        val args = arrayOf<Any?>(
            null,
            "feature",
            Any(),
            "resolved/type",
            null,
            0,
            null,
            null,
            arrayOf("permission"),
            emptyArray<String>(),
            emptyArray<String>(),
            -1, // appOp: must remain unchanged
            null,
            false,
            false,
            -1  // UserHandle.ALL from the guest
        )

        assertTrue(BroadcastUserIdCompat.rewriteLastUserId(args, 0))
        assertEquals(-1, args[11])
        assertEquals(0, args[15])
    }

    @Test
    fun leavesUnknownSignatureUntouched() {
        val args = arrayOf<Any?>(null, "not-an-int-tail")

        assertFalse(BroadcastUserIdCompat.rewriteLastUserId(args, 0))
        assertEquals("not-an-int-tail", args[1])
    }
}
