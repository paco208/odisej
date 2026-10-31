package si.odisej.action;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class OverlayStepTest {

    @Test
    public void notReelsHidesPresentOverlay() {
        assertEquals(OverlayStep.HIDE, OverlayStep.decide(false, true, false));
        assertEquals(OverlayStep.NONE, OverlayStep.decide(false, false, false));
    }

    @Test
    public void reelsShowsOrKeeps() {
        assertEquals(OverlayStep.SHOW, OverlayStep.decide(true, false, false));
        assertEquals(OverlayStep.KEEP, OverlayStep.decide(true, true, false));
    }

    @Test
    public void cooldownBeatsKeepAndShow() {
        assertEquals(OverlayStep.NONE, OverlayStep.decide(true, true, true));
        assertEquals(OverlayStep.NONE, OverlayStep.decide(true, false, true));
    }
}
