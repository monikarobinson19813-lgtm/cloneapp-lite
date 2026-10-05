package top.niunaijun.blackbox.app.dispatcher;

import android.app.Service;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.os.Build;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public final class JobServiceCompat {
    private JobServiceCompat() {}

    static Boolean onStartJob(Service service, JobParameters params) throws Exception {
        if (service instanceof JobService) return ((JobService) service).onStartJob(params);
        return invokeEngine(findJobServiceEngine(service), "onStartJob", params);
    }

    static Boolean onStopJob(Service service, JobParameters params) throws Exception {
        if (service instanceof JobService) return ((JobService) service).onStopJob(params);
        return invokeEngine(findJobServiceEngine(service), "onStopJob", params);
    }

    private static Object findJobServiceEngine(Service service) throws Exception {
        if (service == null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null;
        return findAssignableFieldValue(service, Class.forName("android.app.job.JobServiceEngine"));
    }

    static Object findAssignableFieldValue(Object target, Class<?> assignableType) {
        if (target == null || assignableType == null) return null;
        for (Class<?> current = target.getClass(); current != null && current != Object.class; current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                try {
                    field.setAccessible(true);
                    Object value = field.get(target);
                    if (value != null && assignableType.isInstance(value)) return value;
                } catch (Throwable ignored) {
                }
            }
        }
        return null;
    }

    private static Boolean invokeEngine(Object engine, String methodName, JobParameters params) throws Exception {
        if (engine == null) return null;
        Method method = engine.getClass().getMethod(methodName, JobParameters.class);
        method.setAccessible(true);
        Object result = method.invoke(engine, params);
        return result instanceof Boolean ? (Boolean) result : null;
    }
}
