package top.niunaijun.blackbox.core.system.am;

import android.content.pm.ServiceInfo;
import android.os.Build;

/** Android-version policy for virtual services that requested foreground execution. */
public final class ServiceStartPolicy {
    private ServiceStartPolicy() {
    }

    public static boolean shouldUseForegroundService(boolean requireForeground, int sdkInt) {
        return requireForeground && sdkInt >= Build.VERSION_CODES.O;
    }

    /**
     * Keep this bridge deliberately narrow. Voice calls need microphone now;
     * additional FGS types get added only with their own manifest and regression coverage.
     */
    public static int supportedForegroundServiceType(int guestType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return 0;
        }
        return guestType & ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
    }
}
