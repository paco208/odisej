package si.odisej.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WatchSessionTest {

    private int viewed;
    private int seconds;
    private final WatchSession.Sink sink = new WatchSession.Sink() {
        @Override
        public void addViewed(int count) {
            viewed += count;
        }

        @Override
        public void addSeconds(int s) {
            seconds += s;
        }
    };
    private final WatchSession session = new WatchSession();

    /** Kot servis: tik vsako sekundo od..do. */
    private void tickEverySecond(long from, long to) {
        for (long t = from; t <= to; t += 1000) {
            session.tick(t, sink);
        }
    }

    @Test
    public void enteringCountsFirstReel() {
        session.update(true, 0, sink);
        assertTrue(session.isActive());
        assertEquals(1, viewed);
        assertEquals(0, seconds);
    }

    @Test
    public void blockedOrOtherScreensDoNotStartSession() {
        session.update(false, 0, sink);
        assertFalse(session.isActive());
        assertEquals(0, viewed);
    }

    @Test
    public void countsSecondsWhileWatching() {
        session.update(true, 0, sink);
        tickEverySecond(1000, 90_000);
        session.update(false, 90_400, sink);
        assertEquals(90, seconds);
        assertFalse(session.isActive());
    }

    @Test
    public void subSecondRemaindersAreCarried() {
        session.update(true, 0, sink);
        session.tick(600, sink);
        session.tick(1200, sink);
        session.tick(1800, sink);
        session.stop(2100, sink);
        assertEquals(2, seconds);
    }

    @Test
    public void stopOnScreenOffOrLeavingInstagram() {
        session.update(true, 0, sink);
        tickEverySecond(1000, 10_000);
        session.stop(10_000, sink);
        session.tick(60_000, sink); // po izhodu se nič ne šteje
        assertEquals(10, seconds);
    }

    @Test
    public void longGapIsCappedNotCountedAsWatching() {
        session.update(true, 0, sink);
        session.tick(600_000, sink); // 10 min brez meritve (proces zamrznjen)
        assertEquals(5, seconds);
    }

    @Test
    public void pageIndexChangesCountNewReels() {
        session.update(true, 0, sink);       // 1. reel
        session.onPageIndex(4, sink);        // izhodišče (med potegom še stari indeks)
        session.onPageIndex(4, sink);
        session.onPageIndex(5, sink);        // 2. reel
        session.onPageIndex(5, sink);
        session.onPageIndex(6, sink);        // 3. reel
        session.onPageIndex(5, sink);        // nazaj na prejšnjega: tudi to je ogled
        assertEquals(4, viewed);
    }

    @Test
    public void pageIndexIgnoredOutsideSession() {
        session.onPageIndex(1, sink);
        session.onPageIndex(2, sink);
        assertEquals(0, viewed);
    }

    @Test
    public void newSessionResetsIndexBaseline() {
        session.update(true, 0, sink);
        session.onPageIndex(7, sink);
        session.update(false, 1000, sink);
        session.update(true, 2000, sink);    // nova seja: 1 reel
        session.onPageIndex(2, sink);        // izhodišče, ne nov reel
        assertEquals(2, viewed);
    }

    @Test
    public void settledSwipeIsFallbackOnlyWithoutIndex() {
        session.update(true, 0, sink);
        session.onSwipeSettled(sink);        // brez indeksa → ocena
        assertEquals(2, viewed);
        session.onPageIndex(3, sink);
        session.onSwipeSettled(sink);        // indeks obstaja → rezerva se ne uporablja
        assertEquals(2, viewed);
    }
}
