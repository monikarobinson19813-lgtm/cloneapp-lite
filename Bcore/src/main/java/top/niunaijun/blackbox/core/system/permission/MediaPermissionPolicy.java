package top.niunaijun.blackbox.core.system.permission;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;

/**
 * Separates the host Android media capability from per-virtual-user permission state.
 *
 * Android's real audio stack evaluates the host UID, so the host must hold RECORD_AUDIO.
 * A virtual user can still be denied independently.
 */
public final class MediaPermissionPolicy {
    private static final String PREFS = "blackbox_media_permission_policy";
    private static final String KEY_PREFIX = "permission:";

    private MediaPermissionPolicy() {
    }

    public static boolean isRuntimeMediaPermission(String permission) {
        return Manifest.permission.RECORD_AUDIO.equals(permission);
    }

    public static int permissionResult(Context context, String packageName, int userId, String permission) {
        if (!isRuntimeMediaPermission(permission)) {
            return PackageManager.PERMISSION_DENIED;
        }
        if (!isHostPermissionGranted(context, permission)) {
            return PackageManager.PERMISSION_DENIED;
        }
        return isVirtualPermissionAllowed(context, packageName, userId, permission)
                ? PackageManager.PERMISSION_GRANTED
                : PackageManager.PERMISSION_DENIED;
    }

    public static boolean isHostPermissionGranted(Context context, String permission) {
        if (context == null || permission == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        return context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public static boolean isVirtualPermissionAllowed(
            Context context,
            String packageName,
            int userId,
            String permission
    ) {
        if (context == null || packageName == null || permission == null) {
            return false;
        }
        return preferences(context).getBoolean(key(packageName, userId, permission), true);
    }

    public static void setVirtualPermissionAllowed(
            Context context,
            String packageName,
            int userId,
            String permission,
            boolean allowed
    ) {
        if (context == null || packageName == null || permission == null) {
            return;
        }
        preferences(context)
                .edit()
                .putBoolean(key(packageName, userId, permission), allowed)
                .apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static String key(String packageName, int userId, String permission) {
        return KEY_PREFIX + userId + ":" + packageName + ":" + permission;
    }
}
