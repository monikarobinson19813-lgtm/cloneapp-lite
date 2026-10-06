package top.niunaijun.blackbox.core.system.am;

import android.content.pm.ServiceInfo;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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
        return guestType & ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
    }

    /**
     * Read the guest service's declared foreground-service type without binding
     * the build to a framework field that is absent from some Android stubs.
     */
    public static int readForegroundServiceType(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return 0;
        }

        try {
            Method getter = ServiceInfo.class.getMethod("getForegroundServiceType");
            Object value = getter.invoke(serviceInfo);
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Field field = ServiceInfo.class.getField("foregroundServiceType");
            return field.getInt(serviceInfo);
        } catch (ReflectiveOperationException ignored) {
            return 0;
        }
    }
}
