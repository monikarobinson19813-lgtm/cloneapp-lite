package top.niunaijun.blackbox.fake.provider;

public final class ExternalViewAccessPolicy {
    private ExternalViewAccessPolicy() {
    }

    public static boolean isAllowed(boolean sameHostUid,
                                    int callingVirtualUserId,
                                    int requestedUserId) {
        if (!sameHostUid) {
            return true;
        }
        return callingVirtualUserId >= 0 && callingVirtualUserId == requestedUserId;
    }
}
