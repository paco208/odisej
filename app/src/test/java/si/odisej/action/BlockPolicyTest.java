package si.odisej.action;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import si.odisej.detect.ScreenState;
import si.odisej.settings.Prefs.Mode;

public class BlockPolicyTest {

    @Test
    public void otherScreensAreLeftAlone() {
        assertEquals(BlockAction.NONE, BlockPolicy.decide(ScreenState.OTHER, Mode.BACK, true));
        assertEquals(BlockAction.NONE, BlockPolicy.decide(ScreenState.OTHER, Mode.OVERLAY, false));
    }

    @Test
    public void backModeReelsTabGoesHome() {
        assertEquals(BlockAction.GO_HOME,
                BlockPolicy.decide(ScreenState.REELS_TAB, Mode.BACK, true));
    }

    @Test
    public void backModeViewerGoesBack() {
        assertEquals(BlockAction.BACK,
                BlockPolicy.decide(ScreenState.REELS_VIEWER, Mode.BACK, true));
    }

    @Test
    public void overlayModeShowsOverlayForTabAndViewer() {
        assertEquals(BlockAction.OVERLAY,
                BlockPolicy.decide(ScreenState.REELS_TAB, Mode.OVERLAY, true));
        assertEquals(BlockAction.OVERLAY,
                BlockPolicy.decide(ScreenState.REELS_VIEWER, Mode.OVERLAY, true));
    }

    @Test
    public void reelFromDmIsAllowedWhenExceptionOn() {
        assertEquals(BlockAction.NONE,
                BlockPolicy.decide(ScreenState.REELS_FROM_DM, Mode.OVERLAY, true));
        assertEquals(BlockAction.NONE,
                BlockPolicy.decide(ScreenState.REELS_FROM_DM, Mode.BACK, true));
    }

    @Test
    public void reelFromDmIsBlockedWhenExceptionOff() {
        assertEquals(BlockAction.OVERLAY,
                BlockPolicy.decide(ScreenState.REELS_FROM_DM, Mode.OVERLAY, false));
        assertEquals(BlockAction.BACK,
                BlockPolicy.decide(ScreenState.REELS_FROM_DM, Mode.BACK, false));
    }
}
