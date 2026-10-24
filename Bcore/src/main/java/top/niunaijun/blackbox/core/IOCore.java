package top.niunaijun.blackbox.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.os.Process;
import android.text.TextUtils;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.app.GuestBindGuard;
import top.niunaijun.blackbox.entity.AppConfig;

import top.niunaijun.blackbox.core.env.BEnvironment;
import top.niunaijun.blackbox.utils.FileUtils;
import top.niunaijun.blackbox.utils.TrieTree;


@SuppressLint("SdCardPath")
public class IOCore {
    public static final String TAG = "IOCore";

    private static final IOCore sIOCore = new IOCore();
    private static final TrieTree mTrieTree = new TrieTree();
    private static final TrieTree sBlackTree = new TrieTree();
    private final Map<String, String> mRedirectMap = new LinkedHashMap<>();

    private static final Map<String, Map<String, String>> sCachePackageRedirect = new HashMap<>();

    public static IOCore get() {
        return sIOCore;
    }

    
    public void addRedirect(String origPath, String redirectPath) {
        if (TextUtils.isEmpty(origPath) || TextUtils.isEmpty(redirectPath)) {
            throw new IllegalStateException("Empty IO redirect rule");
        }
        String existing = mRedirectMap.get(origPath);
        if (existing != null) {
            if (!existing.equals(redirectPath)) {
                throw new IllegalStateException("Conflicting IO redirect rule");
            }
            return;
        }
        File redirectFile = new File(redirectPath);
        if (!redirectFile.exists()) {
            FileUtils.mkdirs(redirectPath);
        }
        // Java records the mapping only after the native call returns.
        // NativeCore.addIORule is void: successful native registration is unverified.
        NativeCore.addIORule(origPath, redirectPath);
        mTrieTree.add(origPath);
        mRedirectMap.put(origPath, redirectPath);
    }

    public void addBlackRedirect(String path) {
        if (TextUtils.isEmpty(path))
            return;
        sBlackTree.add(path);
    }

    public String redirectPath(String path) {
        if (TextUtils.isEmpty(path))
            return path;
        if (path.contains("/blackbox/")) {
            return path;
        }
        String search = sBlackTree.search(path);
        if (!TextUtils.isEmpty(search))
            return search;

        
        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key))
            path = path.replace(key, Objects.requireNonNull(mRedirectMap.get(key)));

        return path;
    }

    public File redirectPath(File path) {
        if (path == null)
            return null;
        String pathStr = path.getAbsolutePath();
        return new File(redirectPath(pathStr));
    }

    public String redirectPath(String path, Map<String, String> rule) {
        if (TextUtils.isEmpty(path))
            return path;

        
        String key = mTrieTree.search(path);
        if (!TextUtils.isEmpty(key))
            path = path.replace(key, Objects.requireNonNull(rule.get(key)));

        return path;
    }

    public File redirectPath(File path, Map<String, String> rule) {
        if (path == null)
            return null;
        String pathStr = path.getAbsolutePath();
        return new File(redirectPath(pathStr, rule));
    }

    

    public void enableRedirect(Context context) {
        if (context == null) {
            throw new IllegalStateException("Missing guest context for IO binding");
        }
        String packageName = context.getPackageName();
        AppConfig config = BActivityThread.getAppConfig();
        GuestBindGuard.requireRequest(config, packageName, config == null ? null : config.processName);
        final int virtualUserId = config.userId;
        final int hostUserId = BlackBoxCore.getHostUserId();
        final File expectedDir = BEnvironment.getDataDir(packageName, virtualUserId);

        // Installation creates the private data directory (CreateUserExecutor).
        // Do NOT fabricate a new empty directory when an installed guest's
        // original session directory is missing.
        if (!expectedDir.isDirectory()) {
            throw new IllegalStateException("Missing installed guest data directory");
        }
        final ApplicationInfo packageInfo = BlackBoxCore.getBPackageManager()
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA, virtualUserId);
        if (packageInfo == null || !packageName.equals(packageInfo.packageName)
                || !expectedDir.getAbsolutePath().equals(packageInfo.dataDir)) {
            throw new IllegalStateException("Guest package/data directory mismatch");
        }

        final Set<String> blackRule = new HashSet<>();
        GuestRedirectPlan.prepareValidateApply(
                packageName, hostUserId, expectedDir, mRedirectMap,
                () -> {
                    Map<String, String> rule = new LinkedHashMap<>();
                    rule.put(String.format("/data/data/%s/lib", packageName), packageInfo.nativeLibraryDir);
                    rule.put(String.format("/data/user/%d/%s/lib", hostUserId, packageName), packageInfo.nativeLibraryDir);
                    rule.put(String.format("/data/data/%s", packageName), packageInfo.dataDir);
                    rule.put(String.format("/data/user/%d/%s", hostUserId, packageName), packageInfo.dataDir);

                    File profilesRoot = new File(BEnvironment.getVirtualRoot(), "profiles");
                    FileUtils.mkdirs(profilesRoot.getAbsolutePath());
                    rule.put("/data/misc/profiles", profilesRoot.getAbsolutePath());

                    File profilesCurDir = new File(profilesRoot, String.format("cur/%d/%s", virtualUserId, packageName));
                    File profilesRefDir = new File(profilesRoot, String.format("ref/%d/%s", virtualUserId, packageName));
                    FileUtils.mkdirs(profilesCurDir.getAbsolutePath());
                    FileUtils.mkdirs(profilesRefDir.getAbsolutePath());
                    rule.put(String.format("/data/misc/profiles/cur/%d/%s", virtualUserId, packageName), profilesCurDir.getAbsolutePath());
                    rule.put(String.format("/data/misc/profiles/ref/%d/%s", virtualUserId, packageName), profilesRefDir.getAbsolutePath());

                    if (BlackBoxCore.getContext().getExternalCacheDir() != null && context.getExternalCacheDir() != null) {
                        File external = BEnvironment.getExternalUserDir(virtualUserId);
                        rule.put("/sdcard", external.getAbsolutePath());
                        rule.put(String.format("/storage/emulated/%d", hostUserId), external.getAbsolutePath());
                        blackRule.add("/sdcard/Pictures");
                        blackRule.add(String.format("/storage/emulated/%d/Pictures", hostUserId));
                    }
                    if (BlackBoxCore.get().isHideRoot()) {
                        hideRoot(rule);
                    }
                    proc(rule);
                    return rule;
                },
                (from, to) -> addRedirect(from, to),
                () -> {
                    for (String key : blackRule) {
                        addBlackRedirect(key);
                    }
                    NativeCore.enableIO();
                });
    }

    private void hideRoot(Map<String, String> rule) {
        rule.put("/system/app/Superuser.apk", "/system/app/Superuser.apk-fake");
        rule.put("/sbin/su", "/sbin/su-fake");
        rule.put("/system/bin/su", "/system/bin/su-fake");
        rule.put("/system/xbin/su", "/system/xbin/su-fake");
        rule.put("/data/local/xbin/su", "/data/local/xbin/su-fake");
        rule.put("/data/local/bin/su", "/data/local/bin/su-fake");
        rule.put("/system/sd/xbin/su", "/system/sd/xbin/su-fake");
        rule.put("/system/bin/failsafe/su", "/system/bin/failsafe/su-fake");
        rule.put("/data/local/su", "/data/local/su-fake");
        rule.put("/su/bin/su", "/su/bin/su-fake");
    }

    private void proc(Map<String, String> rule) {
        int appPid = BlackBoxCore.getAppPid();
        int pid = Process.myPid();
        String selfProc = "/proc/self/";
        String proc = "/proc/" + pid + "/";

        String cmdline = new File(BEnvironment.getProcDir(appPid), "cmdline").getAbsolutePath();
        rule.put(proc + "cmdline", cmdline);
        rule.put(selfProc + "cmdline", cmdline);
    }
}
