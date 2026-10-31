package si.odisej.action;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CooldownTest {

    @Test
    public void inactiveBeforeStart() {
        assertFalse(new Cooldown(1500).isActive(0));
    }

    @Test
    public void activeForWindowAfterStart() {
        Cooldown c = new Cooldown(1500);
        c.start(1000);
        assertTrue(c.isActive(1000));
        assertTrue(c.isActive(2499));
        assertFalse(c.isActive(2500));
    }
}
