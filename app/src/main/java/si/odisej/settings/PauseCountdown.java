package si.odisej.settings;

/**
 * Strogi način: premor blokiranja zahteva 60 s odštevanja na zaslonu aplikacije. Čas podamo
 * od zunaj (monotona ura), da je testabilno.
 */
public final class PauseCountdown {

    public static final long COUNTDOWN_MS = 60_000;
    public static final long PAUSE_MS = 15 * 60_000;

    private long startedAt;
    private boolean running;

    public void start(long nowMs) {
        startedAt = nowMs;
        running = true;
    }

    public void cancel() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    /** Preostale sekunde, zaokrožene navzgor (59.2 s → 60). */
    public int remainingSeconds(long nowMs) {
        long left = Math.max(0, startedAt + COUNTDOWN_MS - nowMs);
        return (int) ((left + 999) / 1000);
    }

    public boolean isFinished(long nowMs) {
        return running && nowMs - startedAt >= COUNTDOWN_MS;
    }
}
