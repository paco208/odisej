package si.odisej.settings;

/**
 * Števec blokad (danes / skupaj). Hranimo le dan ZADNJE blokade (za ponastavitev "danes"),
 * ki se prepiše — nobene zgodovine (Z4). Čista Java zaradi testov.
 */
public final class BlockStats {

    public final long day;
    public final int today;
    public final int total;

    public BlockStats(long day, int today, int total) {
        this.day = day;
        this.today = today;
        this.total = total;
    }

    public BlockStats record(long currentDay) {
        return new BlockStats(currentDay, todayOn(currentDay) + 1, total + 1);
    }

    public int todayOn(long currentDay) {
        return day == currentDay ? today : 0;
    }
}
