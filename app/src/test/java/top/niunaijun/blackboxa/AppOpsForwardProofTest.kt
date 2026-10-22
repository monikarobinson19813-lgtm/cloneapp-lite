package top.niunaijun.blackboxa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.fake.service.AppOpsForwardProof
import top.niunaijun.blackbox.fake.service.AppOpsMethodDispatcher

class AppOpsForwardProofTest {
    private val intType = Int::class.javaPrimitiveType!!

    @Test
    fun nonAllowedOpReportsOneRealCallAndItsRealDeniedResult() {
        val events = mutableListOf<String>()
        var calls = 0
        val result = AppOpsMethodDispatcher.invoke(
            "checkOperation", intType, "android:system_alert_window", 0,
            AppOpsMethodDispatcher.RealCall {
                AppOpsForwardProof.trace("checkOperation", "android:system_alert_window",
                    AppOpsForwardProof.RealCall { calls++; 2 },
                    AppOpsForwardProof.EventSink { events.add(it) })
            }
        )
        assertEquals(1, calls)
        assertEquals(2, result)
        assertEquals(2, events.size)
        assertTrue(events[0].contains("path=real_enter"))
        assertTrue(events[1].contains("path=real_return"))
        assertTrue(events[1].contains("mode=2"))
        assertTrue(events[1].contains("op=android:system_alert_window"))
    }

    @Test
    fun approvedIntCheckDoesNotInvokeOrLogRealService() {
        val events = mutableListOf<String>()
        var calls = 0
        val result = AppOpsMethodDispatcher.invoke(
            "checkOperation", intType, "android:camera", 0,
            AppOpsMethodDispatcher.RealCall {
                AppOpsForwardProof.trace("checkOperation", "android:camera",
                    AppOpsForwardProof.RealCall { calls++; 2 },
                    AppOpsForwardProof.EventSink { events.add(it) })
            }
        )
        assertEquals(0, result)
        assertEquals(0, calls)
        assertTrue(events.isEmpty())
    }

    @Test
    fun realServiceExceptionIsUnchangedAndLoggedWithoutDetails() {
        val events = mutableListOf<String>()
        val denied = SecurityException("do not log this message")
        var caught: Throwable? = null
        try {
            AppOpsForwardProof.trace("checkOperation", "android:write_settings",
                AppOpsForwardProof.RealCall { throw denied },
                AppOpsForwardProof.EventSink { events.add(it) })
        } catch (failure: Throwable) {
            caught = failure
        }
        assertTrue(caught === denied)
        assertEquals(2, events.size)
        assertTrue(events[1].contains("path=real_throw"))
        assertTrue(events[1].contains("error_type=SecurityException"))
        assertFalse(events[1].contains("do not log this message"))
    }

    @Test
    fun diagnosticLoggingFailureNeverAltersRealReturn() {
        val result = AppOpsForwardProof.trace("checkOperation", null,
            AppOpsForwardProof.RealCall { 3 },
            AppOpsForwardProof.EventSink { throw IllegalStateException("log failure") })
        assertEquals(3, result)
    }
}
