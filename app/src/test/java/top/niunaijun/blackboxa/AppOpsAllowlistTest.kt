package top.niunaijun.blackboxa

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import top.niunaijun.blackbox.fake.service.AppOpsAllowlist

class AppOpsAllowlistTest {
    @Test
    fun exactApprovedPublicOperationNames() {
        val allowed = listOf(
            "android:record_audio",
            "android:camera",
            "android:picture_in_picture",
            "android:post_notification",
            "android:read_media_audio",
            "android:read_media_images",
            "android:read_media_video",
            "android:read_external_storage",
            "android:foreground_service",
            "android:modify_audio_settings",
            "android:capture_audio_output",
            "android:access_fine_location",
            "android:access_coarse_location",
            "android:bluetooth_scan",
            "android:bluetooth_connect",
            "android:bluetooth_advertise"
        )
        allowed.forEach {
            assertTrue("Expected exact allowlist match for $it", AppOpsAllowlist.isAllowedPublicOp(it))
        }
    }

    @Test
    fun everythingElseMustNotBeApprovedByTheCompatibilityPolicy() {
        val forward = listOf(
            "android:system_alert_window",
            "android:body_sensors",
            "android:nearby_wifi_devices",
            "android:write_settings",
            "android:read_media_visual_user_selected",
            "android:read_sms",
            "android:audio",
            "android:record_audio_extra",
            "android:post_notifications",
            "RECORD_AUDIO",
            "android:CAMERA",
            "android:camera ",
            ""
        )
        forward.forEach {
            assertFalse("Must forward unapproved op $it", AppOpsAllowlist.isAllowedPublicOp(it))
        }
        assertFalse(AppOpsAllowlist.isAllowedPublicOp(null))
    }
}
