package si.odisej.action;

/**
 * Kaj naredi servis z overlayem ob eni klasifikaciji. Čista funkcija: uporabljata jo servis in
 * test (OverlayCountTest), da test ne posnema servisa na pamet.
 */
public enum OverlayStep {
    NONE,
    /** Overlay je prikazan ali ravno izginja → obdrži (prekini fade-out); blokada se ne šteje. */
    KEEP,
    /** Nov prikaz → blokada se šteje. */
    SHOW,
    HIDE;

    /**
     * @param blockWithOverlay zaznan Reels zaslon, ki ga blokiramo z overlayem
     * @param overlayPresent   overlay je v oknu (prikazan ali izginja)
     * @param cooldownActive   uporabnik je pravkar sam zapustil overlay (katerikoli od treh gumbov)
     */
    public static OverlayStep decide(boolean blockWithOverlay, boolean overlayPresent,
                                     boolean cooldownActive) {
        if (!blockWithOverlay) {
            return overlayPresent ? HIDE : NONE;
        }
        // Ohlajanje ima prednost pred KEEP: po tapu je Reels med prehodom (nazaj, Home) še
        // viden; izginjajočega overlaya ne smemo oživiti — sicer je treba tapniti dvakrat.
        if (cooldownActive) {
            return NONE;
        }
        return overlayPresent ? KEEP : SHOW;
    }
}
