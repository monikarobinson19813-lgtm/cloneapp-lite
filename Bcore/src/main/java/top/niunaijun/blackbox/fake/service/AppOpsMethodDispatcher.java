package top.niunaijun.blackbox.fake.service;

/**
 * Testable, platform-independent AppOps dispatch rule. Operations excluded from
 * the exact compatibility set always execute on the real service. Token-bearing
 * start/finish, void and SyncNotedAppOp methods must never synthesize int results.
 */
public final class AppOpsMethodDispatcher {
    private AppOpsMethodDispatcher() {}

    public interface RealCall {
        Object call() throws Throwable;
    }

    public static Object invoke(String name, Class<?> returnType, String publicOpName,
                                int allowedMode, RealCall realCall) throws Throwable {
        boolean intReturn = returnType == int.class || returnType == Integer.class;
        boolean compatibleMethod = name != null && (
                name.startsWith("checkOperation")
                || "checkAudioOperation".equals(name)
                || "checkOpNoThrow".equals(name)
                || "noteOperation".equals(name)
                || "noteProxyOperation".equals(name)
                || "noteOp".equals(name)
                || "noteOpNoThrow".equals(name));
        if (intReturn && compatibleMethod && AppOpsAllowlist.isAllowedPublicOp(publicOpName)) {
            return allowedMode;
        }
        return realCall.call();
    }
}
