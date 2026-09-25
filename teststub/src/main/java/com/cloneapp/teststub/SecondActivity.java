package com.cloneapp.teststub;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class SecondActivity extends Activity {
    private static final String TAG = "CloneAppTestStub";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String marker = getIntent().getStringExtra("marker");
        boolean fromNotification = getIntent().getBooleanExtra("from_notification", false);

        TextView text = new TextView(this);
        text.setText("SECOND_ACTIVITY marker=" + marker + " notification=" + fromNotification);
        text.setContentDescription("second-activity-status");
        setContentView(text);

        Log.i(
                TAG,
                "TESTSTUB_SECOND_ACTIVITY marker=" + marker + " notification=" + fromNotification
        );
    }
}
