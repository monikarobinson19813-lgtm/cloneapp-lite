package top.niunaijun.blackboxa.view.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
    fun pageMapsToStableInstanceLabel() {
        assertEquals(1, InstanceRouting.userForPage(1, listOf(0, 1)))
        assertEquals("User 1", InstanceRouting.instanceLabel(1, null))
        assertEquals("Business", InstanceRouting.instanceLabel(1, "Business"))
    }
}
