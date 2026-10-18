package top.niunaijun.blackboxa

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.fake.service.AppOpsMethodDispatcher

class AppOpsMethodDispatcherTest {
    private val intResult = Int::class.javaPrimitiveType!!

    @Test
    fun exactAllowedCheckUsesCompatibilityModeWithoutRealCall() {
        var forwarded = false
        val result = AppOpsMethodDispatcher.invoke(
            "checkOperation", intResult, "android:camera", 0,
            AppOpsMethodDispatcher.RealCall { forwarded = true; 3 }
        )
        assertEquals(0, result)
        assertFalse(forwarded)
    }

    @Test
    fun unlistedOperationForwardsAndPreservesRealDeniedAnswer() {
        var calls = 0
        val answer = AppOpsMethodDispatcher.invoke(
            "checkOperation", intResult, "android:system_alert_window", 0,
            AppOpsMethodDispatcher.RealCall { calls++; 2 }
        )
        assertEquals(2, answer)
        assertEquals(1, calls)
    }

    @Test
    fun unlistedNoteOperationPreservesRealAnswer() {
        val result = AppOpsMethodDispatcher.invoke(
            "noteOperation", intResult, "android:write_settings", 0,
            AppOpsMethodDispatcher.RealCall { 1 }
        )
        assertEquals(1, result)
    }

    @Test
    fun unknownNamesAndCheckPackageAlwaysForward() {
        for (methodName in listOf("checkPackage", "unknownCheck", "checkOperation")) {
            val op = if (methodName == "checkOperation") null else "android:camera"
            val result = AppOpsMethodDispatcher.invoke(
                methodName, intResult, op, 0, AppOpsMethodDispatcher.RealCall { 2 }
            )
            assertEquals(2, result)
        }
    }

    @Test
    fun tokenLifecycleAlwaysDelegatesEvenForAllowedOperation() {
        for (methodName in listOf("startOp", "startOperation", "finishOp", "finishOperation")) {
            var called = false
            val result = AppOpsMethodDispatcher.invoke(
                methodName, intResult, "android:camera", 0,
                AppOpsMethodDispatcher.RealCall { called = true; 1 }
            )
            assertTrue("$methodName must call real service", called)
            assertEquals(1, result)
        }
    }

    @Test
    fun objectAndVoidReturnsMustNeverReceiveIntegerMode() {
        for (returnType in listOf(Any::class.java, Void.TYPE)) {
            val result = AppOpsMethodDispatcher.invoke(
                "noteOperation", returnType, "android:record_audio", 0,
                AppOpsMethodDispatcher.RealCall { "real" }
            )
            assertEquals("real", result)
        }
    }

    @Test
    fun securityExceptionIsNeverConvertedIntoAllow() {
        var threw = false
        try {
            AppOpsMethodDispatcher.invoke("checkOperation", intResult,
                "android:body_sensors", 0,
                AppOpsMethodDispatcher.RealCall { throw SecurityException("real service rejected") })
        } catch (e: SecurityException) {
            threw = true
            assertEquals("real service rejected", e.message)
        }
        assertTrue(threw)
    }
}
