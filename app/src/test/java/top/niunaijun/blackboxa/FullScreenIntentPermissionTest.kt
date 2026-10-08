package top.niunaijun.blackboxa

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FullScreenIntentPermissionTest {
    @Test
    fun hostRequestsUseFullScreenIntentPermission() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val packageInfo = context.packageManager.getPackageInfo(
            context.packageName,
            PackageManager.GET_PERMISSIONS
        )

        assertTrue(
            packageInfo.requestedPermissions?.contains(
                Manifest.permission.USE_FULL_SCREEN_INTENT
            ) == true
        )
    }
}
