package si.odisej.settings;

/**
 * Seja gledanja neblokiranih Reelsov (premor, izjema za DM): šteje pogledane reele in čas.
 * Ven gredo samo prirastki za dnevne vsote — nobenih časovnih žigov (Z4). Čas podamo od zunaj
 * (monotona ura, ki med globokim spanjem miruje), da je testabilno.
 */
public final class WatchSession {

    /** Kam gredo prirastki (servis jih prišteje dnevnim vsotam). */
    public interface Sink {
        void addViewed(int count);

        void addSeconds(int seconds);
    }

    /**
     * Največji korak med dvema meritvama. Servis meri vsako sekundo; daljša vrzel pomeni, da se
     * je nekaj zataknilo (proces zamrznjen), in je ne štejemo kot gledanje.
     */
    static final long MAX_STEP_MS = 5000;

    private boolean active;
    private long lastAt;
    private long carryMs;
    private int pageIndex = -1;

    public boolean isActive() {
        return active;
    }

    /** Rezultat klasifikacije: watching = Reels zaslon, ki ga NE blokiramo. */
    public void update(boolean watching, long nowMs, Sink sink) {
        if (!watching) {
            stop(nowMs, sink);
            return;
        }
        if (!active) {
            active = true;
            lastAt = nowMs;
            carryMs = 0;
            pageIndex = -1;
            sink.addViewed(1); // reel, na katerega si prišel
        } else {
            flush(nowMs, sink);
        }
    }

    /** Vsako sekundo, dokler je seja aktivna. */
    public void tick(long nowMs, Sink sink) {
        if (active) {
            flush(nowMs, sink);
        }
    }

    /** Izhod iz Reelsov, ugasnjen zaslon, odhod iz Instagrama. */
    public void stop(long nowMs, Sink sink) {
        if (active) {
            flush(nowMs, sink);
            active = false;
        }
    }

    /**
     * Indeks strani iz TYPE_VIEW_SCROLLED na pagerju (ViewPager ga spremeni enkrat na poteg).
     * Prvi indeks seje je izhodišče (ta reel je že štet ob začetku).
     */
    public void onPageIndex(int index, Sink sink) {
        if (!active || index < 0) {
            return;
        }
        if (pageIndex < 0) {
            pageIndex = index;
        } else if (index != pageIndex) {
            pageIndex = index;
            sink.addViewed(1);
        }
    }

    /** Rezerva, če pager indeksa ne pošlje: poteg se je umiril → en reel več (ocena). */
    public void onSwipeSettled(Sink sink) {
        if (active && pageIndex < 0) {
            sink.addViewed(1);
        }
    }

    private void flush(long nowMs, Sink sink) {
        long step = Math.max(0, Math.min(nowMs - lastAt, MAX_STEP_MS));
        lastAt = nowMs;
        long ms = carryMs + step;
        int seconds = (int) (ms / 1000);
        carryMs = ms % 1000;
        if (seconds > 0) {
            sink.addSeconds(seconds);
        }
    }
}
