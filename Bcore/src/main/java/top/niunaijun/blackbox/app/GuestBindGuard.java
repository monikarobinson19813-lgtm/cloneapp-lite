package top.niunaijun.blackbox.app;

import android.os.ConditionVariable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import top.niunaijun.blackbox.entity.AppConfig;

/** Reject mismatched guest identities before accessing any guest private files. */
public final class GuestBindGuard {
    private GuestBindGuard() { }

    public static void requireRequest(AppConfig config, String packageName, String processName) {
        if (config == null || config.userId < 0
                || config.packageName == null || config.packageName.isEmpty()
                || config.processName == null || config.processName.isEmpty()
                || !Objects.equals(config.packageName, packageName)
                || !Objects.equals(config.processName, processName)) {
            throw new IllegalStateException("Missing or mismatched guest binding identity");
        }
    }

    public static void requireCompatible(AppConfig existing, AppConfig proposed) {
        if (proposed == null) {
            throw new IllegalStateException("Missing guest process configuration");
        }
        requireRequest(proposed, proposed.packageName, proposed.processName);
        if (existing != null && (existing.userId != proposed.userId
                || !Objects.equals(existing.packageName, proposed.packageName)
                || !Objects.equals(existing.processName, proposed.processName))) {
            throw new IllegalStateException("Reject cross-guest process reuse");
        }
    }

    /** Main thread callback always signals the blocked worker even if binding fails. */
    public static void runAndSignal(Runnable bind, ConditionVariable completion,
                                    AtomicReference<Throwable> failure) {
        try {
            bind.run();
        } catch (Throwable error) {
            failure.set(error);
        } finally {
            completion.open();
        }
    }

    public static void rethrowFailure(Throwable error) {
        if (error == null) return;
        if (error instanceof Error) throw (Error) error;
        if (error instanceof RuntimeException) throw (RuntimeException) error;
        throw new IllegalStateException("Guest application binding failed", error);
    }
}
