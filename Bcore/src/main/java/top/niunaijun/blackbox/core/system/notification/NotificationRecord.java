package top.niunaijun.blackbox.core.system.notification;

import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;


public class NotificationRecord {
    public final Map<String, NotificationChannel> mNotificationChannels = new HashMap<>();
    public final Map<String, NotificationChannelGroup> mNotificationChannelGroups = new HashMap<>();
    public final Set<NotificationKey> mIds = new HashSet<>();

    public static final class NotificationKey {
        public final String tag;
        public final int id;

        public NotificationKey(String tag, int id) {
            this.tag = tag;
            this.id = id;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof NotificationKey)) {
                return false;
            }
            NotificationKey other = (NotificationKey) obj;
            return id == other.id && Objects.equals(tag, other.tag);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tag, id);
        }
    }
}
