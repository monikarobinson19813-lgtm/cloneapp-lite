package top.niunaijun.blackbox.fake.service;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * ABR-approved compatibility operations for the virtualized WhatsApp guest.
 *
 * Match only the exact value returned by AppOpsManager.opToPublicName(int).
 * Do not use opToName(), substring matching, or broad categories as approval.
 * A missing/null/unrecognized name must be forwarded to the real AppOps service.
 *
 * This policy does not make any authorization decision by itself; it is consumed
 * by the AppOps method dispatcher after return-type and lifecycle validation.
 */
public final class AppOpsAllowlist {
    private static final Set<String> PUBLIC_OPS =
            Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
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
            )));

    private AppOpsAllowlist() {
    }

    public static boolean isAllowedPublicOp(String publicOpName) {
        return publicOpName != null && PUBLIC_OPS.contains(publicOpName);
    }
}
