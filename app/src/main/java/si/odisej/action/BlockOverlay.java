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
import android.view.ViewGroup;
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
import si.odisej.ui.OverlayLayout;

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
    /** Med dihanjem: središče kroga kot delež višine okna. */
    private static final float CIRCLE_CENTER_FRACTION = 0.40f;
    /** Po dihanju se okvir kroga skrči na to višino (mirujoči krog ima premer ~90dp). */
    private static final int SETTLED_CIRCLE_DP = 96;
    /** Najmanjši razmik bloka do navigacijske vrstice in pod statusno vrstico. */
    private static final int BOTTOM_GAP_DP = 48;
    private static final int TOP_GAP_DP = 16;
    /** Poravnava bloka po dihanju. */
    private static final long SETTLE_MS = 300;
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
    private ValueAnimator settle;
    private boolean hiding;
    /** Dihanje je končano in blok je (ali se ravno) poravnan na končni položaj. */
    private boolean settled;
    private int insetTop;
    private int insetBottom;

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

    /** Overlay je v oknu (prikazan ali ravno izginja). */
    public boolean isPresent() {
        return view != null;
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

        settled = false;
        applyInsets(view);
        positionBlock(view);
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
     * Okno sega pod sistemske vrstice (edge-to-edge). Blok je postavljen glede na celo okno,
     * spodnji razmik pa glede na navigacijsko vrstico (kretnje ali trije gumbi).
     * Levo/desno: izrez in vrstice v ležečem položaju.
     */
    @SuppressWarnings("deprecation") // getSystemWindowInset*: samo veja za API < 30
    private void applyInsets(View root) {
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            int left;
            int right;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                android.graphics.Insets i = insets.getInsets(
                        WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                left = i.left;
                right = i.right;
                insetTop = i.top;
                insetBottom = i.bottom;
            } else {
                left = insets.getSystemWindowInsetLeft();
                right = insets.getSystemWindowInsetRight();
                insetTop = insets.getSystemWindowInsetTop();
                insetBottom = insets.getSystemWindowInsetBottom();
            }
            v.setPadding(left, 0, right, 0);
            v.requestLayout();
            return insets;
        });
    }

    /**
     * Ob vsakem layoutu: med dihanjem je središče kroga na 40 % višine; po dihanju (ko animacija
     * poravnave ne teče) je blok na končnem položaju iz {@link OverlayLayout#settledTop}.
     */
    private void positionBlock(View root) {
        View column = root.findViewById(R.id.overlay_column);
        View circle = root.findViewById(R.id.overlay_circle);
        root.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (!settled) {
                float centerInColumn = circle.getTop() + circle.getHeight() / 2f;
                column.setTranslationY(CIRCLE_CENTER_FRACTION * (b - t) - centerInColumn);
            } else if (settle == null || !settle.isRunning()) {
                column.setTranslationY(settledTop(b - t, column.getHeight()));
            }
        });
    }

    private float settledTop(int windowHeight, int blockHeight) {
        return OverlayLayout.settledTop(windowHeight, blockHeight, insetTop, insetBottom,
                dp(BOTTOM_GAP_DP), dp(TOP_GAP_DP));
    }

    private float dp(int value) {
        return value * service.getResources().getDisplayMetrics().density;
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
        BreathingCircleView circle = view.findViewById(R.id.overlay_circle);
        View slot = view.findViewById(R.id.overlay_text_slot);
        View sentence = view.findViewById(R.id.overlay_sentence);
        View column = view.findViewById(R.id.overlay_column);

        // Napis dihanja se prelije v stavek (isti prostor pod krogom).
        view.findViewById(R.id.overlay_label_a).animate().alpha(0f)
                .setDuration(LABEL_CROSSFADE_MS).start();
        view.findViewById(R.id.overlay_label_b).animate().alpha(0f)
                .setDuration(LABEL_CROSSFADE_MS).start();
        sentence.animate().alpha(1f).setDuration(LABEL_CROSSFADE_MS).start();

        // Krog miruje (tudi brez animacij, kjer je bil na STILL_EXPANSION), da gre v manjši okvir.
        circle.setExpansion(0f);

        // En animator: okvir kroga 224 → 96dp, prostor za besedilo → višina stavka, blok → 45 %.
        final int circleFrom = circle.getHeight();
        final int circleTo = Math.round(dp(SETTLED_CIRCLE_DP));
        final int slotFrom = slot.getHeight();
        final int slotTo = sentence.getHeight();
        final int finalBlock = column.getHeight() - (circleFrom - circleTo) - (slotFrom - slotTo);
        final float fromY = column.getTranslationY();
        final float toY = settledTop(view.getHeight(), finalBlock);
        settled = true;
        settle = ValueAnimator.ofFloat(0f, 1f);
        settle.setDuration(SETTLE_MS);
        settle.setInterpolator(new PathInterpolator(0.4f, 0f, 0.2f, 1f)); // fast-out-slow-in
        settle.addUpdateListener(a -> {
            float f = (float) a.getAnimatedValue();
            ViewGroup.LayoutParams cp = circle.getLayoutParams();
            cp.height = Math.round(circleFrom + (circleTo - circleFrom) * f);
            circle.setLayoutParams(cp);
            ViewGroup.LayoutParams sp = slot.getLayoutParams();
            sp.height = Math.round(slotFrom + (slotTo - slotFrom) * f);
            slot.setLayoutParams(sp);
            column.setTranslationY(fromY + (toY - fromY) * f);
        });
        settle.start();

        View actions = view.findViewById(R.id.overlay_actions);
        actions.setVisibility(View.VISIBLE);
        actions.animate().alpha(1f).setStartDelay(REVEAL_DELAY_MS).setDuration(REVEAL_MS)
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
        if (settle != null) {
            settle.cancel();
            settle = null;
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
