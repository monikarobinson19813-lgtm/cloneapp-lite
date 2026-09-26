package top.niunaijun.blackboxa

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import top.niunaijun.blackbox.BlackBoxCore

@RunWith(AndroidJUnit4::class)
class VirtualUser0InstallTest {
    companion object {
        private const val TAG = "CloneAppVirtualTest"
        private const val STUB_PACKAGE = "com.cloneapp.teststub"
    }

    @Test
    fun installsHostStubIntoVirtualUser0() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        targetContext.packageManager.getPackageInfo(STUB_PACKAGE, 0)

        val core = BlackBoxCore.get()
        core.createUser(0)

        if (core.isInstalled(STUB_PACKAGE, 0)) {
            core.uninstallPackageAsUser(STUB_PACKAGE, 0)
        }

        val result = core.installPackageAsUser(STUB_PACKAGE, 0)
        Log.i(TAG, "VIRTUAL_USER_INSTALL_RESULT user=0 success=${result.success} package=${result.packageName} msg=${result.msg}")

        assertTrue("Virtual install failed: ${result.msg}", result.success)
        assertTrue("Stub not installed for virtual User 0", core.isInstalled(STUB_PACKAGE, 0))

        val listed = core.getInstalledApplications(0, 0).any { it.packageName == STUB_PACKAGE }
        assertTrue("Stub missing from virtual User 0 application list", listed)

        Log.i(TAG, "VIRTUAL_USER_INSTALL_OK user=0 package=$STUB_PACKAGE")
    }
}
