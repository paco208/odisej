package si.odisej.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;

import si.odisej.R;

/**
 * 7 tankih stolpcev za zadnjih 7 dni: današnji v poudarni barvi, ostali sivi; brez osi in
 * številk, pod vsakim ena črka dneva. Za bralnik zaslona: opis s števili po dnevih.
 */
public class WeekBarsView extends View {

    private final Paint barToday = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint barPast = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final float density;
    private int[] counts = new int[7];
    private String[] letters = new String[7];

    public WeekBarsView(Context context, AttributeSet attrs) {
        super(context, attrs);
        density = getResources().getDisplayMetrics().density;
        barToday.setColor(ContextCompat.getColor(context, R.color.od_accent));
        barPast.setColor(ContextCompat.getColor(context, R.color.od_dot_idle));
        label.setColor(ContextCompat.getColor(context, R.color.od_text_secondary));
        label.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 11,
                getResources().getDisplayMetrics()));
        label.setTextAlign(Paint.Align.CENTER);
        label.setTypeface(ResourcesCompat.getFont(context, R.font.odisej_regular));
    }

    /** @param counts 7 vrednosti, od najstarejšega dne do danes */
    public void setData(int[] counts, LocalDate today) {
        this.counts = counts.clone();
        Locale locale = getResources().getConfiguration().getLocales().get(0);
        StringBuilder desc = new StringBuilder();
        for (int i = 0; i < 7; i++) {
            DayOfWeek dow = today.minusDays(6 - i).getDayOfWeek();
            letters[i] = dow.getDisplayName(TextStyle.NARROW, locale);
            if (i > 0) {
                desc.append(", ");
            }
            desc.append(dow.getDisplayName(TextStyle.SHORT, locale)).append(' ').append(counts[i]);
        }
        setContentDescription(getResources().getString(R.string.week_description, desc));
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float labelHeight = 18 * density;
        float gap = 8 * density;
        float barWidth = 4 * density;
        float minBar = 2 * density;
        float chartHeight = getHeight() - labelHeight - gap;
        float slot = getWidth() / 7f;

        int max = 0;
        for (int c : counts) {
            max = Math.max(max, c);
        }
        for (int i = 0; i < 7; i++) {
            float cx = slot * i + slot / 2f;
            float h = max > 0 ? Math.max(minBar, chartHeight * counts[i] / max) : minBar;
            rect.set(cx - barWidth / 2f, chartHeight - h, cx + barWidth / 2f, chartHeight);
            canvas.drawRoundRect(rect, barWidth / 2f, barWidth / 2f, i == 6 ? barToday : barPast);
            if (letters[i] != null) {
                canvas.drawText(letters[i], cx, getHeight() - 4 * density, label);
            }
        }
    }
}
