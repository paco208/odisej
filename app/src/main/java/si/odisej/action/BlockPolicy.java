package si.odisej.action;

import si.odisej.detect.ScreenState;
import si.odisej.settings.Prefs;

/** Čista odločitev: stanje zaslona + nastavitve → ukrep. */
public final class BlockPolicy {

    private BlockPolicy() {
    }

    public static BlockAction decide(ScreenState state, Prefs.Mode mode, boolean allowDmReels) {
        if (!state.isReels()) {
            return BlockAction.NONE;
        }
        // §7.3: reel, ki ti ga je nekdo poslal, si lahko ogledaš; poteg na naslednjega
        // (predlaganega) da REELS_VIEWER in se blokira.
        if (state == ScreenState.REELS_FROM_DM && allowDmReels) {
            return BlockAction.NONE;
        }
        if (mode == Prefs.Mode.OVERLAY) {
            return BlockAction.OVERLAY;
        }
        return state == ScreenState.REELS_TAB ? BlockAction.GO_HOME : BlockAction.BACK;
    }
}
