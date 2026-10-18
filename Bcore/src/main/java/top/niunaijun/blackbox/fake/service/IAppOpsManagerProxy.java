package top.niunaijun.blackbox.fake.service;

import android.app.AppOpsManager;
import android.content.Context;
import android.os.IBinder;

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

        // No blanket permission grants and no fail-open exception recovery.
        MethodParameterUtils.replaceFirstAppPkg(args);
        MethodParameterUtils.replaceLastUid(args);
        return super.invoke(proxy, method, args);
    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    /**
     * For int-returning check/note methods only: apply approved exact op public
     * names. Object/void returns and all start/finish calls MUST hit the real
     * service so token/session lifecycle and typed results remain intact.
     */
    private static Object decideOrForward(Object who, Method method, Object[] args)
            throws Throwable {
        String name = method.getName();
        boolean check = name.startsWith("checkOperation")
                || "checkAudioOperation".equals(name)
                || "checkOpNoThrow".equals(name);
        boolean note = "noteOperation".equals(name)
                || "noteProxyOperation".equals(name)
                || "noteOp".equals(name)
                || "noteOpNoThrow".equals(name);
        if ((check || note)
                && (method.getReturnType() == int.class || method.getReturnType() == Integer.class)
                && args != null && args.length > 0 && args[0] instanceof Integer
                && AppOpsAllowlist.isAllowedPublicOp(getOpPublicName((Integer) args[0]))) {
            return AppOpsManager.MODE_ALLOWED;
        }
        return invokeReal(who, method, args);
    }

    private static Object invokeReal(Object who, Method method, Object[] args)
            throws Throwable {
        try {
            return method.invoke(who, args);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw e.getCause();
        }
    }

    @ProxyMethod("noteProxyOperation")
    public static class NoteProxyOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return decideOrForward(who, method, args);
        }
    }

    @ProxyMethod("checkPackage")
    public static class CheckPackage extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return invokeReal(who, method, args);
        }
    }

    @ProxyMethod("checkOperation")
    public static class CheckOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return decideOrForward(who, method, args);
        }
    }

    @ProxyMethod("checkOperationForDevice")
    public static class CheckOperationForDevice extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return decideOrForward(who, method, args);
        }
    }

    @ProxyMethod("noteOperation")
    public static class NoteOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return decideOrForward(who, method, args);
        }
    }

    @ProxyMethod("checkOpNoThrow")
    public static class CheckOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return decideOrForward(who, method, args);
        }
    }

    @ProxyMethod("startOp")
    public static class StartOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return invokeReal(who, method, args);
        }
    }

    @ProxyMethod("startOpNoThrow")
    public static class StartOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return invokeReal(who, method, args);
        }
    }

    @ProxyMethod("finishOp")
    public static class FinishOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return invokeReal(who, method, args);
        }
    }

    @ProxyMethod("noteOp")
    public static class NoteOp extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return decideOrForward(who, method, args);
        }
    }

    @ProxyMethod("noteOpNoThrow")
    public static class NoteOpNoThrow extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return decideOrForward(who, method, args);
        }
    }

    @ProxyMethod("checkOperationRaw")
    public static class CheckOperationRaw extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return decideOrForward(who, method, args);
        }
    }

    @ProxyMethod("checkAudioOperation")
    public static class CheckAudioOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return decideOrForward(who, method, args);
        }
    }

    @ProxyMethod("startOperation")
    public static class StartOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return invokeReal(who, method, args);
        }
    }

    @ProxyMethod("finishOperation")
    public static class FinishOperation extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            return invokeReal(who, method, args);
        }
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
