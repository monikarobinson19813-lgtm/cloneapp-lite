package top.niunaijun.blackbox.fake.delegate;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IIntentReceiver;
import android.content.Intent;

import java.lang.ref.WeakReference;

import black.android.app.BRLoadedApkReceiverDispatcher;
import black.android.app.BRLoadedApkReceiverDispatcherInnerReceiver;
import black.android.content.BRBroadcastReceiver;
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Android 14 registered-receiver bridge.
 *
 * Keeps the framework LoadedApk ReceiverDispatcher.InnerReceiver registered with
 * ActivityManager so ActivityThread can preserve assumeDelivered/sender metadata
 * and the framework PendingResult token. Only virtual-intent unwrapping happens
 * here, one layer later at BroadcastReceiver.onReceive().
 */
public final class RegisteredReceiverBridge extends BroadcastReceiver {
    private static final String TAG = "RegisteredReceiverBridge";

    private final BroadcastReceiver mTarget;

    public RegisteredReceiverBridge(BroadcastReceiver target) {
        if (target == null) {
            throw new IllegalArgumentException("target == null");
        }
        mTarget = target;
    }

    public static boolean install(IIntentReceiver frameworkReceiver) {
        if (frameworkReceiver == null) {
            return false;
        }
        try {
            WeakReference<?> dispatcherRef =
                    BRLoadedApkReceiverDispatcherInnerReceiver.get(frameworkReceiver).mDispatcher();
            Object dispatcher = dispatcherRef != null ? dispatcherRef.get() : null;
            if (dispatcher == null) {
                Slog.w(TAG, "ReceiverDispatcher unavailable; keeping framework receiver unchanged");
                return false;
            }

            BroadcastReceiver current =
                    BRLoadedApkReceiverDispatcher.get(dispatcher).mReceiver();
            if (current == null) {
                Slog.w(TAG, "ReceiverDispatcher has no BroadcastReceiver target");
                return false;
            }
            if (current instanceof RegisteredReceiverBridge) {
                return true;
            }

            BRLoadedApkReceiverDispatcher.get(dispatcher)
                    ._set_mReceiver(new RegisteredReceiverBridge(current));
            return true;
        } catch (Throwable t) {
            Slog.w(TAG, "Unable to install Android 14 registered-receiver bridge", t);
            return false;
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Intent deliveredIntent = unwrap(intent, context != null ? context.getClassLoader() : null);

        BroadcastReceiver.PendingResult frameworkPending =
                BRBroadcastReceiver.get(this).getPendingResult();
        BRBroadcastReceiver.get(mTarget).setPendingResult(frameworkPending);

        try {
            mTarget.onReceive(context, deliveredIntent);
        } finally {
            // If the target called goAsync(), its pending result is now null.
            // Mirror that state back so ReceiverDispatcher does not finish early.
            BroadcastReceiver.PendingResult targetPending =
                    BRBroadcastReceiver.get(mTarget).getPendingResult();
            BRBroadcastReceiver.get(this).setPendingResult(targetPending);
        }
    }

    static Intent unwrap(Intent intent, ClassLoader classLoader) {
        if (intent == null) {
            return null;
        }

        if (classLoader != null) {
            intent.setExtrasClassLoader(classLoader);
        }
        ProxyBroadcastRecord proxyRecord = ProxyBroadcastRecord.create(intent);
        Intent delivered = proxyRecord.mIntent != null ? proxyRecord.mIntent : intent;
        if (classLoader != null) {
            delivered.setExtrasClassLoader(classLoader);
        }
        return delivered;
    }

    BroadcastReceiver targetForTest() {
        return mTarget;
    }
}
