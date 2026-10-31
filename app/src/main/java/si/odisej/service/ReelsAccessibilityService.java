package si.odisej.service;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.time.LocalDate;

import si.odisej.BuildConfig;
import si.odisej.action.BackAction;
import si.odisej.action.BlockAction;
import si.odisej.action.BlockOverlay;
import si.odisej.action.BlockPolicy;
import si.odisej.action.Debouncer;
import si.odisej.detect.AccessibilityUiNode;
import si.odisej.detect.DetectionRules;
import si.odisej.detect.ReelsDetector;
import si.odisej.detect.ScreenState;
import si.odisej.settings.Prefs;

/**
 * Instagram dogodek → filter paketa → throttle → koren → ReelsDetector → ukrep.
 */
public class ReelsAccessibilityService extends AccessibilityService {

    private static final String TAG = "OdisejService";
    /** Največ ena klasifikacija na toliko ms (content change pride večkrat na sekundo). */
    private static final long THROTTLE_MS = 300;
    /** Po ukrepu toliko časa ne ukrepamo znova (animacija prehoda). */
    private static final long DEBOUNCE_MS = 1500;

    private final TreeDumper dumper = new TreeDumper();
    private final ReelsDetector detector = new ReelsDetector();
    private final Debouncer debouncer = new Debouncer(DEBOUNCE_MS);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable classifyRunnable = this::classifyNow;

    private Prefs prefs;
    private BlockOverlay overlay;
    private boolean classifyPending;
    private long lastClassifyAt;
    private ScreenState lastState = ScreenState.OTHER;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = new Prefs(this);
        overlay = new BlockOverlay(this);
        dumper.attach(this);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(classifyRunnable);
        if (overlay != null) {
            overlay.hide();
        }
        dumper.detach(this);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Z2, obramba v globino: XML packageNames že filtrira, a ne zaupamo samo njemu.
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !DetectionRules.INSTAGRAM_PACKAGE.contentEquals(pkg)) {
            return;
        }
        // Trailing throttle: vse dogodke v oknu združimo v eno klasifikacijo na koncu okna,
        // ki prebere takrat najnovejše drevo. Zadnje stanje zaslona tako nikoli ne uide.
        if (classifyPending) {
            return;
        }
        classifyPending = true;
        long delay = Math.max(0, lastClassifyAt + THROTTLE_MS - SystemClock.uptimeMillis());
        handler.postDelayed(classifyRunnable, delay);

        if (BuildConfig.DEBUG) {
            AccessibilityNodeInfo root = instagramRoot();
            if (root != null) {
                dumper.onEvent(event.getEventType(), root);
            }
        }
    }

    private void classifyNow() {
        classifyPending = false;
        if (prefs == null) {
            return;
        }
        lastClassifyAt = SystemClock.uptimeMillis();
        AccessibilityNodeInfo root = instagramRoot();
        if (root == null) {
            return;
        }
        ScreenState state = detector.classify(new AccessibilityUiNode(root));
        if (BuildConfig.DEBUG && state != lastState) {
            Log.d(TAG, "stanje " + lastState + " -> " + state);
        }
        lastState = state;

        if (prefs.isPaused(System.currentTimeMillis())) {
            overlay.hide();
            return;
        }
        BlockAction action = BlockPolicy.decide(state, prefs.getMode(), prefs.isDmReelsAllowed());
        if (action == BlockAction.OVERLAY) {
            // Overlay je idempotenten (ostane, dokler smo v Reelsih), zato brez debounce.
            if (overlay.show()) {
                recordBlock();
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "overlay prikazan");
                }
            }
            return;
        }
        overlay.hide();
        if (action == BlockAction.NONE || !debouncer.tryAcquire(SystemClock.uptimeMillis())) {
            return;
        }
        boolean done = BackAction.perform(this, root, action);
        if (done) {
            recordBlock();
        }
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "ukrep " + action + (done ? " izveden" : " NI uspel"));
        }
    }

    private void recordBlock() {
        prefs.recordBlock(LocalDate.now().toEpochDay());
    }

    /**
     * Koren aktivnega okna, a samo če je iz Instagrama. Q4: aktivno okno je lahko tuje (npr.
     * prekrivno okno druge aplikacije), čeprav je dogodek prišel iz Instagrama.
     */
    private AccessibilityNodeInfo instagramRoot() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return null;
        }
        CharSequence rootPkg = root.getPackageName();
        if (rootPkg == null || !DetectionRules.INSTAGRAM_PACKAGE.contentEquals(rootPkg)) {
            dumper.onForeignRoot(rootPkg);
            return null;
        }
        return root;
    }

    @Override
    public void onInterrupt() {
        // Nimamo povratne informacije (zvok, vibracija), ki bi jo bilo treba prekiniti.
    }
}
