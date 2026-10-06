package top.niunaijun.blackbox.proxy;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.RemoteException;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.entity.am.PendingResultData;
import top.niunaijun.blackbox.proxy.record.ProxyBroadcastRecord;
import top.niunaijun.blackbox.utils.Slog;


public class ProxyBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "ProxyBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        intent.setExtrasClassLoader(context.getClassLoader());
        Slog.d(TAG, "PROXY_BROADCAST_RECEIVED action=" + intent.getAction()
                + " package=" + intent.getPackage()
                + " component=" + intent.getComponent());
        ProxyBroadcastRecord record = ProxyBroadcastRecord.create(intent);
        if (record.mIntent == null) {
            Slog.d(TAG, "PROXY_BROADCAST_DROP_NO_TARGET action=" + intent.getAction());
            return;
        }
        Slog.d(TAG, "PROXY_BROADCAST_ROUTE userId=" + record.mUserId
                + " targetAction=" + record.mIntent.getAction()
                + " targetPackage=" + record.mIntent.getPackage()
                + " targetComponent=" + record.mIntent.getComponent());
        PendingResult pendingResult = goAsync();
        try {
            BlackBoxCore.getBActivityManager().scheduleBroadcastReceiver(record.mIntent, new PendingResultData(pendingResult), record.mUserId);
        } catch (RemoteException e) {
            pendingResult.finish();
        }
    }
}