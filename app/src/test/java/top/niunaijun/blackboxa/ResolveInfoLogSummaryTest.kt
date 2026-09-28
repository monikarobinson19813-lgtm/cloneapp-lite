package top.niunaijun.blackboxa

import org.junit.Assert.assertEquals
import org.junit.Test
import top.niunaijun.blackbox.fake.service.ResolveInfoLogSummary

class ResolveInfoLogSummaryTest {

    @Test
    fun summaryDoesNotStringifyResolveEntries() {
        val toxicEntry = object {
            override fun toString(): String {
                error("ResolveInfo.toString must not be invoked by logging")
            }
        }

        assertEquals(
            "queryIntentReceivers: count=1",
            ResolveInfoLogSummary.summarize("queryIntentReceivers", listOf(toxicEntry))
        )
    }
}
