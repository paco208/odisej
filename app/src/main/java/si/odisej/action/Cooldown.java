package si.odisej.action;

/**
 * Po tem, ko uporabnik sam zapusti overlay (gumb), ga nekaj časa ne pokažemo znova. Med animacijo
 * prehoda (nazaj iz viewerja) je Reels še ~300 ms viden v drevesu; brez tega bi se overlay
 * za hip pokazal še enkrat in blokada bi se štela dvakrat (overlay 29, glavni zaslon 30).
 */
public final class Cooldown {

    private final long windowMs;
    private long until = Long.MIN_VALUE;

    public Cooldown(long windowMs) {
        this.windowMs = windowMs;
    }

    public void start(long nowMs) {
        until = nowMs + windowMs;
    }

    public boolean isActive(long nowMs) {
        return nowMs < until;
    }
}
