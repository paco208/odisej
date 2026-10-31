package si.odisej.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BlockStatsTest {

    private static final BlockStats EMPTY = new BlockStats(-1, 0, 0);

    @Test
    public void firstBlockCountsOnceForTodayAndTotal() {
        BlockStats s = EMPTY.record(100);
        assertEquals(1, s.todayOn(100));
        assertEquals(1, s.total);
    }

    @Test
    public void sameDayAccumulates() {
        BlockStats s = EMPTY.record(100).record(100).record(100);
        assertEquals(3, s.todayOn(100));
        assertEquals(3, s.total);
    }

    @Test
    public void newDayResetsTodayButKeepsTotal() {
        BlockStats s = EMPTY.record(100).record(100).record(101);
        assertEquals(1, s.todayOn(101));
        assertEquals(3, s.total);
    }

    @Test
    public void todayIsZeroWhenLastBlockWasEarlier() {
        BlockStats s = EMPTY.record(100);
        assertEquals(0, s.todayOn(102));
        assertEquals(1, s.total);
    }
}
