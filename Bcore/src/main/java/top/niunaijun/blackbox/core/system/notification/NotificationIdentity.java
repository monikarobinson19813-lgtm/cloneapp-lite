package top.niunaijun.blackbox.core.system.notification;

import android.app.Notification;
import android.app.NotificationChannel;

/** Deterministic per-virtual-user notification identity rules. */
public final class NotificationIdentity {
    private NotificationIdentity() {}

    public static String userLabel(int userId) {
        return "User " + userId;
    }

    public static String channelId(String channelId, int userId) {
        if (channelId == null || channelId.contains(BNotificationManagerService.CHANNEL_BLACK)) {
            return channelId;
        }
        return channelId + BNotificationManagerService.CHANNEL_BLACK + userId;
    }

    public static String groupId(String groupId, int userId) {
        if (groupId == null || groupId.contains(BNotificationManagerService.GROUP_BLACK)) {
            return groupId;
        }
        return groupId + BNotificationManagerService.GROUP_BLACK + userId;
    }

    public static CharSequence labelledText(CharSequence existing, int userId) {
        String label = userLabel(userId);
        if (existing == null || existing.length() == 0) {
            return label;
        }
        if (existing.toString().contains(label)) {
            return existing;
        }
        return existing + " · " + label;
    }

    public static void applyChannelName(NotificationChannel channel, int userId) {
        channel.setName(labelledText(channel.getName(), userId));
    }

    public static void applyNotificationSubText(Notification notification, int userId) {
        if (notification == null || notification.extras == null) {
            return;
        }
        CharSequence existing = notification.extras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        notification.extras.putCharSequence(
                Notification.EXTRA_SUB_TEXT,
                labelledText(existing, userId)
        );
    }
}
