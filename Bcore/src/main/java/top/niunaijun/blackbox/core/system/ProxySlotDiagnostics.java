package top.niunaijun.blackbox.core.system;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Small pure helper for CLF-6 proxy-slot diagnostics.
 *
 * This class does not change allocation behavior. It only recognizes which host
 * ProxyActivity slot a recent Android task refers to so tests and diagnostics can
 * detect a stale-task collision.
 */
public final class ProxySlotDiagnostics {
    private static final String PROXY_PREFIX =
            "top.niunaijun.blackbox.proxy.ProxyActivity$P";
    private static final String TRANSPARENT_PROXY_PREFIX =
            "top.niunaijun.blackbox.proxy.TransparentProxyActivity$P";

    private ProxySlotDiagnostics() {
    }

    public static int parseProxySlot(String className) {
        if (className == null) {
            return -1;
        }

        String suffix;
        if (className.startsWith(PROXY_PREFIX)) {
            suffix = className.substring(PROXY_PREFIX.length());
        } else if (className.startsWith(TRANSPARENT_PROXY_PREFIX)) {
            suffix = className.substring(TRANSPARENT_PROXY_PREFIX.length());
        } else {
            return -1;
        }

        if (suffix.isEmpty()) {
            return -1;
        }

        try {
            int slot = Integer.parseInt(suffix);
            return slot >= 0 ? slot : -1;
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static Set<Integer> collectProxySlots(Collection<String> classNames) {
        Set<Integer> slots = new HashSet<>();
        if (classNames == null) {
            return slots;
        }
        for (String className : classNames) {
            int slot = parseProxySlot(className);
            if (slot >= 0) {
                slots.add(slot);
            }
        }
        return slots;
    }

    public static boolean hasRecentTaskCollision(int candidateSlot, Set<Integer> recentTaskSlots) {
        return candidateSlot >= 0
                && recentTaskSlots != null
                && recentTaskSlots.contains(candidateSlot);
    }
}
