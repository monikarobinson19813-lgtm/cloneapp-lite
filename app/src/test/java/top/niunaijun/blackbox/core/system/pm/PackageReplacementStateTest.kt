package top.niunaijun.blackbox.core.system.pm

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageReplacementStateTest {
    @Test
    fun replacingRemoveMarksWithoutReload() {
        assertEquals(
            PackageReplacementState.BroadcastDisposition.MARK_REPLACING,
            PackageReplacementState.classify("android.intent.action.PACKAGE_REMOVED", true)
        )
    }

    @Test
    fun replacingAddDoesNotCompleteReplacement() {
        assertEquals(
            PackageReplacementState.BroadcastDisposition.KEEP_REPLACING,
            PackageReplacementState.classify("android.intent.action.PACKAGE_ADDED", true)
        )
    }

    @Test
    fun packageReplacedIsTheSingleCompletionPoint() {
        assertEquals(
            PackageReplacementState.BroadcastDisposition.COMPLETE_REPLACEMENT,
            PackageReplacementState.classify("android.intent.action.PACKAGE_REPLACED", false)
        )
    }

    @Test
    fun standalonePackageChangeKeepsLegacyReloadBehavior() {
        assertEquals(
            PackageReplacementState.BroadcastDisposition.RELOAD,
            PackageReplacementState.classify("android.intent.action.PACKAGE_REMOVED", false)
        )
        assertEquals(
            PackageReplacementState.BroadcastDisposition.RELOAD,
            PackageReplacementState.classify("android.intent.action.PACKAGE_ADDED", false)
        )
    }

    @Test
    fun gateStaysSetUntilExplicitCompletionClear() {
        val state = PackageReplacementState()

        assertFalse(state.isReplacing("com.whatsapp"))
        assertTrue(state.mark("com.whatsapp"))
        assertTrue(state.isReplacing("com.whatsapp"))
        assertFalse(state.mark("com.whatsapp"))

        state.clear("com.whatsapp")
        assertFalse(state.isReplacing("com.whatsapp"))
    }
}
