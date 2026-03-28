package org.pilot.sdk;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

final class PilotStreamOverlayView extends View {
    private static final long TAP_RELEASE_MS = 90L;
    private static final long TAP_HIDE_MS = 420L;
    private static final long RELEASE_HIDE_MS = 260L;

    private final Handler m_handler = new Handler(Looper.getMainLooper());
    private final Paint m_outerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint m_innerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private boolean m_visible = false;
    private boolean m_pressed = false;
    private float m_centerX = 0f;
    private float m_centerY = 0f;

    private final Runnable m_releaseRunnable = () -> {
        m_pressed = false;
        invalidate();
    };
    private final Runnable m_hideRunnable = () -> {
        m_visible = false;
        m_pressed = false;
        invalidate();
    };

    PilotStreamOverlayView(@NonNull Context context) {
        super(context);
        init();
    }

    PilotStreamOverlayView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setClickable(false);
        setFocusable(false);
        setWillNotDraw(false);

        m_outerPaint.setStyle(Paint.Style.FILL);
        m_outerPaint.setColor(0x66FFFFFF);

        m_innerPaint.setStyle(Paint.Style.FILL);
        m_innerPaint.setColor(0xFFFF7043);
    }

    void showTap(float x, float y) {
        updatePosition(x, y);
        m_pressed = true;
        m_visible = true;
        invalidate();

        m_handler.removeCallbacks(m_releaseRunnable);
        m_handler.removeCallbacks(m_hideRunnable);
        m_handler.postDelayed(m_releaseRunnable, TAP_RELEASE_MS);
        m_handler.postDelayed(m_hideRunnable, TAP_HIDE_MS);
    }

    void showPress(float x, float y) {
        updatePosition(x, y);
        m_pressed = true;
        m_visible = true;
        m_handler.removeCallbacks(m_releaseRunnable);
        m_handler.removeCallbacks(m_hideRunnable);
        invalidate();
    }

    void showRelease(float x, float y) {
        updatePosition(x, y);
        m_pressed = false;
        m_visible = true;
        m_handler.removeCallbacks(m_releaseRunnable);
        m_handler.removeCallbacks(m_hideRunnable);
        m_handler.postDelayed(m_hideRunnable, RELEASE_HIDE_MS);
        invalidate();
    }

    void clearIndicator() {
        m_handler.removeCallbacks(m_releaseRunnable);
        m_handler.removeCallbacks(m_hideRunnable);
        m_visible = false;
        m_pressed = false;
        invalidate();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        if (!m_visible) {
            return;
        }

        float outerRadius = dp(m_pressed ? 28f : 22f);
        float innerRadius = dp(m_pressed ? 12f : 8f);

        canvas.drawCircle(m_centerX, m_centerY, outerRadius, m_outerPaint);
        canvas.drawCircle(m_centerX, m_centerY, innerRadius, m_innerPaint);
    }

    private void updatePosition(float x, float y) {
        m_centerX = x;
        m_centerY = y;
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }
}