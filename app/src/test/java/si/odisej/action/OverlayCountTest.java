package si.odisej.action;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import si.odisej.settings.BlockStats;
import si.odisej.settings.DailyCounts;

/**
 * Število na overlayu ("tvoj N. reel danes") mora biti enako številu na glavnem zaslonu, in
 * vsak izhod (Na sporočila, Na feed, Zapri Instagram) mora delovati na prvi tap. Po tapu je
 * Reels med animacijo prehoda še viden v drevesu; brez ohlajanja se je overlay vrnil (dvojni tap)
 * ali pa je bil štet dvakrat (overlay 29, glavni zaslon 30).
 * Odločitve dela {@link OverlayStep#decide}, isto kot v servisu.
 */
public class OverlayCountTest {

    private static final long DAY = 20_000;
    private static final long FADE_OUT_MS = 150;

    private BlockStats stats = new BlockStats(DailyCounts.of(DAY, 28), 100);
    private final Cooldown cooldown = new Cooldown(1500);
    /** Overlay je v oknu; fadingUntil > now pomeni, da ravno izginja. */
    private boolean present;
    private long fadingUntil = Long.MIN_VALUE;
    private int shownN = -1;
    private int shows;

    private void tickFade(long now) {
        if (present && fadingUntil != Long.MIN_VALUE && now >= fadingUntil) {
            present = false;
            fadingUntil = Long.MIN_VALUE;
        }
    }

    /** Ena klasifikacija; reels = Reels zaslon, ki ga blokiramo z overlayem. */
    private void classify(boolean reels, long now) {
        tickFade(now);
        switch (OverlayStep.decide(reels, present, cooldown.isActive(now))) {
            case SHOW:
                stats = stats.record(DAY);
                shownN = stats.todayOn(DAY);
                present = true;
                shows++;
                break;
            case KEEP:
                fadingUntil = Long.MIN_VALUE; // prekine fade-out
                break;
            case HIDE:
                fadingUntil = now + FADE_OUT_MS;
                break;
            default:
                break;
        }
    }

    /** Katerikoli od treh gumbov: ohlajanje + fade-out (+ nazaj / Home / klik zavihka). */
    private void userLeaves(long now) {
        cooldown.start(now);
        fadingUntil = now + FADE_OUT_MS;
    }

    private boolean visibleAt(long now) {
        tickFade(now);
        return present;
    }

    @Test
    public void overlayNumberIncludesThisBlock() {
        classify(true, 0);
        assertEquals(29, shownN);
        assertEquals(29, stats.todayOn(DAY));
    }

    @Test
    public void closeInstagramWorksOnFirstTap() {
        classify(true, 0);
        userLeaves(3500);          // "Zapri Instagram" → GLOBAL_ACTION_HOME
        classify(true, 3550);      // med prehodom na zaganjalnik je viewer še viden
        classify(true, 3620);
        assertFalse("overlay se ne sme vrniti", visibleAt(3700));
        assertEquals(1, shows);
        assertEquals(29, stats.todayOn(DAY));
    }

    @Test
    public void backToFeedDuringTransitionDoesNotCountTwice() {
        classify(true, 0);
        classify(true, 300);
        userLeaves(3500);          // "Na feed" → skrij + BACK
        classify(true, 3800);      // viewer še v drevesu
        classify(false, 4100);     // feed
        assertFalse(visibleAt(4200));
        assertEquals(29, shownN);
        assertEquals(29, stats.todayOn(DAY));
    }

    @Test
    public void realReturnAfterCooldownIsCounted() {
        classify(true, 0);
        userLeaves(3500);
        classify(false, 4000);
        classify(true, 6000);      // uporabnik se res vrne v Reelse
        assertEquals(30, shownN);
        assertEquals(30, stats.todayOn(DAY));
    }

    @Test
    public void transientOtherDoesNotRestartOverlay() {
        classify(true, 0);
        classify(false, 1000);     // drevo za hip ne kaže Reelsov → začne fade-out
        classify(true, 1100);      // še med fade-outom → obdrži
        assertEquals(1, shows);
        assertEquals(29, stats.todayOn(DAY));
    }
}
