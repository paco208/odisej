package si.odisej.ui;

/**
 * Krivulja enega vdiha in izdiha: 0 → 1 (vdih, 1,5 s) → 0 (izdih, 1,5 s).
 * (1 − cos 2πt) / 2 je gladka na obeh koncih in na vrhu, brez sunkov — kot dih.
 * Čista Java, da je testabilna.
 */
public final class BreathCurve {

    public static final long INHALE_MS = 1500;
    public static final long EXHALE_MS = 1500;
    public static final long TOTAL_MS = INHALE_MS + EXHALE_MS;

    private BreathCurve() {
    }

    /** Razširjenost kroga 0..1 za delež cikla 0..1 (izven intervala 0). */
    public static float expansion(float fraction) {
        if (fraction <= 0f || fraction >= 1f) {
            return 0f;
        }
        return (float) ((1 - Math.cos(2 * Math.PI * fraction)) / 2);
    }

    /** True, ko je vdih končan in se začne izdih (za menjavo napisa). */
    public static boolean isExhaling(float fraction) {
        return fraction >= (float) INHALE_MS / TOTAL_MS;
    }
}
