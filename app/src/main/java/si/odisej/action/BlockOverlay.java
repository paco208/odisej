package si.odisej.action;

import android.accessibilityservice.AccessibilityService;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowInsets;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.animation.LinearInterpolator;
import android.view.animation.PathInterpolator;
import android.widget.TextView;


import si.odisej.R;
import si.odisej.detect.DetectionRules;
import si.odisej.ui.BreathCurve;
import si.odisej.ui.BreathingCircleView;

/**
 * v2 ukrep, "trenutek premora": črn zaslon čez Instagram prek TYPE_ACCESSIBILITY_OVERLAY
 * (ne potrebuje SYSTEM_ALERT_WINDOW). En vdih in izdih (3 s), šele nato stavek in gumba —
 * da ne tapneš refleksno. Odstrani se, ko zaznava ni več REELS_* ali Instagram ni spredaj.
 */
public final class BlockOverlay {

    private static final long FADE_IN_MS = 200;
    private static final long FADE_OUT_MS = 150;
    private static final long LABEL_CROSSFADE_MS = 300;
    private static final long REVEAL_DELAY_MS = 150;
    private static final long REVEAL_MS = 350;
    /** Brez animacij krog miruje na tej razširjenosti. */
    private static final float STILL_EXPANSION = 0.5f;
    /** Kako pogosto preverimo, ali je Instagram še spredaj (samo ime paketa). */
    private static final long FOREGROUND_CHECK_MS = 1000;
    /** Po BACK počakamo, da se prejšnji zaslon prikaže, nato kliknemo zavihek. */
    private static final long AFTER_BACK_CLICK_MS = 600;

    private final AccessibilityService service;
    private final WindowManager windowManager;
    private final AudioManager audioManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable foregroundCheck = this::checkForeground;
    private final Runnable reveal = this::reveal;
    private final AudioFocusRequest focusRequest;
    /** Uporabnik je sam zapustil overlay (gumb) — servis začne ohlajanje. */
    private final Runnable onUserLeft;

    private View view;
    private ValueAnimator breath;
    private boolean hiding;

    public BlockOverlay(AccessibilityService service, Runnable onUserLeft) {
        this.service = service;
        this.onUserLeft = onUserLeft;
        windowManager = (WindowManager) service.getSystemService(Context.WINDOW_SERVICE);
        audioManager = (AudioManager) service.getSystemService(Context.AUDIO_SERVICE);
        focusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .setOnAudioFocusChangeListener(change -> { })
                .build();
    }

    /**
     * Če je overlay prikazan (ali ravno izginja), ga obdrži in vrne true. Servis tako ne šteje
     * iste blokade dvakrat, ko se uporabnik med izhodno animacijo vrne v Reelse.
     */
    public boolean keepIfShowing() {
        if (view == null) {
            return false;
        }
        if (hiding) {
            hiding = false;
            view.animate().cancel();
            view.setAlpha(1f);
            startForegroundChecks();
        }
        return true;
    }

    /** @param reelsToday števec blokad danes, vključno s to (za stavek po dihanju) */
    @SuppressLint("InflateParams") // samostojno okno nima starša; parametre da WindowManager
    public void show(int reelsToday) {
        if (view != null) {
            return;
        }
        Context themed = new ContextThemeWrapper(service, R.style.Theme_Odisej);
        view = LayoutInflater.from(themed).inflate(R.layout.overlay_block, null);
        view.findViewById(R.id.overlay_btn_messages).setOnClickListener(
                v -> leaveTo(DetectionRules.DIRECT_TAB_ID));
        view.findViewById(R.id.overlay_btn_home).setOnClickListener(
                v -> leaveTo(DetectionRules.HOME_TAB_ID));
        view.findViewById(R.id.overlay_btn_close).setOnClickListener(v -> closeInstagram());
        ((TextView) view.findViewById(R.id.overlay_sentence)).setText(
                service.getString(R.string.overlay_sentence, reelsToday));

        applyInsets(view);
        windowManager.addView(view, layoutParams());
        // Predvajalniki se ob izgubi zvočnega fokusa ustavijo — reel za overlayem utihne.
        audioManager.requestAudioFocus(focusRequest);
        startForegroundChecks();

        view.setAlpha(0f);
        view.animate().alpha(1f).setDuration(FADE_IN_MS).start();
        // Haptika spoštuje sistemsko nastavitev vibracij (brez FLAG_IGNORE_GLOBAL_SETTING) in ne
        // potrebuje dovoljenja VIBRATE.
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK);
        startBreath();
        // Gumbi pridejo po uri, ne ob koncu animatorja: brez animacij se animator konča takoj,
        // pavza 3 s pa mora ostati.
        handler.postDelayed(reveal, BreathCurve.TOTAL_MS);
    }

    private WindowManager.LayoutParams layoutParams() {
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                // NOT_FOCUSABLE: sistemski "nazaj" gre Instagramu (umik iz Reelsov skrije overlay).
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        // Edge-to-edge: črnina tudi pod statusno vrstico in izrezom kamere.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            lp.setFitInsetsTypes(0);
        }
        return lp;
    }

    /**
     * Okno sega pod sistemske vrstice (edge-to-edge), vsebina pa ne sme: odmik = vrstice + izrez,
     * prištet osnovnemu robu iz layouta. Deluje za kretnje in za tri gumbe.
     */
    private static void applyInsets(View root) {
        final int baseLeft = root.getPaddingLeft();
        final int baseTop = root.getPaddingTop();
        final int baseRight = root.getPaddingRight();
        final int baseBottom = root.getPaddingBottom();
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left;
            int top;
            int right;
            int bottom;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets i = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = i.left;
                top = i.top;
                right = i.right;
                bottom = i.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                top = insets.getSystemWindowInsetTop();
                right = insets.getSystemWindowInsetRight();
                bottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(baseLeft + left, baseTop + top, baseRight + right, baseBottom + bottom);
            return insets;
        });
    }

    private void startBreath() {
        BreathingCircleView circle = view.findViewById(R.id.overlay_circle);
        TextView labelA = view.findViewById(R.id.overlay_label_a);
        TextView labelB = view.findViewById(R.id.overlay_label_b);

        if (!ValueAnimator.areAnimatorsEnabled()) {
            // "Odstrani animacije": krog miruje, a premor ostane.
            circle.setExpansion(STILL_EXPANSION);
            labelA.setText(R.string.overlay_still);
            return;
        }
        labelA.setText(R.string.overlay_inhale);
        labelB.setText(R.string.overlay_exhale);
        breath = ValueAnimator.ofFloat(0f, 1f);
        breath.setDuration(BreathCurve.TOTAL_MS);
        // Gladkost je v sami krivulji (kosinus), zato tu linearni čas.
        breath.setInterpolator(new LinearInterpolator());
        final boolean[] exhaling = {false};
        breath.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            circle.setExpansion(BreathCurve.expansion(f));
            if (!exhaling[0] && BreathCurve.isExhaling(f)) {
                exhaling[0] = true;
                // Vrh vdiha: nežen pulz (tudi ta spoštuje sistemsko nastavitev vibracij).
                circle.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK);
                labelA.animate().alpha(0f).setDuration(LABEL_CROSSFADE_MS).start();
                labelB.animate().alpha(1f).setDuration(LABEL_CROSSFADE_MS).start();
            }
        });
        breath.start();
    }

    private void reveal() {
        if (view == null) {
            return;
        }
        // Napis dihanja se umakne, pojavita se stavek in gumba.
        view.findViewById(R.id.overlay_label_a).animate().alpha(0f)
                .setDuration(LABEL_CROSSFADE_MS).start();
        view.findViewById(R.id.overlay_label_b).animate().alpha(0f)
                .setDuration(LABEL_CROSSFADE_MS).start();
        View group = view.findViewById(R.id.overlay_reveal);
        group.setVisibility(View.VISIBLE);
        group.animate().alpha(1f).setStartDelay(REVEAL_DELAY_MS).setDuration(REVEAL_MS)
                .setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f)) // fast-out-slow-in
                .start();
    }

    /** Izhod s kratkim fade-outom. */
    public void hide() {
        stopForegroundChecks();
        if (view == null || hiding) {
            return;
        }
        hiding = true;
        final View leaving = view;
        leaving.animate().alpha(0f).setDuration(FADE_OUT_MS).withEndAction(() -> {
            if (view == leaving && hiding) {
                removeNow();
            }
        }).start();
    }

    /** Takojšnja odstranitev (servis se ustavlja). */
    public void hideNow() {
        stopForegroundChecks();
        if (view != null) {
            view.animate().cancel();
            removeNow();
        }
    }

    private void removeNow() {
        handler.removeCallbacks(reveal);
        if (breath != null) {
            breath.cancel();
            breath = null;
        }
        windowManager.removeViewImmediate(view);
        view = null;
        hiding = false;
        audioManager.abandonAudioFocusRequest(focusRequest);
    }

    private void startForegroundChecks() {
        handler.removeCallbacks(foregroundCheck);
        handler.postDelayed(foregroundCheck, FOREGROUND_CHECK_MS);
    }

    private void stopForegroundChecks() {
        handler.removeCallbacks(foregroundCheck);
    }

    /**
     * Servis dobiva dogodke samo iz Instagrama (Z2): če uporabnik pritisne Home, tega ne bi
     * izvedeli in overlay bi ostal čez zaganjalnik. Zato, dokler je prikazan, preverjamo
     * SAMO ime paketa aktivnega okna — vsebine ne beremo.
     */
    private void checkForeground() {
        if (view == null || hiding) {
            return;
        }
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        CharSequence pkg = root != null ? root.getPackageName() : null;
        if (pkg != null && !DetectionRules.INSTAGRAM_PACKAGE.contentEquals(pkg)) {
            hide();
            return;
        }
        handler.postDelayed(foregroundCheck, FOREGROUND_CHECK_MS);
    }

    /** "Zapri Instagram": na začetni zaslon telefona; overlay izgine (Instagram ni več spredaj). */
    private void closeInstagram() {
        onUserLeft.run();
        hide();
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_HOME);
    }

    /** Skrije overlay in pelje na zavihek; če ga ni (viewer čez cel zaslon), najprej nazaj. */
    private void leaveTo(String tabId) {
        onUserLeft.run();
        hide();
        if (BackAction.clickById(service.getRootInActiveWindow(), tabId)) {
            return;
        }
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        handler.postDelayed(
                () -> BackAction.clickById(service.getRootInActiveWindow(), tabId),
                AFTER_BACK_CLICK_MS);
    }
}
