package si.odisej.action;

/**
 * Po ukrepu nekaj časa ignorira nove zaznave. Brez tega: nazaj → med animacijo je Reels še
 * v drevesu → spet nazaj → izhod iz Instagrama (CONTEXT.md §7.1). Čas podamo od zunaj,
 * da je razred testabilen.
 */
public final class Debouncer {

    private final long windowMs;
    private long lastAt;
    private boolean fired;

    public Debouncer(long windowMs) {
        this.windowMs = windowMs;
    }

    /** True in začne novo okno, če je od zadnjega uspešnega klica minilo vsaj windowMs. */
    public boolean tryAcquire(long nowMs) {
        if (fired && nowMs - lastAt < windowMs) {
            return false;
        }
        fired = true;
        lastAt = nowMs;
        return true;
    }
}
