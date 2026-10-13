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
        assertEquals(4, InstanceRouting.nextVirtualUserId(listOf(0, 2, 3)))
        assertEquals(2, InstanceRouting.userForPage(1, listOf(0, 2, 3, 4)))
        assertEquals(3, InstanceRouting.userForPage(2, listOf(0, 2, 3, 4)))
    }

    @Test
    fun deleteRejectsAbsentUserAndUnrelatedPackages() {
        assertTrue(InstanceRouting.mayDeleteInstance(3, listOf(0, 1, 2, 3), "com.whatsapp", true))
        assertFalse(InstanceRouting.mayDeleteInstance(3, listOf(0, 1, 2), "com.whatsapp", true))
        assertFalse(InstanceRouting.mayDeleteInstance(3, listOf(0, 1, 2, 3), "com.android.vending", true))
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
    fun pageMapsToStableInstanceLabel() {
        assertEquals(1, InstanceRouting.userForPage(1, listOf(0, 1)))
        assertEquals("User 1", InstanceRouting.instanceLabel(1, null))
        assertEquals("Business", InstanceRouting.instanceLabel(1, "Business"))
    }
}
