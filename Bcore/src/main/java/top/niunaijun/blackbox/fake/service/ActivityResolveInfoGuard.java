package top.niunaijun.blackbox.fake.service;

import android.content.pm.ResolveInfo;
public final class ActivityResolveInfoGuard {
    private ActivityResolveInfoGuard() {
    }

    public static boolean isUsable(ResolveInfo resolveInfo) {
        return resolveInfo != null
                && resolveInfo.activityInfo != null
                && resolveInfo.activityInfo.packageName != null
                && !resolveInfo.activityInfo.packageName.isEmpty()
                && resolveInfo.activityInfo.name != null
                && !resolveInfo.activityInfo.name.isEmpty();
    }
}
