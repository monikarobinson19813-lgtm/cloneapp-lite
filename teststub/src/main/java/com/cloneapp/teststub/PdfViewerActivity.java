package com.cloneapp.teststub;

import android.app.Activity;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class PdfViewerActivity extends Activity {
    private static final String TAG = "CloneAppTestStub";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Uri uri = getIntent().getData();
        TextView status = new TextView(this);
        status.setContentDescription("pdf-viewer-status");
        setContentView(status);

        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IllegalStateException("ContentResolver returned null stream");
            }

            byte[] header = new byte[5];
            int read = in.read(header);
            boolean valid = read == 5 && "%PDF-".equals(new String(header, StandardCharsets.US_ASCII));

            if (!valid) {
                throw new IllegalStateException("PDF header mismatch");
            }

            status.setText("PDF_OK " + uri);
            Log.i(TAG, "TESTSTUB_PDF_OK uri=" + uri);
        } catch (Exception e) {
            status.setText("PDF_FAIL " + e.getClass().getSimpleName() + ": " + e.getMessage());
            Log.e(TAG, "TESTSTUB_PDF_FAIL uri=" + uri, e);
        }
    }
}
