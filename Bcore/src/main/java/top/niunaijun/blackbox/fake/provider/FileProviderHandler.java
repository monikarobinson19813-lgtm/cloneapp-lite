package top.niunaijun.blackbox.fake.provider;

import android.content.Context;
import android.content.ContentResolver;
import android.content.pm.ProviderInfo;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import top.niunaijun.blackbox.BlackBoxCore;
import top.niunaijun.blackbox.app.BActivityThread;
import top.niunaijun.blackbox.utils.Slog;
import top.niunaijun.blackbox.utils.compat.BuildCompat;


public class FileProviderHandler {

    private static final String TAG = "FileProviderHandler";

    public static Uri materializeForExternalView(Context guestContext, Uri sourceUri,
                                                 String mimeType, int userId) {
        if (guestContext == null || sourceUri == null) {
            return null;
        }

        File directory = new File(BlackBoxCore.getContext().getCacheDir(),
                "external-view/user-" + userId);
        if (!directory.exists() && !directory.mkdirs()) {
            Slog.w(TAG, "Unable to create external-view cache directory");
            return null;
        }

        File[] staleFiles = directory.listFiles();
        if (staleFiles != null) {
            for (File staleFile : staleFiles) {
                if (staleFile.isFile()) {
                    staleFile.delete();
                }
            }
        }

        String suffix = "application/pdf".equalsIgnoreCase(mimeType) ? ".pdf" : ".bin";
        File destination = new File(directory,
                "attachment-" + System.currentTimeMillis() + suffix);
        ContentResolver resolver = guestContext.getContentResolver();

        try (InputStream input = resolver.openInputStream(sourceUri);
             OutputStream output = new FileOutputStream(destination)) {
            if (input == null) {
                return null;
            }
            byte[] buffer = new byte[64 * 1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            output.flush();
        } catch (Exception e) {
            Slog.w(TAG, "Unable to materialize external content URI: " + sourceUri, e);
            destination.delete();
            return null;
        }

        Uri bridgedUri = ExternalViewContentProvider.getUriForFile(
                userId,
                destination);
        if (bridgedUri == null) {
            destination.delete();
            return null;
        }
        Slog.i(TAG, "EXTERNAL_VIEW_TEMP_CREATED userId=" + userId
                + " file=" + destination.getAbsolutePath());
        return bridgedUri;
    }

    public static Uri convertFileUri(Context context, Uri uri) {
        if (BuildCompat.isN()) {
            File file = convertFile(context, uri);
            if (file == null)
                return null;
            return BlackBoxCore.getBStorageManager().getUriForFile(file.getAbsolutePath());
        }
        return uri;
    }

    public static File convertFile(Context context, Uri uri) {
        List<ProviderInfo> providers = BActivityThread.getProviders();
        for (ProviderInfo provider : providers) {
            try {
                File fileForUri = FileProvider.getFileForUri(context, provider.authority, uri);
                if (fileForUri != null && fileForUri.exists()) {
                    return fileForUri;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }
}
