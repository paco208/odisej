package si.odisej.action;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import si.odisej.detect.ScreenState;

public class BlockPolicyTest {

    @Test
    public void reelsTabGoesHome() {
        assertEquals(BlockAction.GO_HOME, BlockPolicy.decide(ScreenState.REELS_TAB));
    }

    @Test
    public void viewerGoesBack() {
        assertEquals(BlockAction.BACK, BlockPolicy.decide(ScreenState.REELS_VIEWER));
    }

    @Test
    public void reelFromDmIsBlockedInV1() {
        assertEquals(BlockAction.BACK, BlockPolicy.decide(ScreenState.REELS_FROM_DM));
    }

    @Test
    public void otherScreensAreLeftAlone() {
        assertEquals(BlockAction.NONE, BlockPolicy.decide(ScreenState.OTHER));
    }
}
