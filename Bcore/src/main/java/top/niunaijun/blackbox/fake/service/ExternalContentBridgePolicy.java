package top.niunaijun.blackbox.fake.service;

public final class ExternalContentBridgePolicy {
    private ExternalContentBridgePolicy() {
    }

    public static boolean shouldBridge(String action, String scheme, String mimeType) {
        return "android.intent.action.VIEW".equals(action)
                && "content".equalsIgnoreCase(scheme)
                && "application/pdf".equalsIgnoreCase(mimeType);
    }
}
