package si.odisej.ui;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DurationTextTest {

    private static String f(int s) {
        return DurationText.format(s, "<1 min", "%d min", "%d h %d min");
    }

    @Test
    public void underAMinute() {
        assertEquals("<1 min", f(0));
        assertEquals("<1 min", f(59));
    }

    @Test
    public void minutesAreFloored() {
        assertEquals("1 min", f(60));
        assertEquals("12 min", f(12 * 60 + 59));
    }

    @Test
    public void hoursAndMinutes() {
        assertEquals("1 h 0 min", f(3600));
        assertEquals("2 h 5 min", f(2 * 3600 + 5 * 60 + 30));
    }
}
