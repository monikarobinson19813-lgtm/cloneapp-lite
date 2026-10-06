package top.niunaijun.blackbox.fake.service.context.providers;

public final class ContentProviderCallerIdentity {
    private ContentProviderCallerIdentity() {
    }

    public static boolean rewriteLegacyCallingPackage(Object[] args, String packageName) {
        if (args == null || args.length == 0 || !(args[0] instanceof String)) {
            return false;
        }
        args[0] = packageName;
        return true;
    }
}
