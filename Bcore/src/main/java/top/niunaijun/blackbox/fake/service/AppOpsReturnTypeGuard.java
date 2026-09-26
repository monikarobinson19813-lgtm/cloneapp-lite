package top.niunaijun.blackbox.fake.service;

public final class AppOpsReturnTypeGuard {
    private static final String SYNC_NOTED_APP_OP = "android.app.SyncNotedAppOp";

    private AppOpsReturnTypeGuard() {
    }

    public static boolean isSyncNotedNoteOperation(String methodName, String returnTypeName) {
        return "noteOperation".equals(methodName)
                && SYNC_NOTED_APP_OP.equals(returnTypeName);
    }
}
