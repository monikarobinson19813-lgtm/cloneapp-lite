package top.niunaijun.blackbox.fake.delegate;

import android.app.Activity;
import android.app.Application;
import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.util.Log;
import android.view.WindowManager;

import java.lang.reflect.Field;

import black.android.app.BRActivity;
import black.android.app.BRActivityThread;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.fake.hook.HookManager;
import top.niunaijun.blackbox.fake.hook.IInjectHook;
import top.niunaijun.blackbox.fake.service.HCallbackProxy;
import top.niunaijun.blackbox.fake.service.IActivityClientProxy;
import top.niunaijun.blackbox.utils.HackAppUtils;
import top.niunaijun.blackbox.utils.compat.ActivityCompat;
import top.niunaijun.blackbox.utils.compat.ActivityManagerCompat;
import top.niunaijun.blackbox.utils.compat.ContextCompat;

public final class AppInstrumentation extends BaseInstrumentationDelegate implements IInjectHook {

    private static final String TAG = AppInstrumentation.class.getSimpleName();

    private static AppInstrumentation sAppInstrumentation;

    public static AppInstrumentation get() {
        if (sAppInstrumentation == null) {
            synchronized (AppInstrumentation.class) {
                if (sAppInstrumentation == null) {
                    sAppInstrumentation = new AppInstrumentation();
                }
            }
        }
        return sAppInstrumentation;
    }

    public AppInstrumentation() {
    }

    @Override
    public void injectHook() {
        try {
            Instrumentation mInstrumentation = getCurrInstrumentation();
            if (mInstrumentation == this || checkInstrumentation(mInstrumentation))
                return;
            mBaseInstrumentation = (Instrumentation) mInstrumentation;
            BRActivityThread.get(BlackBoxCore.mainThread())._set_mInstrumentation(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Instrumentation getCurrInstrumentation() {
        Object currentActivityThread = BlackBoxCore.mainThread();
        return BRActivityThread.get(currentActivityThread).mInstrumentation();
    }

    @Override
    public boolean isBadEnv() {
        return !checkInstrumentation(getCurrInstrumentation());
    }

    private boolean checkInstrumentation(Instrumentation instrumentation) {
        if (instrumentation instanceof AppInstrumentation) {
            return true;
        }
        Class<?> clazz = instrumentation.getClass();
        if (Instrumentation.class.equals(clazz)) {
            return false;
        }
        do {
            assert clazz != null;
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
                if (Instrumentation.class.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    try {
                        Object obj = field.get(instrumentation);
                        if ((obj instanceof AppInstrumentation)) {
                            return true;
                        }
                    } catch (Exception e) {
                        return false;
                    }
                }
            }
            clazz = clazz.getSuperclass();
        } while (!Instrumentation.class.equals(clazz));
        return false;
    }

    private void checkHCallback() {
        HookManager.get().checkEnv(HCallbackProxy.class);
    }

    private void checkActivity(Activity activity) {
        Log.d(TAG, "callActivityOnCreate: " + activity.getClass().getName());
        HackAppUtils.enableQQLogOutput(activity.getPackageName(), activity.getClassLoader());
        checkHCallback();
        HookManager.get().checkEnv(IActivityClientProxy.class);
        ActivityInfo info = BRActivity.get(activity).mActivityInfo();
        ContextCompat.fix(activity);
        ActivityCompat.fix(activity);
        if (info.theme != 0) {
            activity.getTheme().applyStyle(info.theme, true);
        }
        ActivityManagerCompat.setActivityOrientation(activity, info.screenOrientation);
    }

    private static boolean isVoipActivity(Activity activity) {
        return activity != null && activity.getClass().getName().endsWith(".VoipActivityV2");
    }

    private static String diagnosticIdentity(Object object) {
        return object == null ? "null" : Integer.toHexString(System.identityHashCode(object));
    }

    private static void logVoipLifecycle(String event, Activity activity) {
        if (!isVoipActivity(activity)) return;
        try {
            WindowManager.LayoutParams attrs = activity.getWindow() == null ? null : activity.getWindow().getAttributes();
            Object decor = activity.getWindow() == null ? null : activity.getWindow().getDecorView();
            Object decorToken = activity.getWindow() == null || activity.getWindow().getDecorView() == null
                    ? null : activity.getWindow().getDecorView().getWindowToken();
            Log.d(TAG, "CLF9_VOIP_LIFECYCLE"
                    + " event=" + event
                    + " activity=" + diagnosticIdentity(activity)
                    + " taskId=" + activity.getTaskId()
                    + " finishing=" + activity.isFinishing()
                    + " destroyed=" + activity.isDestroyed()
                    + " window=" + diagnosticIdentity(activity.getWindow())
                    + " decor=" + diagnosticIdentity(decor)
                    + " decorToken=" + diagnosticIdentity(decorToken)
                    + " attrsToken=" + diagnosticIdentity(attrs == null ? null : attrs.token)
                    + " title=" + (attrs == null ? "null" : String.valueOf(attrs.getTitle())));
        } catch (Throwable t) {
            Log.w(TAG, "CLF9_VOIP_LIFECYCLE event=" + event + " log_error=" + t.getClass().getSimpleName());
        }
    }

    @Override
    public Application newApplication(ClassLoader cl, String className, Context context) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        ContextCompat.fix(context);

        return super.newApplication(cl, className, context);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle, PersistableBundle persistentState) {
        checkActivity(activity);
        super.callActivityOnCreate(activity, icicle, persistentState);
        logVoipLifecycle("CREATE", activity);
    }

    @Override
    public void callActivityOnCreate(Activity activity, Bundle icicle) {
        checkActivity(activity);
        super.callActivityOnCreate(activity, icicle);
        logVoipLifecycle("CREATE", activity);
    }

    @Override
    public void callActivityOnStart(Activity activity) {
        super.callActivityOnStart(activity);
        logVoipLifecycle("START", activity);
    }

    @Override
    public void callActivityOnResume(Activity activity) {
        super.callActivityOnResume(activity);
        logVoipLifecycle("RESUME", activity);
    }

    @Override
    public void callActivityOnPause(Activity activity) {
        logVoipLifecycle("PAUSE_BEFORE", activity);
        super.callActivityOnPause(activity);
        logVoipLifecycle("PAUSE_AFTER", activity);
    }

    @Override
    public void callActivityOnStop(Activity activity) {
        logVoipLifecycle("STOP_BEFORE", activity);
        super.callActivityOnStop(activity);
    }

    @Override
    public void callActivityOnDestroy(Activity activity) {
        logVoipLifecycle("DESTROY_BEFORE", activity);
        super.callActivityOnDestroy(activity);
        logVoipLifecycle("DESTROY_AFTER", activity);
    }

    @Override
    public void callApplicationOnCreate(Application app) {
        checkHCallback();
        super.callApplicationOnCreate(app);
    }

    public Activity newActivity(ClassLoader cl, String className, Intent intent) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        try {
            return super.newActivity(cl, className, intent);
        } catch (ClassNotFoundException e) {
            return mBaseInstrumentation.newActivity(cl, className, intent);
        }
    }
}
