package si.odisej.settings;

/** Stanje zaščite za glavni zaslon in ploščico v hitrih nastavitvah. */
public enum ProtectionStatus {
    ACTIVE,
    PAUSED,
    OFF;

    /** Izklopljen servis ima prednost pred premorom: takrat ne blokira nič. */
    public static ProtectionStatus of(boolean serviceEnabled, boolean paused) {
        if (!serviceEnabled) {
            return OFF;
        }
        return paused ? PAUSED : ACTIVE;
    }
}
