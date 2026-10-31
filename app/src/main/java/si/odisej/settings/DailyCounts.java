package si.odisej.settings;

import java.util.Map;
import java.util.TreeMap;

/**
 * Število blokad po dnevih, SAMO za zadnjih 7 dni (Z4). Starejši dnevi se ob vsakem zapisu
 * pobrišejo. Dan = LocalDate.toEpochDay(); ura dneva se ne hrani. Nespremenljiv, čista Java.
 */
public final class DailyCounts {

    public static final int DAYS = 7;

    private final TreeMap<Long, Integer> counts;

    private DailyCounts(TreeMap<Long, Integer> counts) {
        this.counts = counts;
    }

    public static DailyCounts empty() {
        return new DailyCounts(new TreeMap<>());
    }

    public static DailyCounts of(long day, int count) {
        return empty().with(day, count);
    }

    private DailyCounts with(long day, int count) {
        TreeMap<Long, Integer> copy = new TreeMap<>(counts);
        if (count > 0) {
            copy.put(day, count);
        } else {
            copy.remove(day);
        }
        return new DailyCounts(copy);
    }

    /** Ena blokada več na dan {@code today}; hkrati pobriše dneve izven zadnjih 7. */
    public DailyCounts record(long today) {
        return with(today, countOn(today) + 1).pruned(today);
    }

    /**
     * Pobriše dneve, starejše od [today − 6]. "Prihodnjih" dni (ura premaknjena nazaj, npr.
     * potovanje čez časovni pas) ne briše, da se štetje ne izgubi; izginejo, ko se postarajo.
     */
    public DailyCounts pruned(long today) {
        return new DailyCounts(new TreeMap<>(counts.tailMap(today - (DAYS - 1), true)));
    }

    public int countOn(long day) {
        Integer c = counts.get(day);
        return c != null ? c : 0;
    }

    /** Zadnjih 7 dni, od najstarejšega do danes. */
    public int[] lastDays(long today) {
        int[] out = new int[DAYS];
        for (int i = 0; i < DAYS; i++) {
            out[i] = countOn(today - (DAYS - 1) + i);
        }
        return out;
    }

    /** Oblika na disku: "20391:3,20392:5". */
    public String serialize() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Long, Integer> e : counts.entrySet()) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(e.getKey()).append(':').append(e.getValue());
        }
        return sb.toString();
    }

    /** Strpno branje: pokvarjene vnose preskoči. */
    public static DailyCounts parse(String text) {
        TreeMap<Long, Integer> map = new TreeMap<>();
        if (text != null && !text.isEmpty()) {
            for (String part : text.split(",")) {
                String[] kv = part.split(":");
                if (kv.length != 2) {
                    continue;
                }
                try {
                    int count = Integer.parseInt(kv[1].trim());
                    if (count > 0) {
                        map.put(Long.parseLong(kv[0].trim()), count);
                    }
                } catch (NumberFormatException ignored) {
                    // preskoči pokvarjen vnos
                }
            }
        }
        return new DailyCounts(map);
    }
}
