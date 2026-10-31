package si.odisej.action;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import si.odisej.settings.BlockStats;

/**
 * Število na overlayu ("tvoj N. reel danes") mora biti enako številu na glavnem zaslonu po tej
 * blokadi. Na telefonu je overlay pokazal 29, glavni zaslon 30: po tapu na gumb je bil viewer
 * med animacijo prehoda še viden, overlay se je pokazal (in štel) še enkrat. Test posnema
 * odločanje servisa (ReelsAccessibilityService.classifyNow) za način overlay.
 */
public class OverlayCountTest {

    private static final long DAY = 20_000;

    private BlockStats stats = new BlockStats(-1, 0, 0);
    private final Cooldown cooldown = new Cooldown(1500);
    private boolean showing;
    private int shownN = -1;

    /** Ena klasifikacija: reels = zaznan Reels zaslon. */
    private void classify(boolean reels, long now) {
        if (!reels) {
            showing = false;
            return;
        }
        if (showing || cooldown.isActive(now)) {
            return;
        }
        stats = stats.record(DAY);
        shownN = stats.todayOn(DAY);
        showing = true;
    }

    private void userTapsButton(long now) {
        cooldown.start(now);
        showing = false;
    }

    private int mainScreenCount() {
        return stats.todayOn(DAY);
    }

    @Test
    public void overlayNumberIncludesThisBlock() {
        stats = new BlockStats(DAY, 28, 100);
        classify(true, 0);
        assertEquals(29, shownN);
        assertEquals(mainScreenCount(), shownN);
    }

    @Test
    public void leavingDuringBackTransitionDoesNotCountTwice() {
        stats = new BlockStats(DAY, 28, 100);
        classify(true, 0);          // overlay: 29
        classify(true, 300);        // še vedno Reels → isti overlay
        userTapsButton(3500);       // "Na feed" → skrij + BACK
        classify(true, 3800);       // med animacijo je viewer še v drevesu
        classify(false, 4100);      // feed
        assertEquals(29, shownN);
        assertEquals(29, mainScreenCount());
    }

    @Test
    public void realReturnAfterCooldownIsCounted() {
        stats = new BlockStats(DAY, 28, 100);
        classify(true, 0);
        userTapsButton(3500);
        classify(false, 4000);
        classify(true, 6000);       // uporabnik se res vrne v Reelse
        assertEquals(30, shownN);
        assertEquals(mainScreenCount(), shownN);
    }
}
