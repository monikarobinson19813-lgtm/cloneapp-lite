package top.niunaijun.blackbox.core.system.pm

import org.junit.Assert.assertEquals
import org.junit.Test

class ResolverRefreshSequenceTest {
    @Test
    fun reloadsPackagesBeforeReplacingResolverComponents() {
        val events = mutableListOf<String>()

        ResolverRefreshSequence.run(
            { events += "reload" },
            { events += "remove-old" },
            { events += "add-current" }
        )

        assertEquals(listOf("reload", "remove-old", "add-current"), events)
    }
}
