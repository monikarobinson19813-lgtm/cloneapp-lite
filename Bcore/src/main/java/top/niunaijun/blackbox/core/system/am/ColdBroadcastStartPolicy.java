package top.niunaijun.blackbox.core.system.am;

/**
 * Limits broadcast-triggered guest cold starts to push delivery only.
 *
 * Starting a missing guest for every registered system broadcast would create
 * unnecessary proxy processes. WhatsApp's cold wake path is expected to enter
 * through the standard Google push receive action.
 */
final class ColdBroadcastStartPolicy {
    static final String GOOGLE_PUSH_RECEIVE_ACTION = "com.google.android.c2dm.intent.RECEIVE";
    static final long START_TIMEOUT_MS = 2500L;

    private ColdBroadcastStartPolicy() {
    }

    static boolean shouldColdStart(String action) {
        return GOOGLE_PUSH_RECEIVE_ACTION.equals(action);
    }
}
