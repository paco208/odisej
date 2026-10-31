package si.odisej.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PauseCountdownTest {

    @Test
    public void notRunningUntilStarted() {
        PauseCountdown c = new PauseCountdown();
        assertFalse(c.isRunning());
        assertFalse(c.isFinished(1_000_000));
    }

    @Test
    public void countsDownSixtySecondsRoundingUp() {
        PauseCountdown c = new PauseCountdown();
        c.start(1000);
        assertEquals(60, c.remainingSeconds(1000));
        assertEquals(60, c.remainingSeconds(1500));
        assertEquals(59, c.remainingSeconds(2000));
        assertEquals(1, c.remainingSeconds(60_500));
        assertEquals(0, c.remainingSeconds(61_000));
    }

    @Test
    public void finishesOnlyAfterFullMinute() {
        PauseCountdown c = new PauseCountdown();
        c.start(0);
        assertFalse(c.isFinished(59_999));
        assertTrue(c.isFinished(60_000));
    }

    @Test
    public void cancelStopsCountdown() {
        PauseCountdown c = new PauseCountdown();
        c.start(0);
        c.cancel();
        assertFalse(c.isRunning());
        assertFalse(c.isFinished(120_000));
    }

    @Test
    public void restartBeginsFromFullMinute() {
        PauseCountdown c = new PauseCountdown();
        c.start(0);
        c.cancel();
        c.start(50_000);
        assertFalse(c.isFinished(100_000));
        assertEquals(10, c.remainingSeconds(100_000));
    }
}
