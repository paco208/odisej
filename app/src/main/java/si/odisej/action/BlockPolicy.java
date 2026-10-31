package si.odisej.action;

import si.odisej.detect.ScreenState;

/** Čista odločitev: stanje zaslona → ukrep. */
public final class BlockPolicy {

    private BlockPolicy() {
    }

    public static BlockAction decide(ScreenState state) {
        switch (state) {
            case REELS_TAB:
                return BlockAction.GO_HOME;
            case REELS_VIEWER:
            case REELS_FROM_DM:
                return BlockAction.BACK;
            case OTHER:
            default:
                return BlockAction.NONE;
        }
    }
}
