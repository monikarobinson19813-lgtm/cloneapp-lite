package top.niunaijun.blackboxa.view.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.assertFalse
import org.junit.Test

class InstanceRoutingTest {
    @Test
    fun delayedResultUsesReturnedInstance() {
        val visibleInstance = 1
        val returnedInstance = 0
        val target = InstanceRouting.installTarget(returnedInstance, listOf(0, 1))
        assertEquals(0, target)
        assertEquals(1, visibleInstance)
    }

    @Test
    fun detachedInstanceIsRejected() {
        assertNull(InstanceRouting.installTarget(1, listOf(0)))
    }

    @Test
    fun deletedMiddleUserDoesNotRenumberSurvivors() {
        assertEquals(4, InstanceRouting.nextVirtualUserId(
            InstanceRouting.seedHighWaterMark(-1, listOf(0, 2, 3))
        ))
        assertEquals(2, InstanceRouting.userForPage(1, listOf(0, 2, 3, 4)))
        assertEquals(3, InstanceRouting.userForPage(2, listOf(0, 2, 3, 4)))
    }

    @Test
    fun deletingHighestAllocatedIdNeverReusesIt() {
        val persistedHighestEver = 4 // U4 reserved before install.
        val survivors = listOf(0, 1, 2, 3) // U4 deleted.
        val mark = InstanceRouting.seedHighWaterMark(persistedHighestEver, survivors)
        assertEquals(4, mark)
        assertEquals(5, InstanceRouting.nextVirtualUserId(mark))
    }

    @Test
    fun persistedAllocationSurvivesSimulatedRestartAndReboot() {
        val fakePreferences = mutableMapOf<String, Int>()
        val key = "highest_ever_user_id"
        // First launch migrates the existing U0/U1/U2 registrations.
        val firstMark = InstanceRouting.seedHighWaterMark(
            fakePreferences[key] ?: -1, listOf(0, 1, 2)
        )
        assertEquals(3, InstanceRouting.nextVirtualUserId(firstMark))
        // Simulate synchronous commit of the allocated U3, then delete U3.
        fakePreferences[key] = 3
        // New Activity/process reads storage again; deleted U3 is absent.
        val restartedMark = InstanceRouting.seedHighWaterMark(
            fakePreferences[key] ?: -1, listOf(0, 1, 2)
        )
        assertEquals(4, InstanceRouting.nextVirtualUserId(restartedMark))
        // Simulate another allocation, persistence and device reboot.
        fakePreferences[key] = 4
        val rebootedMark = InstanceRouting.seedHighWaterMark(
            fakePreferences[key] ?: -1, listOf(0, 1, 2)
        )
        assertEquals(5, InstanceRouting.nextVirtualUserId(rebootedMark))
    }

    @Test
    fun userIdExhaustionIsFailClosed() {
        assertNull(InstanceRouting.nextVirtualUserId(Int.MAX_VALUE))
        assertEquals(Int.MAX_VALUE, InstanceRouting.seedHighWaterMark(
            Int.MAX_VALUE, listOf(0, 1, 2)
        ))
        assertEquals(0, InstanceRouting.nextVirtualUserId(-1))
    }

    @Test
    fun deleteRejectsAbsentUserAndUnrelatedPackages() {
        assertTrue(InstanceRouting.mayDeleteInstance(3, listOf(0, 1, 2, 3), "com.whatsapp", true))
        assertFalse(InstanceRouting.mayDeleteInstance(3, listOf(0, 1, 2), "com.whatsapp", true))
        assertFalse(InstanceRouting.mayDeleteInstance(3, listOf(0, 1, 2, 3), "com.android.vending", true))
        assertFalse(InstanceRouting.mayDeleteInstance(3, listOf(0, 1, 2, 3), "com.whatsapp.w4b", true))
        assertFalse(InstanceRouting.mayDeleteInstance(3, listOf(0, 1, 2, 3), "com.whatsapp", false))
        assertFalse(InstanceRouting.mayDeleteInstance(-1, listOf(0, 1), "com.whatsapp", true))
    }

    @Test
    fun deleteVerifiesAllOtherUserIdsAndBothStorageRoots() {
        val before = listOf(0, 1, 2, 3)
        assertTrue(InstanceRouting.deletionComplete(3, before, listOf(0, 1, 2), false, false))
        assertFalse(InstanceRouting.deletionComplete(3, before, listOf(0, 1), false, false))
        assertFalse(InstanceRouting.deletionComplete(3, before, listOf(0, 1, 2), true, false))
        assertFalse(InstanceRouting.deletionComplete(3, before, listOf(0, 1, 2), false, true))
        assertFalse(InstanceRouting.deletionComplete(3, before, before, false, false))
    }

    @Test
    fun partialDeletionNamesSpecificRemainingRootAndUserState() {
        val before = listOf(0, 1, 2, 3)
        val survivors = listOf(0, 1, 2)
        val internalLeft = InstanceRouting.deletionFailureDetails(
            3, before, survivors, true, false, false
        )!!
        assertTrue(internalLeft.contains("user removed but data remains"))
        assertTrue(internalLeft.contains("internal data/user/3"))
        val externalLeft = InstanceRouting.deletionFailureDetails(
            3, before, survivors, false, true, false
        )!!
        assertTrue(externalLeft.contains("external storage/emulated/3"))
        val stillRegistered = InstanceRouting.deletionFailureDetails(
            3, before, before, false, false, true
        )!!
        assertTrue(stillRegistered.contains("user still registered"))
        assertTrue(stillRegistered.contains("WhatsApp package still installed"))
        val missingSibling = InstanceRouting.deletionFailureDetails(
            3, before, listOf(0, 1), false, false, false
        )!!
        assertTrue(missingSibling.contains("surviving user IDs differ"))
        assertNull(InstanceRouting.deletionFailureDetails(
            3, before, survivors, false, false, false
        ))
    }

    @Test
    fun pageMapsToStableInstanceLabel() {
        assertEquals(1, InstanceRouting.userForPage(1, listOf(0, 1)))
        assertEquals("User 1", InstanceRouting.instanceLabel(1, null))
        assertEquals("Business", InstanceRouting.instanceLabel(1, "Business"))
    }
}
