package si.odisej.settings;

/**
 * Števec blokad: po dnevih za zadnjih 7 dni + skupaj. Nobene druge zgodovine (Z4).
 * Čista Java zaradi testov.
 */
public final class BlockStats {

    public final DailyCounts days;
    public final int total;

    public BlockStats(DailyCounts days, int total) {
        this.days = days;
        this.total = total;
    }

    public static BlockStats empty() {
        return new BlockStats(DailyCounts.empty(), 0);
    }

    public BlockStats record(long currentDay) {
        return new BlockStats(days.record(currentDay), total + 1);
    }

    public int todayOn(long currentDay) {
        return days.countOn(currentDay);
    }

    public int[] lastSevenDays(long currentDay) {
        return days.lastDays(currentDay);
    }
}
