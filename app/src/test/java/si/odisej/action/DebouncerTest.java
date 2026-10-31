package si.odisej.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class DebouncerTest {

    @Test
    public void firstCallPasses() {
        assertTrue(new Debouncer(1500).tryAcquire(0));
    }

    @Test
    public void callsInsideWindowAreBlocked() {
        Debouncer d = new Debouncer(1500);
        assertTrue(d.tryAcquire(1000));
        assertFalse(d.tryAcquire(1300));
        assertFalse(d.tryAcquire(2499));
    }

    @Test
    public void callAfterWindowPassesAndStartsNewWindow() {
        Debouncer d = new Debouncer(1500);
        assertTrue(d.tryAcquire(1000));
        assertTrue(d.tryAcquire(2500));
        assertFalse(d.tryAcquire(3000));
    }

    @Test
    public void blockedCallsDoNotExtendWindow() {
        Debouncer d = new Debouncer(1500);
        assertTrue(d.tryAcquire(0));
        assertFalse(d.tryAcquire(1400));
        assertTrue(d.tryAcquire(1500));
    }
}
