package si.odisej.settings;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Edino, kar Odisej shrani na disk (Z4): nastavitve. Nič o zaslonu, nobene zgodovine.
 */
public final class Prefs {

    public enum Mode { BACK, OVERLAY }

    private static final String FILE = "odisej";
    private static final String KEY_MODE = "mode";
    private static final String KEY_ALLOW_DM_REELS = "allow_dm_reels";

    private final SharedPreferences sp;

    public Prefs(Context context) {
        sp = context.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    public Mode getMode() {
        return Mode.BACK.name().equals(sp.getString(KEY_MODE, null)) ? Mode.BACK : Mode.OVERLAY;
    }

    public void setMode(Mode mode) {
        sp.edit().putString(KEY_MODE, mode.name()).apply();
    }

    public boolean isDmReelsAllowed() {
        return sp.getBoolean(KEY_ALLOW_DM_REELS, true);
    }

    public void setDmReelsAllowed(boolean allowed) {
        sp.edit().putBoolean(KEY_ALLOW_DM_REELS, allowed).apply();
    }
}
