package si.odisej.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Vrednosti v dp (funkcija je neodvisna od enote). */
public class OverlayLayoutTest {

    private static final float EPS = 0.01f;

    @Test
    public void centersBlockSlightlyAboveMiddle() {
        // Jakobov zaslon: 964dp, trije gumbi (48dp), blok 367dp.
        float top = OverlayLayout.settledTop(964, 367, 44, 48, 48, 16);
        assertEquals(0.45f * 964 - 367 / 2f, top, EPS);
        assertTrue("središče nad sredino", top + 367 / 2f < 964 / 2f);
    }

    @Test
    public void bottomGapWinsOverCenteringOnSmallScreen() {
        // Majhno okno: centriranje (vrh 0,45·600 − 250 = 20) bi spodnji rob bloka spustilo
        // na 520, kar je manj kot 48dp nad navigacijo (meja 504) → blok gre višje.
        float centered = 0.45f * 600 - 500 / 2f;
        float top = OverlayLayout.settledTop(600, 500, 0, 48, 48, 0);
        assertTrue(centered + 500 > 600 - 48 - 48);
        assertEquals(600 - 48 - 48 - 500, top, EPS);
    }

    @Test
    public void neverAboveStatusBar() {
        float top = OverlayLayout.settledTop(400, 380, 24, 48, 48, 16);
        assertEquals(24 + 16, top, EPS);
    }

    @Test
    public void gestureNavLeavesBlockCentered() {
        float three = OverlayLayout.settledTop(964, 367, 44, 48, 48, 16);
        float gesture = OverlayLayout.settledTop(964, 367, 44, 24, 48, 16);
        assertEquals(three, gesture, EPS); // dovolj prostora: inset ne premakne bloka
    }
}
