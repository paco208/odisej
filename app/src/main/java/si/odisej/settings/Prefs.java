package si.odisej.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Edino, kar Odisej shrani na disk (Z4): nastavitve, konec premora, število blokad na dan za
 * zadnjih 7 dni in skupaj. Nič o zaslonu, nobenih ur. SharedPreferences namesto DataStore (D24).
 */
public final class Prefs {

    public enum Mode { BACK, OVERLAY }

    private static final String FILE = "odisej";
    private static final String KEY_MODE = "mode";
    private static final String KEY_ALLOW_DM_REELS = "allow_dm_reels";
    private static final String KEY_PAUSED_UNTIL = "paused_until";
    private static final String KEY_DAILY = "daily_counts";
    private static final String KEY_STATS_TOTAL = "stats_total";
    /** Stara oblika (do 7-dnevnih stolpcev): samo dan zadnje blokade in število tega dne. */
    private static final String LEGACY_STATS_DAY = "stats_day";
    private static final String LEGACY_STATS_TODAY = "stats_today";
    /** Stabilne vrednosti na disku (ne Enum.name(), ki bi ga lahko spremenilo preimenovanje). */
    private static final String MODE_BACK = "back";
    private static final String MODE_OVERLAY = "overlay";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public Mode getMode() {
        return MODE_BACK.equals(sp.getString(KEY_MODE, MODE_OVERLAY)) ? Mode.BACK : Mode.OVERLAY;
    }

    public void setMode(Mode mode) {
        sp.edit().putString(KEY_MODE, mode == Mode.BACK ? MODE_BACK : MODE_OVERLAY).apply();
    }

    public boolean isDmReelsAllowed() {
        return sp.getBoolean(KEY_ALLOW_DM_REELS, true);
    }

    public void setDmReelsAllowed(boolean allowed) {
        sp.edit().putBoolean(KEY_ALLOW_DM_REELS, allowed).apply();
    }

    /** Stenska ura (System.currentTimeMillis), ker mora preživeti ponovni zagon procesa. */
    public long getPausedUntil() {
        return sp.getLong(KEY_PAUSED_UNTIL, 0);
    }

    public boolean isPaused(long nowWallMs) {
        return nowWallMs < getPausedUntil();
    }

    public void setPausedUntil(long wallMs) {
        sp.edit().putLong(KEY_PAUSED_UNTIL, wallMs).apply();
    }

    public BlockStats getStats() {
        DailyCounts days;
        if (sp.contains(KEY_DAILY)) {
            days = DailyCounts.parse(sp.getString(KEY_DAILY, ""));
        } else {
            // Prenos iz stare oblike: dan zadnje blokade + število tega dne.
            days = DailyCounts.of(sp.getLong(LEGACY_STATS_DAY, -1),
                    sp.getInt(LEGACY_STATS_TODAY, 0));
        }
        return new BlockStats(days, sp.getInt(KEY_STATS_TOTAL, 0));
    }

    public void recordBlock(long epochDay) {
        BlockStats next = getStats().record(epochDay);
        sp.edit()
                .putString(KEY_DAILY, next.days.serialize())
                .putInt(KEY_STATS_TOTAL, next.total)
                .remove(LEGACY_STATS_DAY)
                .remove(LEGACY_STATS_TODAY)
                .apply();
    }
}
