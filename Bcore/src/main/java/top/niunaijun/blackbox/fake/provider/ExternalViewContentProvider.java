package top.niunaijun.blackbox.fake.provider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.Binder;
import android.os.Handler;
import android.os.Looper;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.provider.OpenableColumns;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.core.system.BProcessManagerService;
import top.niunaijun.blackbox.core.system.ProcessRecord;
import top.niunaijun.blackbox.proxy.ProxyManifest;
import top.niunaijun.blackbox.utils.Slog;

/**
 * Read-only bridge for temporary external-view files.
 *
 * Android URI grants protect external viewers. Calls that originate from another
 * virtual process share the host UID, so we additionally resolve the calling PID
 * back to its virtual user and reject cross-user reads.
 */
public class ExternalViewContentProvider extends ContentProvider {
    private static final String TAG = "ExternalViewProvider";
    private static final String USER_PREFIX = "user-";

    public static Uri getUriForFile(int userId, File file) {
        if (file == null) {
            return null;
        }
        return new Uri.Builder()
                .scheme("content")
                .authority(ProxyManifest.getExternalViewProvider())
                .appendPath(USER_PREFIX + userId)
                .appendPath(file.getName())
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        ResolvedFile resolved = resolveAndEnforce(uri);
        return resolved == null ? null : "application/pdf";
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {
        ResolvedFile resolved = resolveAndEnforce(uri);
        if (resolved == null) {
            return null;
        }
        MatrixCursor cursor = new MatrixCursor(
                new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE}, 1);
        cursor.addRow(new Object[]{resolved.file.getName(), resolved.file.length()});
        return cursor;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode)
            throws FileNotFoundException {
        if (!"r".equals(mode)) {
            throw new FileNotFoundException("External view provider is read-only");
        }
        final ResolvedFile resolved = resolveAndEnforce(uri);
        if (resolved == null || !resolved.file.isFile()) {
            throw new FileNotFoundException("External view file not found");
        }

        Slog.i(TAG, "EXTERNAL_VIEW_TEMP_OPEN userId=" + resolved.userId
                + " callerUid=" + Binder.getCallingUid()
                + " callerPid=" + Binder.getCallingPid()
                + " file=" + resolved.file.getAbsolutePath());

        Handler handler = new Handler(Looper.getMainLooper());
        try {
            return ParcelFileDescriptor.open(
                    resolved.file,
                    ParcelFileDescriptor.MODE_READ_ONLY,
                    handler,
                    e -> {
                        boolean deleted = resolved.file.delete();
                        File parent = resolved.file.getParentFile();
                        if (parent != null) {
                            String[] children = parent.list();
                            if (children != null && children.length == 0) {
                                parent.delete();
                            }
                        }
                        Slog.i(TAG, "EXTERNAL_VIEW_TEMP_DELETED userId=" + resolved.userId
                                + " deleted=" + deleted
                                + " file=" + resolved.file.getAbsolutePath());
                    });
        } catch (java.io.IOException e) {
            FileNotFoundException notFound = new FileNotFoundException(
                    "Unable to open external-view file");
            notFound.initCause(e);
            throw notFound;
        }
    }

    private ResolvedFile resolveAndEnforce(Uri uri) {
        List<String> segments = uri.getPathSegments();
        if (segments.size() != 2 || !segments.get(0).startsWith(USER_PREFIX)) {
            throw new SecurityException("Malformed external-view URI");
        }

        final int requestedUserId;
        try {
            requestedUserId = Integer.parseInt(
                    segments.get(0).substring(USER_PREFIX.length()));
        } catch (NumberFormatException e) {
            throw new SecurityException("Malformed external-view user");
        }

        int callingUid = Binder.getCallingUid();
        if (callingUid == Process.myUid()) {
            ProcessRecord record = BProcessManagerService.get()
                    .findProcessByPid(Binder.getCallingPid());
            int callingVirtualUserId = record == null ? -1 : record.userId;
            if (!ExternalViewAccessPolicy.isAllowed(
                    true, callingVirtualUserId, requestedUserId)) {
                Slog.w(TAG, "EXTERNAL_VIEW_CROSS_USER_DENIED requestedUserId="
                        + requestedUserId + " callingVirtualUserId=" + callingVirtualUserId
                        + " callerPid=" + Binder.getCallingPid());
                throw new SecurityException("Cross-user external-view access denied");
            }
        }

        File root = new File(BlackBoxCore.getContext().getCacheDir(),
                "external-view/" + USER_PREFIX + requestedUserId);
        File file = new File(root, segments.get(1));
        try {
            File canonicalRoot = root.getCanonicalFile();
            File canonicalFile = file.getCanonicalFile();
            if (!canonicalFile.getParentFile().equals(canonicalRoot)) {
                throw new SecurityException("External-view path escaped user root");
            }
            return new ResolvedFile(requestedUserId, canonicalFile);
        } catch (Exception e) {
            if (e instanceof SecurityException) {
                throw (SecurityException) e;
            }
            throw new SecurityException("Unable to resolve external-view file", e);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values,
                      @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("Read-only provider");
    }

    private static final class ResolvedFile {
        final int userId;
        final File file;

        ResolvedFile(int userId, File file) {
            this.userId = userId;
            this.file = file;
        }
    }
}
