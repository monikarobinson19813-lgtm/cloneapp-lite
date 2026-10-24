package top.niunaijun.blackbox.core;

import java.io.File;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Java-side fail-closed boundary before enabling guest native IO interception.
 * Native addIORule/enableIO return void; native acknowledgement remains UNKNOWN.
 */
public final class GuestRedirectPlan {
    private GuestRedirectPlan() { }

    public interface Registrar {
        void register(String from, String to);
    }

    public static void prepareValidateApply(String packageName, int hostUserId,
            File expectedGuestDir, Map<String, String> installed,
            Supplier<Map<String, String>> prepare, Registrar registrar, Runnable enable) {
        if (packageName == null || packageName.isEmpty() || packageName.contains("/")
                || hostUserId < 0 || expectedGuestDir == null || !expectedGuestDir.isDirectory()) {
            throw new IllegalStateException("Invalid guest redirect configuration");
        }

        // Prepare fully BEFORE changing any registered Java/native rule.
        Map<String, String> candidate = Objects.requireNonNull(prepare.get(), "Null redirect plan");
        String dest = expectedGuestDir.getAbsolutePath();
        String data = "/data/data/" + packageName;
        String userData = "/data/user/" + hostUserId + "/" + packageName;
        if (!dest.equals(candidate.get(data)) || !dest.equals(candidate.get(userData))) {
            throw new IllegalStateException("Mandatory guest data redirects missing or invalid");
        }
        for (Map.Entry<String, String> entry : candidate.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (key == null || key.isEmpty() || value == null || value.isEmpty()) {
                throw new IllegalStateException("Incomplete guest redirect rule");
            }
            String prior = installed.get(key);
            if (prior != null && !prior.equals(value)) {
                throw new IllegalStateException("Conflicting guest redirect rule");
            }
        }

        // The registrar may throw; in that case we never enable IO or bind
        // the guest. Any partial Java/native registration is process-local.
        for (Map.Entry<String, String> entry : candidate.entrySet()) {
            registrar.register(entry.getKey(), entry.getValue());
        }
        if (!dest.equals(installed.get(data)) || !dest.equals(installed.get(userData))) {
            throw new IllegalStateException("Mandatory guest redirects not registered");
        }
        enable.run();
    }
}
