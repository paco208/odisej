package si.odisej.service;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import androidx.core.content.ContextCompat;

import java.time.LocalDate;

import si.odisej.BuildConfig;
import si.odisej.action.BackAction;
import si.odisej.action.BlockAction;
import si.odisej.action.BlockOverlay;
import si.odisej.action.BlockPolicy;
import si.odisej.action.Cooldown;
import si.odisej.action.Debouncer;
import si.odisej.action.OverlayStep;
import si.odisej.detect.AccessibilityUiNode;
import si.odisej.detect.DetectionRules;
import si.odisej.detect.ReelsDetector;
import si.odisej.detect.ScreenState;
import si.odisej.settings.Prefs;
import si.odisej.settings.WatchSession;

/**
 * Instagram dogodek → filter paketa → throttle → koren → ReelsDetector → ukrep.
 */
public class ReelsAccessibilityService extends AccessibilityService {

    private static final String TAG = "OdisejService";
    /** Največ ena klasifikacija na toliko ms (content change pride večkrat na sekundo). */
    private static final long THROTTLE_MS = 300;
    /** Po ukrepu toliko časa ne ukrepamo znova (animacija prehoda). */
    private static final long DEBOUNCE_MS = 1500;
    /** Med sejo gledanja: meritev časa + preverjanje, ali je Instagram še spredaj. */
    private static final long SESSION_TICK_MS = 1000;
    /** Sekunde gledanja pišemo na disk največ tako pogosto (in ob koncu seje). */
    private static final long PERSIST_SECONDS_MS = 15_000;
    /** Rezerva brez indeksa strani: poteg je končan, ko toliko časa ni novega drsenja. */
    private static final long SWIPE_SETTLE_MS = 600;

    private final TreeDumper dumper = new TreeDumper();
    private final ReelsDetector detector = new ReelsDetector();
    private final Debouncer debouncer = new Debouncer(DEBOUNCE_MS);
    /** Po odhodu z overlaya (gumb) ga toliko časa ne pokažemo in ne štejemo znova. */
    private final Cooldown overlayCooldown = new Cooldown(DEBOUNCE_MS);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable classifyRunnable = this::classifyNow;
    private final WatchSession session = new WatchSession();
    private final Runnable sessionTick = this::onSessionTick;
    private final Runnable swipeSettled = () -> session.onSwipeSettled(sessionSink());
    private final BroadcastReceiver screenOff = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            stopSession();
        }
    };
    private int pendingSeconds;
    private long lastPersistAt;

    private Prefs prefs;
    private BlockOverlay overlay;
    private boolean classifyPending;
    private long lastClassifyAt;
    private ScreenState lastState = ScreenState.OTHER;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        prefs = new Prefs(this);
        overlay = new BlockOverlay(this,
                () -> overlayCooldown.start(SystemClock.uptimeMillis()));
        dumper.attach(this);
        ContextCompat.registerReceiver(this, screenOff, new IntentFilter(Intent.ACTION_SCREEN_OFF),
                ContextCompat.RECEIVER_NOT_EXPORTED);
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(classifyRunnable);
        stopSession();
        unregisterReceiver(screenOff);
        if (overlay != null) {
            overlay.hideNow();
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
        if (event.getEventType() == AccessibilityEvent.TYPE_VIEW_SCROLLED) {
            // Drsenje rabimo samo za štetje pogledanih reelov; klasifikacije ne sproža (feed
            // pošilja drsenja neprestano, stanje pa že pokrijejo content change dogodki).
            if (session.isActive()) {
                onScrolled(event);
            }
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
            updateSession(state.isReels());
            overlay.hide();
            return;
        }
        BlockAction action = BlockPolicy.decide(state, prefs.getMode(), prefs.isDmReelsAllowed());
        // Gledanje = Reels zaslon, ki ga ne blokiramo (premor zgoraj, izjema za DM tu).
        updateSession(state.isReels() && action == BlockAction.NONE);
        OverlayStep step = OverlayStep.decide(action == BlockAction.OVERLAY, overlay.isPresent(),
                overlayCooldown.isActive(SystemClock.uptimeMillis()));
        switch (step) {
            case KEEP:
                overlay.keepIfShowing();
                return;
            case SHOW:
                overlay.show(recordBlock());
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "overlay prikazan");
                }
                return;
            case HIDE:
                overlay.hide();
                break;
            case NONE:
            default:
                if (action == BlockAction.OVERLAY) {
                    return; // ohlajanje
                }
                break;
        }
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

    // --- Pogledani reeli in čas v Reelsih (samo dnevne vsote, Z4) ---

    private WatchSession.Sink sessionSink() {
        return new WatchSession.Sink() {
            @Override
            public void addViewed(int count) {
                prefs.addViewed(LocalDate.now().toEpochDay(), count);
            }

            @Override
            public void addSeconds(int seconds) {
                pendingSeconds += seconds;
                if (SystemClock.uptimeMillis() - lastPersistAt >= PERSIST_SECONDS_MS) {
                    persistSeconds();
                }
            }
        };
    }

    private void updateSession(boolean watching) {
        boolean wasActive = session.isActive();
        session.update(watching, SystemClock.uptimeMillis(), sessionSink());
        if (session.isActive() && !wasActive) {
            lastPersistAt = SystemClock.uptimeMillis();
            handler.postDelayed(sessionTick, SESSION_TICK_MS);
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "seja gledanja: začetek");
            }
        } else if (!session.isActive() && wasActive) {
            endSession();
        }
    }

    /** Vsako sekundo med sejo: čas + ali je Instagram še spredaj (SAMO ime paketa, kot D21). */
    private void onSessionTick() {
        if (!session.isActive()) {
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        CharSequence pkg = root != null ? root.getPackageName() : null;
        if (pkg != null && !DetectionRules.INSTAGRAM_PACKAGE.contentEquals(pkg)) {
            stopSession();
            return;
        }
        session.tick(SystemClock.uptimeMillis(), sessionSink());
        handler.postDelayed(sessionTick, SESSION_TICK_MS);
    }

    private void stopSession() {
        if (session.isActive()) {
            session.stop(SystemClock.uptimeMillis(), sessionSink());
            endSession();
        }
    }

    private void endSession() {
        handler.removeCallbacks(sessionTick);
        handler.removeCallbacks(swipeSettled);
        persistSeconds();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "seja gledanja: konec");
        }
    }

    private void persistSeconds() {
        if (pendingSeconds > 0) {
            prefs.addReelsSeconds(LocalDate.now().toEpochDay(), pendingSeconds);
            pendingSeconds = 0;
        }
        lastPersistAt = SystemClock.uptimeMillis();
    }

    /** Nov reel = sprememba indeksa strani na pagerju Reelsov (beremo samo ID vira in indeks). */
    private void onScrolled(AccessibilityEvent event) {
        AccessibilityNodeInfo source = event.getSource();
        if (source == null
                || !DetectionRules.REELS_PAGER_ID.equals(source.getViewIdResourceName())) {
            return;
        }
        int index = event.getToIndex();
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "pager drsenje: toIndex=" + index + " from=" + event.getFromIndex()
                    + " count=" + event.getItemCount());
        }
        if (index >= 0) {
            session.onPageIndex(index, sessionSink());
        } else {
            handler.removeCallbacks(swipeSettled);
            handler.postDelayed(swipeSettled, SWIPE_SETTLE_MS);
        }
    }

    /** @return število blokad danes, vključno s to */
    private int recordBlock() {
        long today = LocalDate.now().toEpochDay();
        prefs.recordBlock(today);
        return prefs.getStats().todayOn(today);
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
