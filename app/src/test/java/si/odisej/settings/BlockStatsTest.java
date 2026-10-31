package si.odisej.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class BlockStatsTest {

    @Test
    public void firstBlockCountsOnceForTodayAndTotal() {
        BlockStats s = BlockStats.empty().record(100);
        assertEquals(1, s.todayOn(100));
        assertEquals(1, s.total);
    }

    @Test
    public void sameDayAccumulates() {
        BlockStats s = BlockStats.empty().record(100).record(100).record(100);
        assertEquals(3, s.todayOn(100));
        assertEquals(3, s.total);
    }

    @Test
    public void newDayResetsTodayButKeepsTotal() {
        BlockStats s = BlockStats.empty().record(100).record(100).record(101);
        assertEquals(1, s.todayOn(101));
        assertEquals(3, s.total);
    }

    @Test
    public void totalSurvivesPruningOfOldDays() {
        BlockStats s = BlockStats.empty().record(100).record(100).record(120);
        assertEquals(0, s.todayOn(100));
        assertEquals(3, s.total);
    }
}
