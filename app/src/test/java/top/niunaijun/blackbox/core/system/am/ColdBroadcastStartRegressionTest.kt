package top.niunaijun.blackbox.core.system.am

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ColdBroadcastStartRegressionTest {

    @Test
    fun scheduleBroadcastReceiverColdStartsMissingGuestProcess() {
        val source = activityManagerServiceSource()
        val methodBody = scheduleBroadcastReceiverBody(source)

        val lookupIndex = methodBody.indexOf("findProcessRecord(")
        val coldStartIndex = methodBody.indexOf("startProcessLocked(")
        val scheduleIndex = methodBody.indexOf(".scheduleReceiver(")

        assertTrue("scheduleBroadcastReceiver() must look up the target guest process", lookupIndex >= 0)
        assertTrue(
            "Cold broadcast delivery must start the virtual guest process when no ProcessRecord exists",
            coldStartIndex >= 0
        )
        assertTrue("scheduleBroadcastReceiver() must eventually schedule the receiver", scheduleIndex >= 0)
        assertTrue(
            "The cold-start decision must happen before scheduling the guest receiver",
            coldStartIndex < scheduleIndex
        )
    }

    @Test
    fun coldStartIsRestrictedToGooglePushDelivery() {
        assertTrue(
            ColdBroadcastStartPolicy.shouldColdStart(
                ColdBroadcastStartPolicy.GOOGLE_PUSH_RECEIVE_ACTION
            )
        )
        assertFalse(ColdBroadcastStartPolicy.shouldColdStart("android.intent.action.SCREEN_ON"))
        assertFalse(ColdBroadcastStartPolicy.shouldColdStart("android.intent.action.BOOT_COMPLETED"))
        assertFalse(ColdBroadcastStartPolicy.shouldColdStart(null))
        assertTrue(
            "Cold-start wait must remain well below the 9 second broadcast timeout",
            ColdBroadcastStartPolicy.START_TIMEOUT_MS in 1..3000
        )
    }

    @Test
    fun failedColdStartFinishesBroadcastAndLeavesDiagnosticMarkers() {
        val source = activityManagerServiceSource()
        val methodBody = scheduleBroadcastReceiverBody(source)

        assertTrue(methodBody.contains("COLD_BROADCAST_START_FAILED reason=timeout"))
        assertTrue(methodBody.contains("COLD_BROADCAST_START_FAILED reason=exception"))
        assertTrue(methodBody.contains("COLD_BROADCAST_START_FAILED reason=null_process"))
        assertTrue(methodBody.contains("if (scheduledReceiverCount == 0)"))
        assertTrue(methodBody.contains("mBroadcastManager.finishBroadcast(pendingResultData)"))
        assertTrue(methodBody.contains("pendingResultData.build().finish()"))
        assertTrue(methodBody.contains("BROADCAST_UNDELIVERED_FINISHED"))
    }

    private fun scheduleBroadcastReceiverBody(source: String): String {
        val methodStart = source.indexOf("public void scheduleBroadcastReceiver(")
        assertTrue("scheduleBroadcastReceiver() must exist", methodStart >= 0)
        val nextOverride = source.indexOf("\n    @Override", methodStart + 1)
        return if (nextOverride > methodStart) {
            source.substring(methodStart, nextOverride)
        } else {
            source.substring(methodStart)
        }
    }

    private fun activityManagerServiceSource(): String {
        val relativePath =
            "Bcore/src/main/java/top/niunaijun/blackbox/core/system/am/BActivityManagerService.java"
        val candidates = listOf(
            File(System.getProperty("user.dir"), "../" + relativePath),
            File(System.getProperty("user.dir"), relativePath),
            File(relativePath)
        )
        val sourceFile = candidates.firstOrNull { it.isFile }
            ?: error("Unable to locate " + relativePath + " from " + System.getProperty("user.dir"))
        return sourceFile.readText()
    }
}
