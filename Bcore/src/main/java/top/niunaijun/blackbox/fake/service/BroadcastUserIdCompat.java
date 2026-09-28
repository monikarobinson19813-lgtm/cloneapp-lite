package top.niunaijun.blackbox.fake.service;

/**
 * Keeps virtual broadcast routing separate from the real Android user boundary.
 *
 * Android 14's IActivityManager broadcastIntent/broadcastIntentWithFeature
 * signatures place the framework user id in the final argument.  Guest
 * UserHandle values (including USER_ALL / -1) must not escape to the host
 * ActivityManager.
 */
public final class BroadcastUserIdCompat {
    private BroadcastUserIdCompat() {
    }

    public static boolean rewriteLastUserId(Object[] args, int hostUserId) {
        if (args == null || args.length == 0) {
            return false;
        }

        int userIdIndex = args.length - 1;
        if (!(args[userIdIndex] instanceof Integer)) {
            return false;
        }

        args[userIdIndex] = hostUserId;
        return true;
    }
}
