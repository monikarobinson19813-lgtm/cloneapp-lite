package top.niunaijun.blackbox.fake.service;

import java.util.List;

public final class ResolveInfoLogSummary {
    private ResolveInfoLogSummary() {
    }

    public static String summarize(String operation, List<?> resolves) {
        int count = resolves == null ? 0 : resolves.size();
        return operation + ": count=" + count;
    }
}
