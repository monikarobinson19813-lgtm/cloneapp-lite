package com.cloneapp.teststub;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String TAG = "CloneAppTestStub";
    private static final String CHANNEL_ID = "cloneapp_test_notifications";
    private static final String PREFS = "stub_state";
    private static final String KEY_MARKER = "marker";

    private String marker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String incomingMarker = getIntent().getStringExtra("marker");
        if (incomingMarker != null && !incomingMarker.trim().isEmpty()) {
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_MARKER, incomingMarker).apply();
        }
        marker = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_MARKER, "unset");

        setContentView(buildUi());

        String action = getIntent().getStringExtra("action");
        if ("post_notification".equals(action)) {
            postNotification();
        } else if ("open_pdf".equals(action)) {
            openPdf();
        } else if ("open_second".equals(action)) {
            openSecondActivity();
        } else if ("clear_user_data".equals(action)) {
            probeClearApplicationUserData();
        } else if ("sync_noted_app_op".equals(action)) {
            probeSyncNotedAppOp();
        }
    }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        root.setPadding(padding, padding, padding, padding);

        TextView status = new TextView(this);
        status.setText("TEST_STUB_READY " + marker);
        status.setContentDescription("test-stub-status");
        root.addView(status);

        Button notification = new Button(this);
        notification.setText("Post notification");
        notification.setOnClickListener(v -> postNotification());
        root.addView(notification);

        Button pdf = new Button(this);
        pdf.setText("Open PDF");
        pdf.setOnClickListener(v -> openPdf());
        root.addView(pdf);

        Button second = new Button(this);
        second.setText("Open second activity");
        second.setOnClickListener(v -> openSecondActivity());
        root.addView(second);

        Button clearData = new Button(this);
        clearData.setText("Probe clearApplicationUserData");
        clearData.setOnClickListener(v -> probeClearApplicationUserData());
        root.addView(clearData);

        Button syncNoted = new Button(this);
        syncNoted.setText("Probe SyncNotedAppOp");
        syncNoted.setOnClickListener(v -> probeSyncNotedAppOp());
        root.addView(syncNoted);

        return root;
    }

    private void postNotification() {
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "CloneApp Test Notifications",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        Intent openIntent = new Intent(this, SecondActivity.class)
                .setAction("com.cloneapp.teststub.OPEN_" + marker)
                .putExtra("marker", marker)
                .putExtra("from_notification", true);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                marker.hashCode(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("CloneApp Test " + marker)
                .setContentText("Tap to open " + marker)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);

        manager.notify(1001, builder.build());
        Log.i(TAG, "TESTSTUB_NOTIFICATION_POSTED marker=" + marker);
    }

    private void openPdf() {
        try {
            File dir = new File(getCacheDir(), "pdf");
            if (!dir.exists() && !dir.mkdirs()) {
                throw new IllegalStateException("Unable to create PDF cache directory");
            }

            File pdf = new File(dir, "sample-" + marker + ".pdf");
            String content = "%PDF-1.4\n% CloneApp deterministic test PDF " + marker + "\n%%EOF\n";
            try (FileOutputStream out = new FileOutputStream(pdf)) {
                out.write(content.getBytes(StandardCharsets.US_ASCII));
            }

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".files",
                    pdf
            );

            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/pdf")
                    .setClass(this, PdfViewerActivity.class)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            Log.i(TAG, "TESTSTUB_PDF_INTENT uri=" + uri + " marker=" + marker);
            startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "TESTSTUB_PDF_LAUNCH_FAILED marker=" + marker, e);
            throw new RuntimeException(e);
        }
    }

    private void probeClearApplicationUserData() {
        String packageName = getPackageName();
        if (!"com.cloneapp.teststub".equals(packageName)) {
            Log.e(TAG, "TESTSTUB_CLEAR_USER_DATA_ABORT unexpectedPackage=" + packageName + " marker=" + marker);
            Toast.makeText(this, "Probe aborted: unexpected package", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            ActivityManager manager = getSystemService(ActivityManager.class);
            boolean result = manager.clearApplicationUserData();
            Log.i(TAG, "TESTSTUB_CLEAR_USER_DATA_RESULT result=" + result + " marker=" + marker);
            Toast.makeText(this, "clearApplicationUserData returned " + result, Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Log.e(TAG, "TESTSTUB_CLEAR_USER_DATA_THROWN marker=" + marker, t);
            Toast.makeText(this, "clearApplicationUserData threw " + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void probeSyncNotedAppOp() {
        try {
            AppOpsManager manager = getSystemService(AppOpsManager.class);
            int mode = manager.noteOpNoThrow(
                    AppOpsManager.OPSTR_CAMERA,
                    Process.myUid(),
                    getPackageName()
            );
            Log.i(TAG, "TESTSTUB_SYNC_NOTED_APP_OP_RESULT mode=" + mode + " marker=" + marker);
            Toast.makeText(this, "SyncNotedAppOp probe returned mode " + mode, Toast.LENGTH_LONG).show();
        } catch (Throwable t) {
            Log.e(TAG, "TESTSTUB_SYNC_NOTED_APP_OP_THROWN marker=" + marker, t);
            Toast.makeText(this, "SyncNotedAppOp probe threw " + t.getClass().getSimpleName(), Toast.LENGTH_LONG).show();
        }
    }

    private void openSecondActivity() {
        startActivity(
                new Intent(this, SecondActivity.class)
                        .putExtra("marker", marker)
        );
    }
}
