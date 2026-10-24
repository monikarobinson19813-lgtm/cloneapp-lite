package top.niunaijun.blackboxa.binding

import android.app.Application
import android.os.ConditionVariable
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import top.niunaijun.blackbox.app.GuestBindGuard
import top.niunaijun.blackbox.core.GuestRedirectPlan
import top.niunaijun.blackbox.entity.AppConfig
import java.io.File
import java.util.LinkedHashMap
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class GuestBindingFailClosedTest {
    private val pkg = "com.whatsapp"
    private val hostUser = 0

    private fun config(user: Int, name: String = pkg, process: String = pkg) = AppConfig().apply {
        userId = user; packageName = name; processName = process
    }

    private fun guestDir(user: Int) = File(System.getProperty("java.io.tmpdir"), "p13-testing-${user}-${System.nanoTime()}").apply {
        assertTrue(mkdirs())
    }

    private fun rules(dir: File) = linkedMapOf(
        "/data/data/$pkg" to dir.absolutePath,
        "/data/user/$hostUser/$pkg" to dir.absolutePath
    )

    private fun runPlan(dir: File, builder: () -> Map<String, String>,
                        registered: MutableMap<String, String> = LinkedHashMap(),
                        enabled: IntArray = intArrayOf(0)) {
        GuestRedirectPlan.prepareValidateApply(
            pkg, hostUser, dir, registered,
            { builder() },
            { from, to -> registered[from] = to },
            { enabled[0]++ }
        )
    }

    @Test fun t1MandatoryMappingsAllowDistinctU0U1Bindings() {
        val a = guestDir(0); val b = guestDir(1)
        try {
            val map0 = linkedMapOf<String, String>(); val map1 = linkedMapOf<String, String>()
            val on0 = intArrayOf(0); val on1 = intArrayOf(0)
            runPlan(a, { rules(a) }, map0, on0)
            runPlan(b, { rules(b) }, map1, on1)
            assertEquals(a.absolutePath, map0["/data/data/$pkg"])
            assertEquals(b.absolutePath, map1["/data/data/$pkg"])
            assertNotEquals(map0["/data/data/$pkg"], map1["/data/data/$pkg"])
            assertEquals(1, on0[0]); assertEquals(1, on1[0])
        } finally { a.deleteRecursively(); b.deleteRecursively() }
    }

    @Test fun t2MissingMandatoryRedirectAbortsWithoutApplyingOrEnabling() {
        val dir = guestDir(2)
        try {
            val map = linkedMapOf<String, String>(); val enable = intArrayOf(0)
            expectRejection { runPlan(dir, { mapOf("/data/data/$pkg" to dir.absolutePath) }, map, enable) }
            assertTrue(map.isEmpty()); assertEquals(0, enable[0])
        } finally { dir.deleteRecursively() }
    }

    @Test fun t3ExceptionDuringPreparationNeverAppliesPartialRules() {
        val dir = guestDir(3)
        try {
            val map = linkedMapOf<String, String>(); val enable = intArrayOf(0)
            expectRejection {
                runPlan(dir, {
                    val partial = rules(dir)
                    assertEquals(2, partial.size)
                    throw IllegalStateException("Simulated optional rule preparation failure")
                }, map, enable)
            }
            assertTrue(map.isEmpty()); assertEquals(0, enable[0])
        } finally { dir.deleteRecursively() }
    }

    @Test fun t4ConflictingPriorMappingNeverOverridesAnotherGuest() {
        val dir = guestDir(4); val prior = guestDir(5)
        try {
            val map = linkedMapOf("/data/data/$pkg" to prior.absolutePath)
            val enable = intArrayOf(0)
            expectRejection { runPlan(dir, { rules(dir) }, map, enable) }
            assertEquals(prior.absolutePath, map["/data/data/$pkg"])
            assertEquals(0, enable[0])
        } finally { dir.deleteRecursively(); prior.deleteRecursively() }
    }

    @Test fun t5RejectsWrongUserProcessReuseWithoutMutatingOriginal() {
        val old = config(0)
        expectRejection { GuestBindGuard.requireCompatible(old, config(1)) }
        expectRejection { GuestBindGuard.requireCompatible(old, config(0, process = "com.whatsapp:other")) }
        assertEquals(0, old.userId)
        GuestBindGuard.requireCompatible(old, config(0))
    }

    @Test fun t6MissingOrMismatchedAppConfigNeverDefaultsToU0() {
        expectRejection { GuestBindGuard.requireRequest(null, pkg, pkg) }
        expectRejection { GuestBindGuard.requireRequest(config(-1), pkg, pkg) }
        expectRejection { GuestBindGuard.requireRequest(config(1), "com.other", pkg) }
        expectRejection { GuestBindGuard.requireRequest(config(1), pkg, "com.whatsapp:service") }
    }

    @Test fun t7RebuiltColdStatePreservesUserSpecificDirectories() {
        val a = guestDir(6); val b = guestDir(7)
        try {
            val freshBefore = linkedMapOf<String, String>()
            runPlan(a, { rules(a) }, freshBefore)
            val freshAfter = linkedMapOf<String, String>()
            runPlan(a, { rules(a) }, freshAfter)
            val independent = linkedMapOf<String, String>()
            runPlan(b, { rules(b) }, independent)
            assertEquals(freshBefore, freshAfter)
            assertNotEquals(freshAfter["/data/data/$pkg"], independent["/data/data/$pkg"])
        } finally { a.deleteRecursively(); b.deleteRecursively() }
    }

    @Test fun t8FailureAlwaysSignalsCallerAndPropagatesOnWorker() {
        val gate = ConditionVariable()
        val error = AtomicReference<Throwable>()
        val failure = IllegalStateException("synthetic bind failure")
        val worker = Thread {
            GuestBindGuard.runAndSignal({ throw failure }, gate, error)
        }
        worker.start()
        assertTrue("failed bind must release waiter", gate.block(3000))
        worker.join(3000)
        assertFalse(worker.isAlive)
        assertSame(failure, error.get())
        expectRejection { GuestBindGuard.rethrowFailure(error.get()) }
        val clean = AtomicReference<Throwable>()
        val cleanGate = ConditionVariable()
        GuestBindGuard.runAndSignal({ GuestBindGuard.requireRequest(config(0), pkg, pkg) }, cleanGate, clean)
        assertTrue(cleanGate.block(1000))
        GuestBindGuard.rethrowFailure(clean.get())
    }

    private inline fun expectRejection(action: () -> Unit) {
        try { action(); fail("Expected fail-closed binding rejection") }
        catch (expected: IllegalStateException) { /* Intentional safe rejection. */ }
    }
}
