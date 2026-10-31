package si.odisej.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Edino, kar Odisej shrani na disk (Z4): nastavitve, konec premora, dnevne vsote za zadnjih
 * 7 dni (blokade, pogledani reeli, sekunde v Reelsih) in skupno število blokad. Nič o zaslonu,
 * nobenih ur ali časovnih žigov. SharedPreferences namesto DataStore (D24).
 */
public final class Prefs {

    public enum Mode { BACK, OVERLAY }

    private static final String FILE = "odisej";
    private static final String KEY_MODE = "mode";
    private static final String KEY_ALLOW_DM_REELS = "allow_dm_reels";
    private static final String KEY_PAUSED_UNTIL = "paused_until";
    private static final String KEY_DAILY = "daily_counts";
    private static final String KEY_STATS_TOTAL = "stats_total";
    private static final String KEY_DAILY_VIEWED = "daily_viewed";
    private static final String KEY_DAILY_SECONDS = "daily_seconds";
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

    /** Ključi, ob katerih spremembi naj se glavni zaslon osveži. */
    public static boolean isStatsKey(String key) {
        return KEY_DAILY.equals(key) || KEY_STATS_TOTAL.equals(key)
                || KEY_DAILY_VIEWED.equals(key) || KEY_DAILY_SECONDS.equals(key);
    }

    public void registerListener(SharedPreferences.OnSharedPreferenceChangeListener l) {
        sp.registerOnSharedPreferenceChangeListener(l);
    }

    public void unregisterListener(SharedPreferences.OnSharedPreferenceChangeListener l) {
        sp.unregisterOnSharedPreferenceChangeListener(l);
    }

    public DailyCounts getViewed() {
        return DailyCounts.parse(sp.getString(KEY_DAILY_VIEWED, ""));
    }

    public DailyCounts getReelsSeconds() {
        return DailyCounts.parse(sp.getString(KEY_DAILY_SECONDS, ""));
    }

    /** Pogledani (neblokirani) reeli: samo dnevna vsota. */
    public void addViewed(long epochDay, int count) {
        sp.edit().putString(KEY_DAILY_VIEWED, getViewed().add(epochDay, count).serialize()).apply();
    }

    /** Sekunde v neblokiranih Reelsih: samo dnevna vsota. */
    public void addReelsSeconds(long epochDay, int seconds) {
        sp.edit().putString(KEY_DAILY_SECONDS,
                getReelsSeconds().add(epochDay, seconds).serialize()).apply();
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
