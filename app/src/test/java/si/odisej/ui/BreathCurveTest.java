package si.odisej.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BreathCurveTest {

    private static final float EPS = 1e-4f;

    @Test
    public void startsAndEndsAtRest() {
        assertEquals(0f, BreathCurve.expansion(0f), EPS);
        assertEquals(0f, BreathCurve.expansion(1f), EPS);
    }

    @Test
    public void peaksAtEndOfInhale() {
        assertEquals(1f, BreathCurve.expansion(0.5f), EPS);
    }

    @Test
    public void inhaleGrowsAndExhaleShrinks() {
        float prev = -1f;
        for (int i = 0; i <= 50; i++) {
            float e = BreathCurve.expansion(i / 100f);
            assertTrue("vdih mora rasti pri " + i, e >= prev);
            prev = e;
        }
        for (int i = 51; i <= 100; i++) {
            float e = BreathCurve.expansion(i / 100f);
            assertTrue("izdih se mora krčiti pri " + i, e <= prev);
            prev = e;
        }
    }

    @Test
    public void isSymmetric() {
        assertEquals(BreathCurve.expansion(0.2f), BreathCurve.expansion(0.8f), EPS);
    }

    @Test
    public void outsideCycleIsAtRest() {
        assertEquals(0f, BreathCurve.expansion(-0.5f), EPS);
        assertEquals(0f, BreathCurve.expansion(1.5f), EPS);
    }

    @Test
    public void labelSwitchesHalfway() {
        assertEquals(3000, BreathCurve.TOTAL_MS);
        assertFalse(BreathCurve.isExhaling(0.49f));
        assertTrue(BreathCurve.isExhaling(0.5f));
    }
}
