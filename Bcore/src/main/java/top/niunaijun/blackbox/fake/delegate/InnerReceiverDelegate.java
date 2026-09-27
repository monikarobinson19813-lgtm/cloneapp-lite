package top.niunaijun.blackbox.fake.delegate;

import android.content.IIntentReceiver;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Process;
import android.os.RemoteException;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import black.android.app.BRLoadedApkReceiverDispatcherInnerReceiver;
import black.android.content.BRIIntentReceiver;
import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;


public class InnerReceiverDelegate extends IIntentReceiver.Stub {
    public static final String TAG = "InnerReceiverDelegate";

    private static final Map<IBinder, InnerReceiverDelegate> sInnerReceiverDelegate = new HashMap<>();
    private final WeakReference<IIntentReceiver> mIntentReceiver;

    private InnerReceiverDelegate(IIntentReceiver iIntentReceiver) {
        this.mIntentReceiver = new WeakReference<>(iIntentReceiver);
    }

    public static InnerReceiverDelegate getDelegate(IBinder iBinder) {
        return sInnerReceiverDelegate.get(iBinder);
    }

    public static IIntentReceiver createProxy(IIntentReceiver base) {
        if (base instanceof InnerReceiverDelegate) {
            return base;
        }
        final IBinder iBinder = base.asBinder();
        InnerReceiverDelegate delegate = sInnerReceiverDelegate.get(iBinder);
        if (delegate == null) {
            try {
                iBinder.linkToDeath(new DeathRecipient() {
                    @Override
                    public void binderDied() {
                        sInnerReceiverDelegate.remove(iBinder);
                        iBinder.unlinkToDeath(this, 0);
                    }
                }, 0);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            delegate = new InnerReceiverDelegate(base);
            sInnerReceiverDelegate.put(iBinder, delegate);
        }
        return delegate;
    }

    @Override
    public void performReceive(Intent intent, int resultCode, String data, Bundle extras, boolean ordered, boolean sticky, int sendingUser) throws RemoteException {
        intent.setExtrasClassLoader(BlackBoxCore.getApplication().getClassLoader());
        ProxyBroadcastRecord proxyBroadcastRecord = ProxyBroadcastRecord.create(intent);
        Intent perIntent;
        if (proxyBroadcastRecord.mIntent != null) {
            proxyBroadcastRecord.mIntent.setExtrasClassLoader(BlackBoxCore.getApplication().getClassLoader());
            perIntent = proxyBroadcastRecord.mIntent;
        } else {
            perIntent = intent;
        }
        IIntentReceiver iIntentReceiver = mIntentReceiver.get();
        if (iIntentReceiver != null) {
            if (BuildCompat.isU()) {
                try {
                    // Android 14's ActivityThread carries an assumeDelivered bit
                    // outside the legacy IIntentReceiver Binder signature. Our proxy
                    // cannot receive that bit, so dispatch through the framework
                    // InnerReceiver overload with explicit completion enabled.
                    // ReceiverDispatcher.mIIntentReceiver already points at this
                    // proxy binder, which keeps finishReceiver() on the token AMS
                    // actually registered.
                    BRLoadedApkReceiverDispatcherInnerReceiver.get(iIntentReceiver)
                            .performReceive(
                                    perIntent,
                                    resultCode,
                                    data,
                                    extras,
                                    ordered,
                                    sticky,
                                    false,
                                    sendingUser,
                                    Process.INVALID_UID,
                                    null);
                    return;
                } catch (Throwable t) {
                    Slog.w(TAG, "Android 14 registered-receiver completion path failed; falling back", t);
                }
            }
            BRIIntentReceiver.get(iIntentReceiver).performReceive(
                    perIntent, resultCode, data, extras, ordered, sticky, sendingUser);
        }
    }
}
