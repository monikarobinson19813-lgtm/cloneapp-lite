package top.niunaijun.blackboxa

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import top.niunaijun.blackbox.core.system.permission.MediaPermissionPolicy

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class MediaPermissionPolicyRobolectricTest {

    @Test
    fun hostDenialFailsClosedEvenWhenVirtualCloneAllowsMicrophone() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val application = context as Application
        shadowOf(application).denyPermissions(Manifest.permission.RECORD_AUDIO)
        MediaPermissionPolicy.setVirtualPermissionAllowed(
            context, "com.whatsapp", 0, Manifest.permission.RECORD_AUDIO, true
        )

        assertEquals(
            PackageManager.PERMISSION_DENIED,
            MediaPermissionPolicy.permissionResult(
                context, "com.whatsapp", 0, Manifest.permission.RECORD_AUDIO
            )
        )
    }

    @Test
    fun virtualMicrophonePermissionIsIsolatedPerCloneWhenHostCapabilityExists() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val application = context as Application
        shadowOf(application).grantPermissions(Manifest.permission.RECORD_AUDIO)
        MediaPermissionPolicy.setVirtualPermissionAllowed(
            context, "com.whatsapp", 0, Manifest.permission.RECORD_AUDIO, true
        )
        MediaPermissionPolicy.setVirtualPermissionAllowed(
            context, "com.whatsapp", 1, Manifest.permission.RECORD_AUDIO, false
        )

        assertEquals(
            PackageManager.PERMISSION_GRANTED,
            MediaPermissionPolicy.permissionResult(
                context, "com.whatsapp", 0, Manifest.permission.RECORD_AUDIO
            )
        )
        assertEquals(
            PackageManager.PERMISSION_DENIED,
            MediaPermissionPolicy.permissionResult(
                context, "com.whatsapp", 1, Manifest.permission.RECORD_AUDIO
            )
        )
    }
}
