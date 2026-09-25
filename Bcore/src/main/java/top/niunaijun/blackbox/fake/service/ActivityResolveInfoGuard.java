package top.niunaijun.blackbox.fake.service;

import android.content.pm.ResolveInfo;
import android.text.TextUtils;

public final class ActivityResolveInfoGuard {
    private ActivityResolveInfoGuard() {
    }

    public static boolean isUsable(ResolveInfo resolveInfo) {
        return resolveInfo != null
                && resolveInfo.activityInfo != null
                && !TextUtils.isEmpty(resolveInfo.activityInfo.packageName)
                && !TextUtils.isEmpty(resolveInfo.activityInfo.name);
    }
}
