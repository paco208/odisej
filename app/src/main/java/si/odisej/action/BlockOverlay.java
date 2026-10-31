package si.odisej.action;

import android.accessibilityservice.AccessibilityService;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Looper;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityNodeInfo;

import si.odisej.R;
import si.odisej.detect.DetectionRules;

/**
 * v2 ukrep: prekrivno okno čez cel zaslon prek TYPE_ACCESSIBILITY_OVERLAY (ne potrebuje
 * SYSTEM_ALERT_WINDOW). Odstrani se, ko zaznava ni več REELS_* ali ko Instagram ni več spredaj.
 */
public final class BlockOverlay {

    /** Kako pogosto preverimo, ali je Instagram še spredaj (samo ime paketa). */
    private static final long FOREGROUND_CHECK_MS = 1000;
    /** Po BACK počakamo, da se prejšnji zaslon prikaže, nato kliknemo zavihek. */
    private static final long AFTER_BACK_CLICK_MS = 600;

    private final AccessibilityService service;
    private final WindowManager windowManager;
    private final AudioManager audioManager;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable foregroundCheck = this::checkForeground;
    private final AudioFocusRequest focusRequest;

    private View view;

    public BlockOverlay(AccessibilityService service) {
        this.service = service;
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

    public boolean isShowing() {
        return view != null;
    }

    /** @return true, če se je overlay pravkar prikazal (prej ni bil). */
    @SuppressLint("InflateParams") // samostojno okno nima starša; parametre da WindowManager
    public boolean show() {
        if (view != null) {
            return false;
        }
        Context themed = new ContextThemeWrapper(service, R.style.Theme_Odisej);
        view = LayoutInflater.from(themed).inflate(R.layout.overlay_block, null);
        view.findViewById(R.id.overlay_btn_messages).setOnClickListener(
                v -> leaveTo(DetectionRules.DIRECT_TAB_ID));
        view.findViewById(R.id.overlay_btn_home).setOnClickListener(
                v -> leaveTo(DetectionRules.HOME_TAB_ID));

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                // NOT_FOCUSABLE: sistemski "nazaj" gre Instagramu (umik iz Reelsov skrije overlay).
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT);
        windowManager.addView(view, lp);
        // Predvajalniki se ob izgubi zvočnega fokusa običajno ustavijo — reel za overlayem utihne.
        audioManager.requestAudioFocus(focusRequest);
        handler.postDelayed(foregroundCheck, FOREGROUND_CHECK_MS);
        return true;
    }

    public void hide() {
        handler.removeCallbacks(foregroundCheck);
        if (view == null) {
            return;
        }
        windowManager.removeViewImmediate(view);
        view = null;
        audioManager.abandonAudioFocusRequest(focusRequest);
    }

    /**
     * Servis dobiva dogodke samo iz Instagrama (Z2): če uporabnik pritisne Home, tega ne bi
     * izvedeli in overlay bi ostal čez zaganjalnik. Zato, dokler je prikazan, preverjamo
     * SAMO ime paketa aktivnega okna — vsebine ne beremo.
     */
    private void checkForeground() {
        if (view == null) {
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

    /** Skrije overlay in pelje na zavihek; če ga ni (viewer čez cel zaslon), najprej nazaj. */
    private void leaveTo(String tabId) {
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
