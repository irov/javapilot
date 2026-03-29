package org.pilot.sdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionManager;
import android.os.Bundle;

import androidx.annotation.Nullable;

/**
 * Transparent helper Activity that requests MediaProjection permission
 * via the system dialog and delivers the result back to the SDK.
 */
public class PilotScreenCaptureActivity extends Activity {
    private static final int REQUEST_CODE = 7291;

    @Nullable
    static volatile ScreenCaptureCallback s_callback;

    interface ScreenCaptureCallback {
        void onScreenCaptureResult(int resultCode, @Nullable Intent data);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            // Activity was recreated (e.g., config change) — just finish
            finish();
            return;
        }

        MediaProjectionManager mgr = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mgr == null) {
            deliverResult(Activity.RESULT_CANCELED, null);
            return;
        }

        try {
            startActivityForResult(mgr.createScreenCaptureIntent(), REQUEST_CODE);
        } catch (Exception e) {
            deliverResult(Activity.RESULT_CANCELED, null);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE) {
            deliverResult(resultCode, data);
        }
    }

    private void deliverResult(int resultCode, @Nullable Intent data) {
        ScreenCaptureCallback cb = s_callback;
        s_callback = null;
        if (cb != null) {
            cb.onScreenCaptureResult(resultCode, data);
        }
        finish();
    }
}
