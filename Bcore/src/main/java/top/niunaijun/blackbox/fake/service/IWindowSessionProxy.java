package top.niunaijun.blackbox.fake.service;

import android.os.IInterface;
import android.util.Log;
import android.view.WindowManager;

import java.lang.reflect.Method;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.fake.hook.BinderInvocationStub;
import top.niunaijun.blackbox.fake.hook.MethodHook;
import top.niunaijun.blackbox.fake.hook.ProxyMethod;



public class IWindowSessionProxy extends BinderInvocationStub {
    public static final String TAG = "WindowSessionStub";

    private IInterface mSession;

    private static boolean isVoipWindow(WindowManager.LayoutParams lp) {
        return lp != null
                && lp.getTitle() != null
                && lp.getTitle().toString().contains("VoipActivityV2");
    }

    private static String diagnosticIdentity(Object object) {
        return object == null ? "null" : Integer.toHexString(System.identityHashCode(object));
    }

    private static void logVoipWindow(String event, WindowManager.LayoutParams lp) {
        if (!isVoipWindow(lp)) return;
        Log.d(TAG, "CLF9_VOIP_WINDOW"
                + " event=" + event
                + " lp=" + diagnosticIdentity(lp)
                + " token=" + diagnosticIdentity(lp.token)
                + " package=" + lp.packageName
                + " title=" + String.valueOf(lp.getTitle())
                + " type=" + lp.type
                + " flags=0x" + Integer.toHexString(lp.flags)
                + " format=" + lp.format
                + " gravity=" + lp.gravity
                + " size=" + lp.width + "x" + lp.height);
    }

    public IWindowSessionProxy(IInterface session) {
        super(session.asBinder());
        mSession = session;
    }

    @Override
    protected Object getWho() {
        return mSession;
    }

    @Override
    protected void inject(Object baseInvocation, Object proxyInvocation) {

    }

    @Override
    public boolean isBadEnv() {
        return false;
    }

    @Override
    public Object getProxyInvocation() {
        return super.getProxyInvocation();
    }

    @ProxyMethod("addToDisplay")
    public static class AddToDisplay extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            for (Object arg : args) {
                if (arg == null) {
                    continue;
                }
                if (arg instanceof WindowManager.LayoutParams) {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) arg;
                    logVoipWindow("ADD_BEFORE", lp);
                    lp.packageName = BlackBoxCore.getHostPkg();
                    if (BlackBoxCore.get().isDisableFlagSecure()) {
                        lp.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
                    }
                    logVoipWindow("ADD_AFTER", lp);
                }
            }
            return method.invoke(who, args);
        }
    }

    @ProxyMethod("addToDisplayAsUser")
    public static class AddToDisplayAsUser extends AddToDisplay {
    }

    @ProxyMethod("relayout")
    public static class Relayout extends MethodHook {
        @Override
        protected Object hook(Object who, Method method, Object[] args) throws Throwable {
            for (Object arg : args) {
                if (arg == null) {
                    continue;
                }
                if (arg instanceof WindowManager.LayoutParams) {
                    WindowManager.LayoutParams lp = (WindowManager.LayoutParams) arg;
                    logVoipWindow("RELAYOUT_BEFORE", lp);
                    if (BlackBoxCore.get().isDisableFlagSecure()) {
                        lp.flags &= ~WindowManager.LayoutParams.FLAG_SECURE;
                    }
                    logVoipWindow("RELAYOUT_AFTER", lp);
                }
            }
            return method.invoke(who, args);
        }
    }
}
