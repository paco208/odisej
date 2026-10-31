package si.odisej.settings;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ProtectionStatusTest {

    @Test
    public void enabledAndNotPausedIsActive() {
        assertEquals(ProtectionStatus.ACTIVE, ProtectionStatus.of(true, false));
    }

    @Test
    public void enabledAndPausedIsPaused() {
        assertEquals(ProtectionStatus.PAUSED, ProtectionStatus.of(true, true));
    }

    @Test
    public void disabledServiceIsOffEvenWhenPaused() {
        assertEquals(ProtectionStatus.OFF, ProtectionStatus.of(false, false));
        assertEquals(ProtectionStatus.OFF, ProtectionStatus.of(false, true));
    }
}
