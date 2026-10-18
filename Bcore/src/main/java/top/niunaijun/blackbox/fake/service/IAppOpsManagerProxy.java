package top.niunaijun.blackbox.fake.service;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.IBinder;
import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Method;

import black.android.app.BRAppOpsManager;
import black.android.os.BRServiceManager;
import black.com.android.internal.app.BRIAppOpsServiceStub;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;
import top.niunaijun.blackbox.utils.MethodParameterUtils;
import top.niunaijun.blackbox.utils.Slog;


public class IAppOpsManagerProxy extends BinderInvocationStub {
    public IAppOpsManagerProxy() {
        super(BRServiceManager.get().getService(Context.APP_OPS_SERVICE));
    }

    @Override
    protected Object getWho() {
        IBinder call = BRServiceManager.get().getService(Context.APP_OPS_SERVICE);
        return BRIAppOpsServiceStub.get().asInterface(call);
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {
        if (BRAppOpsManager.get(null)._check_mService() != null) {
            AppOpsManager appOpsManager = (AppOpsManager) BlackBoxCore.getContext().getSystemService(Context.APP_OPS_SERVICE);
            try {
                BRAppOpsManager.get(appOpsManager)._set_mService(getProxyInvocation());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        replaceSystemService(Context.APP_OPS_SERVICE);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Diagnostic-only: observe the original arguments BEFORE any guest/host translation.
        // This call must not change the existing return value or exception behavior.
        logAppOpsMap(method, args, "invoke");

        String methodName = method.getName();


        if (AppOpsReturnTypeGuard.isSyncNotedNoteOperation(
                methodName, method.getReturnType().getName())) {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            try {
                Object result = method.invoke(getBase(), args);
                String actualType = result == null ? "null" : result.getClass().getName();
                Slog.i(TAG, "APP_OPS_SYNC_NOTED_RESULT declaredType="
                        + method.getReturnType().getName()
                        + " actualType=" + actualType);
                return result;
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }

        if (methodName.startsWith("check") || 
            methodName.startsWith("note") || 
            methodName.startsWith("start")) {
            Slog.d(TAG, "AppOps invoke: Bypassing system for " + methodName + ", allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
        
        
        if (methodName.startsWith("finish")) {
            Slog.d(TAG, "AppOps invoke: Bypassing system for " + methodName);
            return null;
        }
        
        
        try {
            MethodParameterUtils.replaceFirstAppPkg(args);
            MethodParameterUtils.replaceLastUid(args);
            return super.invoke(proxy, method, args);
        } catch (SecurityException e) {
            
            Slog.w(TAG, "AppOps invoke: SecurityException caught for " + methodName + ", allowing operation", e);
            return AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            Slog.e(TAG, "AppOps invoke: Error in method " + methodName, e);
            
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public boolean transact(int code, @NonNull Parcel data, @Nullable Parcel reply, int flags)
            throws RemoteException {
        // Diagnostic only: raw Binder calls can bypass the Java invocation handler.
        // Do not read, rewind or modify either Parcel or alter the delegate result.
        logAppOpsRawTransact(code);
        return super.transact(code, data, reply, flags);
    }

    @ProxyMethod("noteProxyOperation")
    public static class NoteProxyOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkPackage")
    public static class CheckPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOperation")
    public static class CheckOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            
            
            Slog.d(TAG, "AppOps CheckOperation: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    
    @ProxyMethod("checkOperationForDevice")
    public static class CheckOperationForDevice extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            
            Slog.d(TAG, "AppOps CheckOperationForDevice: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("noteOperation")
    public static class NoteOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            
            Slog.d(TAG, "AppOps NoteOperation: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("checkOpNoThrow")
    public static class CheckOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            
            Slog.d(TAG, "AppOps CheckOpNoThrow: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    
    @ProxyMethod("startOp")
    public static class StartOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            
            Slog.d(TAG, "AppOps StartOp: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    @ProxyMethod("startOpNoThrow")
    public static class StartOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            
            Slog.d(TAG, "AppOps StartOpNoThrow: Bypassing system check, allowing operation");
            return AppOpsManager.MODE_ALLOWED;
        }
    }

    
    @ProxyMethod("finishOp")
    public static class FinishOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            try {
                int op = (int) args[0];
                String name = getOpPublicName(op);
                if (name != null && isMediaStorageOrAudioOp(name)) {
                    Slog.d(TAG, "AppOps FinishOp: Finishing operation: " + name);
                }
            } catch (Throwable ignored) {
            }
            return null;
        }
    }

    
    @ProxyMethod("noteOp")
    public static class NoteOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            try {
                int op = (int) args[0];
                String name = getOpPublicName(op);
                if (name != null && (name.contains("RECORD_AUDIO") || name.contains("AUDIO") || name.contains("MICROPHONE"))) {
                    Slog.d(TAG, "AppOps NoteOp: Allowing RECORD_AUDIO operation: " + name);
                    return AppOpsManager.MODE_ALLOWED;
                }
            } catch (Throwable ignored) {
            }
            return method.invoke(who, args);
        }
    }

    
    @ProxyMethod("noteOpNoThrow")
    public static class NoteOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            logAppOpsMap(method, args, "hook");
            try {
                int op = (int) args[0];
                String name = getOpPublicName(op);
                if (name != null && (name.contains("RECORD_AUDIO") || name.contains("AUDIO") || name.contains("MICROPHONE"))) {
                    Slog.d(TAG, "AppOps NoteOpNoThrow: Allowing RECORD_AUDIO operation: " + name);
                    return AppOpsManager.MODE_ALLOWED;
                }
            } catch (Throwable ignored) {
            }
            return method.invoke(who, args);
        }
    }

    /**
     * Throwaway instrumentation only. Never log account, package, attribution,
     * message, path or Parcel contents. The Java proxy, direct hook and raw Binder
     * paths are logged separately to make coverage gaps observable.
     */
    private static void logAppOpsMap(Method method, Object[] args, String path) {
        try {
            String methodName = method.getName();
            String phase = methodName.startsWith("check") ? "check"
                    : methodName.startsWith("note") ? "note"
                    : methodName.startsWith("start") ? "start"
                    : methodName.startsWith("finish") ? "finish" : "other";

            String opName = "not_applicable";
            int argUid = -1;
            if ("checkPackage".equals(methodName)) {
                // checkPackage(uid, packageName) has no operation code.
                argUid = integerAt(args, 0);
            } else if (isOperationMethod(methodName)) {
                // Operation argument is an int on current IAppOpsService APIs.
                // Legacy AppOpsManager variants can supply a public op string.
                Object operation = firstOperationArg(args);
                if (operation instanceof Integer) {
                    opName = resolveOpName((Integer) operation);
                } else if (operation instanceof String) {
                    String value = (String) operation;
                    opName = value.startsWith("android:") && value.matches("[a-zA-Z0-9_:./]+")
                            ? value : "unresolved_string";
                } else {
                    opName = "unresolved";
                }

                // These indices describe known API shapes only. -1 means unknown.
                // noteProxyOperation carries UID in AttributionSource, not a flat int.
                if ("checkAudioOperation".equals(methodName)) {
                    argUid = integerAt(args, 2); // (op, usage, uid, package)
                } else if (methodName.startsWith("startOperation")
                        || methodName.startsWith("finishOperation")) {
                    argUid = integerAt(args, 2); // (token, op, uid, ...)
                } else if (methodName.startsWith("checkOperation")
                        || methodName.startsWith("noteOperation")
                        || "noteOp".equals(methodName) || "noteOpNoThrow".equals(methodName)
                        || "checkOpNoThrow".equals(methodName)) {
                    argUid = integerAt(args, 1);
                }
            }

            Slog.i("APPOPS_MAP", "path=" + path
                    + " phase=" + phase
                    + " method=" + methodName
                    + " op=" + opName
                    + " callerUid=" + Binder.getCallingUid()
                    + " argUid=" + argUid
                    + " virtualUid=" + diagnosticVirtualUid()
                    + " userId=" + diagnosticVirtualUserId());
        } catch (Throwable ignored) {
            // Observation must never change the original AppOps result.
        }
    }

    private static boolean isOperationMethod(String methodName) {
        return methodName.startsWith("checkOperation")
                || methodName.startsWith("noteOperation")
                || methodName.startsWith("startOperation")
                || methodName.startsWith("finishOperation")
                || "noteProxyOperation".equals(methodName)
                || "checkAudioOperation".equals(methodName)
                || "checkOpNoThrow".equals(methodName)
                || "noteOp".equals(methodName)
                || "noteOpNoThrow".equals(methodName)
                || "startOp".equals(methodName)
                || "startOpNoThrow".equals(methodName)
                || "finishOp".equals(methodName);
    }

    private static Object firstOperationArg(Object[] args) {
        if (args == null) return null;
        for (Object arg : args) {
            if (arg instanceof Integer) return arg;
            if (arg instanceof String && ((String) arg).startsWith("android:")) return arg;
        }
        return null;
    }

    private static int integerAt(Object[] args, int position) {
        if (args == null || position >= args.length || !(args[position] instanceof Integer)) {
            return -1;
        }
        return (Integer) args[position];
    }

    private static int diagnosticVirtualUid() {
        try { return BlackBoxCore.getBUid(); }
        catch (Throwable ignored) { return -1; }
    }

    private static int diagnosticVirtualUserId() {
        try { return BlackBoxCore.getUserId(); }
        catch (Throwable ignored) { return -1; }
    }

    private static void logAppOpsRawTransact(int code) {
        try {
            Slog.i("APPOPS_MAP", "path=binder phase=raw method=transact op=unresolved"
                    + " transactionCode=" + code
                    + " callerUid=" + Binder.getCallingUid()
                    + " argUid=-1"
                    + " virtualUid=" + diagnosticVirtualUid()
                    + " userId=" + diagnosticVirtualUserId());
        } catch (Throwable ignored) {
            // No Parcel reads, return changes, or new exceptions.
        }
    }

    private static String resolveOpName(int op) {
        String name = getOpPublicName(op);
        if (name != null && !name.isEmpty()) return name;
        try {
            java.lang.reflect.Method m = AppOpsManager.class.getMethod("opToName", int.class);
            Object value = m.invoke(null, op);
            if (value != null) return value.toString();
        } catch (Throwable ignored) {
            // One or both Android op-name APIs may be hidden on this OS.
        }
        return "id:" + op;
    }

    private static boolean isMediaStorageOrAudioOp(String opPublicNameOrStr) {
        if (opPublicNameOrStr == null) return false;
        
        String n = opPublicNameOrStr.toUpperCase();
        return n.contains("READ_MEDIA")
                || n.contains("READ_EXTERNAL_STORAGE")
                || n.contains("RECORD_AUDIO")
                || n.contains("CAPTURE_AUDIO_OUTPUT")
                || n.contains("MODIFY_AUDIO_SETTINGS")
                || n.contains("AUDIO")
                || n.contains("MICROPHONE")
                || n.contains("FOREGROUND_SERVICE")
                || n.contains("SYSTEM_ALERT_WINDOW")
                || n.contains("WRITE_SETTINGS")
                || n.contains("ACCESS_FINE_LOCATION")
                || n.contains("ACCESS_COARSE_LOCATION")
                || n.contains("CAMERA")
                || n.contains("BODY_SENSORS")
                || n.contains("BLUETOOTH_SCAN")
                || n.contains("BLUETOOTH_CONNECT")
                || n.contains("BLUETOOTH_ADVERTISE")
                || n.contains("NEARBY_WIFI_DEVICES")
                || n.contains("POST_NOTIFICATIONS");
    }

    private static String getOpPublicName(int op) {
        try {
            
            java.lang.reflect.Method m = AppOpsManager.class.getMethod("opToPublicName", int.class);
            Object name = m.invoke(null, op);
            return name != null ? name.toString() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
