package si.odisej.detect;

public enum ScreenState {
    /** Vse, kar ni Reels: feed, storyji, DM-ji, profil, Explore, objavljanje. */
    OTHER,
    /** Zavihek Reels v spodnji vrstici. */
    REELS_TAB,
    /** Reels viewer čez cel zaslon (iz feeda, Explore ali naslednji reel v DM viewerju). */
    REELS_VIEWER,
    /** Reel, ki ga je nekdo poslal v DM (prvi reel v tem viewerju, z vrstico za odgovor). */
    REELS_FROM_DM;

    public boolean isReels() {
        return this != OTHER;
    }
}
