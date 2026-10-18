package top.niunaijun.blackbox.fake.service;

/**
 * TEMPORARY DIAGNOSTIC ONLY. Record completion of existing real-service
 * invocations without changing AppOps operation selection, arguments, return
 * values, permission modes, or exception behavior.
 *
 * Do not log UIDs, packages, tokens, attribution chains or exception messages.
 * The 'returned' event is emitted only AFTER the real callback has completed.
 * Remove this helper after ABR accepts the forwarding evidence.
 */
public final class AppOpsForwardProof {
    private AppOpsForwardProof() {}

    public interface RealCall {
        Object call() throws Throwable;
    }

    public interface EventSink {
        void record(String event);
    }

    public static Object trace(String method, String publicOpName,
                               RealCall realCall, EventSink sink) throws Throwable {
        String context = "method=" + method
                + " op=" + (publicOpName == null ? "unresolved" : publicOpName);
        emit(sink, "path=real_enter " + context);
        try {
            Object result = realCall.call();
            String resultLabel = result instanceof Integer
                    ? "mode=" + result
                    : "return_type=" + (result == null ? "void_or_null"
                    : result.getClass().getSimpleName());
            emit(sink, "path=real_return " + context + " " + resultLabel);
            return result;
        } catch (Throwable error) {
            emit(sink, "path=real_throw " + context
                    + " error_type=" + error.getClass().getSimpleName());
            throw error;
        }
    }

    private static void emit(EventSink sink, String message) {
        // A diagnostic logger must never alter guest authorization or exceptions.
        try {
            sink.record(message);
        } catch (RuntimeException ignored) {
            // Log failure is not an AppOps failure; preserve the real call.
        }
    }
}
