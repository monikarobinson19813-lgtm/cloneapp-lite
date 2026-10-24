package top.niunaijun.blackbox.fake.service;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.entity.AppConfig;
import top.niunaijun.blackbox.utils.Md5Utils;

/**
 * One stable, synthetic telephony identity per virtual user.
 *
 * This replaces the existing host-package-only synthetic ID; it never reads
 * physical telephony identifiers. Do not silently fall back to virtual U0
 * when a guest process has no verified AppConfig.
 */
public final class GuestTelephonyIdentity {

    private GuestTelephonyIdentity() {
    }

    public static String forCurrentGuest() {
        AppConfig config = BActivityThread.getAppConfig();
        if (config == null) {
            throw new IllegalStateException("No guest AppConfig for synthetic device ID");
        }
        return fromGuestConfig(BlackBoxCore.getHostPkg(), config);
    }

    /** Pure derivation function, shared by all five existing telephony hooks. */
    public static String fromGuestConfig(String hostPackage, AppConfig config) {
        if (config == null || config.userId < 0
                || config.packageName == null || config.packageName.isEmpty()) {
            throw new IllegalStateException("Invalid guest AppConfig for synthetic device ID");
        }
        if (hostPackage == null || hostPackage.isEmpty()) {
            throw new IllegalStateException("Missing host package for synthetic device ID");
        }
        // Keep the original lowercase 32-character MD5-hex return format.
        String id = Md5Utils.md5(hostPackage + ":" + config.userId);
        if (id == null) {
            throw new IllegalStateException("Cannot derive synthetic device ID");
        }
        return id;
    }
}
