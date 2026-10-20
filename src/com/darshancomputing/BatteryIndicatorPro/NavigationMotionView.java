package com.darshancomputing.BatteryIndicatorPro;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.Choreographer;
import android.view.View;
import android.view.ViewGroup;

/** One moving selection pill; no input interception or independent page gestures. */
public final class NavigationMotionView extends View implements Choreographer.FrameCallback {
    private final NavigationSpring selection = new NavigationSpring(0, 18);
    private final NavigationSpring[] presses = {
        new NavigationSpring(1, 26), new NavigationSpring(1, 26), new NavigationSpring(1, 26)
    };
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF pill = new RectF();
    private ViewGroup[] items;
    private boolean motionEnabled = true, scheduled, tracking;
    private float trackingOffset;
    private long lastFrame;

    public NavigationMotionView(Context context, AttributeSet attributes) {
        super(context, attributes);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    void bind(ViewGroup[] items) {
        this.items = items;
        invalidate();
    }

    void setPillColor(int color) {
        if (paint.getColor() == color) return;
        paint.setColor(color);
        invalidate();
    }

    void setMotionEnabled(boolean enabled) {
        if (motionEnabled == enabled) return;
        motionEnabled = enabled;
        if (!enabled) finish();
    }

    void moveTo(float position, boolean animate) {
        tracking = false;
        if (motionEnabled && animate && getWindowVisibility() == VISIBLE) {
            selection.retarget(position);
            requestFrame();
        } else {
            selection.snap(position);
            invalidate();
        }
    }

    void beginTracking(float pagePosition) {
        tracking = true;
        trackingOffset = (float) selection.position - pagePosition;
    }

    void track(float pagePosition) {
        if (!tracking) return;
        selection.snap(Math.max(0, Math.min(2, pagePosition + trackingOffset)));
        invalidate();
    }

    void endTracking(float target, float pagesPerSecond) {
        tracking = false;
        selection.velocity = pagesPerSecond;
        moveTo(target, true);
    }

    void press(int index, boolean pressed) {
        presses[index].retarget(pressed && motionEnabled ? 0.96 : 1);
        if (motionEnabled) requestFrame();
        else {
            presses[index].snap(1);
            applyPresses();
        }
    }

    private void requestFrame() {
        if (scheduled || getWindowVisibility() != VISIBLE) return;
        scheduled = true;
        lastFrame = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }

    @Override
    public void doFrame(long frameTimeNanos) {
        double dt = lastFrame == 0 ? 0 : Math.max(0, (frameTimeNanos - lastFrame) / 1_000_000_000.0);
        lastFrame = frameTimeNanos;
        if (!tracking) selection.advance(dt);
        boolean active = !tracking && !selection.isAtRest();
        for (NavigationSpring spring : presses) {
            spring.advance(dt);
            active |= !spring.isAtRest();
        }
        applyPresses();
        invalidate();
        if (motionEnabled && active && getWindowVisibility() == VISIBLE)
            Choreographer.getInstance().postFrameCallback(this);
        else {
            scheduled = false;
            lastFrame = 0;
        }
    }

    private void applyPresses() {
        if (items == null) return;
        for (int i = 0; i < items.length; i++) {
            float scale = (float) presses[i].position;
            items[i].setScaleX(scale);
            items[i].setScaleY(scale);
        }
    }

    void finish() {
        Choreographer.getInstance().removeFrameCallback(this);
        scheduled = tracking = false;
        lastFrame = 0;
        selection.snap(selection.target);
        for (NavigationSpring spring : presses) spring.snap(1);
        applyPresses();
        invalidate();
    }

    @Override
    protected void onWindowVisibilityChanged(int visibility) {
        super.onWindowVisibilityChanged(visibility);
        if (visibility != VISIBLE) finish();
    }

    @Override
    protected void onDetachedFromWindow() {
        finish();
        super.onDetachedFromWindow();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (items == null || items[0].getWidth() == 0) return;
        float progress = (float) Math.max(0, Math.min(items.length - 1, selection.position));
        int first = (int) progress;
        int next = Math.min(items.length - 1, first + 1);
        float fraction = progress - first;
        // Read real item bounds so RTL, safe insets and rotation need no spring reset.
        float from = items[first].getLeft() + items[first].getWidth() / 2f;
        float to = items[next].getLeft() + items[next].getWidth() / 2f;
        float center = from + (to - from) * fraction;
        View icon = items[first].getChildAt(0);
        float density = getResources().getDisplayMetrics().density;
        float top = items[first].getTop() + icon.getTop() - getTop();
        pill.set(center - 32 * density, top, center + 32 * density, top + 32 * density);
        canvas.drawRoundRect(pill, 16 * density, 16 * density, paint);
    }
}
