package top.niunaijun.blackbox.fake.delegate;

public final class RegisteredReceiverDeliveryCompat {
    private static final int ANDROID_14_API = 34;

    private RegisteredReceiverDeliveryCompat() {
    }

    public static boolean mustUseFrameworkDispatcher(int sdkInt) {
        return sdkInt >= ANDROID_14_API;
    }
}
