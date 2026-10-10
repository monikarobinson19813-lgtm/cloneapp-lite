package top.niunaijun.blackbox.core.system.pm;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class PackageReplacementState {
    public enum BroadcastDisposition {
        MARK_REPLACING,
        KEEP_REPLACING,
        COMPLETE_REPLACEMENT,
        RELOAD
    }

    private static final String ACTION_PACKAGE_ADDED = "android.intent.action.PACKAGE_ADDED";
    private static final String ACTION_PACKAGE_REMOVED = "android.intent.action.PACKAGE_REMOVED";
    private static final String ACTION_PACKAGE_REPLACED = "android.intent.action.PACKAGE_REPLACED";

    private final Set<String> replacingPackages = ConcurrentHashMap.newKeySet();

    public boolean mark(String packageName) {
        return packageName != null && replacingPackages.add(packageName);
    }

    public void clear(String packageName) {
        if (packageName != null) {
            replacingPackages.remove(packageName);
        }
    }

    public boolean isReplacing(String packageName) {
        return packageName != null && replacingPackages.contains(packageName);
    }

    public static BroadcastDisposition classify(String action, boolean replacingExtra) {
        if (ACTION_PACKAGE_REPLACED.equals(action)) {
            return BroadcastDisposition.COMPLETE_REPLACEMENT;
        }
        if (ACTION_PACKAGE_REMOVED.equals(action) && replacingExtra) {
            return BroadcastDisposition.MARK_REPLACING;
        }
        if (ACTION_PACKAGE_ADDED.equals(action) && replacingExtra) {
            return BroadcastDisposition.KEEP_REPLACING;
        }
        return BroadcastDisposition.RELOAD;
    }
}
