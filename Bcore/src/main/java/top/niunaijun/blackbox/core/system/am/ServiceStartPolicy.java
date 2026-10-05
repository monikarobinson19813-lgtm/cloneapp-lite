package top.niunaijun.blackbox.core.system.am;

import android.app.Service;
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

    public static int supportedForegroundServiceType(int guestType) {
        int supported = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            supported |= ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA;
        }
        return guestType & supported;
    }

    /**
     * Read the guest service's declared foreground-service type without binding
     * the core module to one Android SDK field shape.
     */
    public static int readForegroundServiceType(ServiceInfo serviceInfo) {
        if (serviceInfo == null) {
            return ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE;
        }
        try {
            Method getter = ServiceInfo.class.getMethod("getForegroundServiceType");
            Object value = getter.invoke(serviceInfo);
            if (value instanceof Integer) {
                return (Integer) value;
            }
        } catch (Throwable ignored) {
        }
        try {
            Field field = ServiceInfo.class.getField("foregroundServiceType");
            return field.getInt(serviceInfo);
        } catch (Throwable ignored) {
            return ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE;
        }
    }
}
