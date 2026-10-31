package si.odisej.settings;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class DailyCountsTest {

    private static final long D = 20_000;

    @Test
    public void recordsPerDay() {
        DailyCounts c = DailyCounts.empty().record(D).record(D).record(D + 1);
        assertEquals(2, c.countOn(D));
        assertEquals(1, c.countOn(D + 1));
    }

    @Test
    public void keepsOnlyLastSevenDays() {
        DailyCounts c = DailyCounts.empty();
        for (long d = D; d < D + 10; d++) {
            c = c.record(d);
        }
        // zadnji dan D+9 → hranimo D+3 .. D+9
        assertEquals(0, c.countOn(D + 2));
        assertEquals(1, c.countOn(D + 3));
        assertEquals("" + (D + 3) + ":1," + (D + 4) + ":1," + (D + 5) + ":1," + (D + 6) + ":1,"
                + (D + 7) + ":1," + (D + 8) + ":1," + (D + 9) + ":1", c.serialize());
    }

    @Test
    public void clockMovedBackKeepsLaterDayUntilItAges() {
        DailyCounts c = DailyCounts.of(D + 1, 3).record(D);
        assertEquals(3, c.countOn(D + 1));
        assertEquals(1, c.countOn(D));
        assertEquals(0, c.record(D + 8).countOn(D + 1));
    }

    @Test
    public void lastDaysIsOldestFirstEndingToday() {
        DailyCounts c = DailyCounts.of(D - 6, 4).record(D).record(D).record(D - 1);
        assertArrayEquals(new int[]{4, 0, 0, 0, 0, 1, 2}, c.lastDays(D));
    }

    @Test
    public void serializeParseRoundTrip() {
        DailyCounts c = DailyCounts.empty().record(D).record(D - 2).record(D);
        assertEquals(c.serialize(), DailyCounts.parse(c.serialize()).serialize());
        assertArrayEquals(c.lastDays(D), DailyCounts.parse(c.serialize()).lastDays(D));
    }

    @Test
    public void parseIsTolerant() {
        DailyCounts c = DailyCounts.parse("x,20000:3,bad:1,20001:-2,20002:,20003:5");
        assertEquals(3, c.countOn(20000));
        assertEquals(0, c.countOn(20001));
        assertEquals(5, c.countOn(20003));
        assertEquals("", DailyCounts.parse(null).serialize());
        assertEquals("", DailyCounts.parse("").serialize());
    }

    @Test
    public void legacyMigrationKeepsTodaysCount() {
        assertEquals(30, DailyCounts.of(D, 30).countOn(D));
        assertEquals("", DailyCounts.of(-1, 0).serialize());
    }
}
