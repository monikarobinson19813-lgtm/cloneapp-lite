package top.niunaijun.blackbox.core.system.am

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ColdBroadcastStartRegressionTest {

    @Test
    fun scheduleBroadcastReceiverColdStartsMissingGuestProcess() {
        val sourceFile = locateActivityManagerServiceSource()
        val source = sourceFile.readText()

        val methodStart = source.indexOf("public void scheduleBroadcastReceiver(")
        assertTrue("scheduleBroadcastReceiver() must exist", methodStart >= 0)

        val nextOverride = source.indexOf("\n    @Override", methodStart + 1)
        val methodBody = if (nextOverride > methodStart) {
            source.substring(methodStart, nextOverride)
        } else {
            source.substring(methodStart)
        }

        val lookupIndex = methodBody.indexOf("findProcessRecord(")
        val coldStartIndex = methodBody.indexOf("startProcessLocked(")
        val scheduleIndex = methodBody.indexOf(".scheduleReceiver(")

        assertTrue("scheduleBroadcastReceiver() must look up the target guest process", lookupIndex >= 0)
        assertTrue("scheduleBroadcastReceiver() must eventually schedule the receiver", scheduleIndex >= 0)
        assertTrue(
            "Cold broadcast delivery must start the virtual guest process when no ProcessRecord exists",
            coldStartIndex >= 0
        )
        assertTrue(
            "The cold-start decision must happen before scheduling the guest receiver",
            coldStartIndex < scheduleIndex
        )
    }

    private fun locateActivityManagerServiceSource(): File {
        val relativePath =
            "Bcore/src/main/java/top/niunaijun/blackbox/core/system/am/BActivityManagerService.java"

        val candidates = listOf(
            File(System.getProperty("user.dir"), "../" + relativePath),
            File(System.getProperty("user.dir"), relativePath),
            File(relativePath)
        )

        return candidates.firstOrNull { it.isFile }
            ?: error("Unable to locate " + relativePath + " from " + System.getProperty("user.dir"))
    }
}
