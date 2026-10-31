package si.odisej.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.core.content.ContextCompat;

import si.odisej.R;

/** Poln krog v poudarni barvi; velikost določa razširjenost 0..1. Okrasni element. */
public class BreathingCircleView extends View {

    /** Polmer kot delež polovice stranice: mirovanje / vrh vdiha. */
    private static final float MIN_RADIUS = 0.40f;
    private static final float MAX_RADIUS = 0.95f;

    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private float expansion;

    public BreathingCircleView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint.setColor(ContextCompat.getColor(context, R.color.od_accent));
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void setExpansion(float expansion) {
        this.expansion = expansion;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // Polmer iz ŠIRINE: po dihanju se okvir po višini skrči (224 → 96dp), krog pa ostane enak.
        float half = getWidth() / 2f;
        float r = half * (MIN_RADIUS + (MAX_RADIUS - MIN_RADIUS) * expansion);
        canvas.drawCircle(getWidth() / 2f, getHeight() / 2f, r, paint);
    }
}
