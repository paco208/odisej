package si.odisej.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import si.odisej.R;

/**
 * Tanek krog okoli števila sekund: lok se gladko krajša od polnega do nič. Riše po uri
 * (Choreographer), ne prek animatorja — je kazalnik napredka, zato teče tudi, ko so
 * animacije v sistemu izklopljene.
 */
public class CountdownRingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF oval = new RectF();
    private long startedAt;
    private long durationMs;
    private boolean running;

    public CountdownRingView(Context context, AttributeSet attrs) {
        super(context, attrs);
        float stroke = 2 * getResources().getDisplayMetrics().density;
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(stroke);
        track.setColor(ContextCompat.getColor(context, R.color.od_hairline));
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(stroke);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(ContextCompat.getColor(context, R.color.od_text));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    /** @param startedAtElapsed SystemClock.elapsedRealtime() ob začetku */
    public void start(long startedAtElapsed, long durationMs) {
        this.startedAt = startedAtElapsed;
        this.durationMs = durationMs;
        running = true;
        postInvalidateOnAnimation();
    }

    public void stop() {
        running = false;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float inset = arc.getStrokeWidth();
        oval.set(inset, inset, getWidth() - inset, getHeight() - inset);
        canvas.drawOval(oval, track);
        if (!running) {
            return;
        }
        long left = Math.max(0, startedAt + durationMs - SystemClock.elapsedRealtime());
        float fraction = durationMs > 0 ? (float) left / durationMs : 0f;
        canvas.drawArc(oval, -90f, 360f * fraction, false, arc);
        if (left > 0) {
            postInvalidateOnAnimation();
        }
    }
}
