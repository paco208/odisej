package si.odisej.action;

/** Kaj naredimo ob zaznanem zaslonu. */
public enum BlockAction {
    NONE,
    /** Klik na zavihek Home (z zavihka Reels; neodvisno od sklada "nazaj"). */
    GO_HOME,
    /** GLOBAL_ACTION_BACK (iz viewerja čez cel zaslon). */
    BACK,
    /** Prekrivno okno čez cel zaslon (v2). */
    OVERLAY
}
