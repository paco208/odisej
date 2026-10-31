package si.odisej.detect;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * VSA pravila zaznave na enem mestu (CLAUDE.md pravilo 6). Ko Instagram ob posodobitvi
 * zamenja ID-je, se popravi samo ta datoteka in testna drevesa.
 *
 * Izmerjeno: Instagram 453.0.0.57.79, Android 16, 2026-10-31 (docs/zaznava.md, CONTEXT.md D15).
 * Vsa pravila veljajo samo za VIDNA vozlišča: Instagram drži skrit fragment Reels zavihka
 * v drevesu skoraj vseh zaslonov.
 */
public final class DetectionRules {

    public static final String INSTAGRAM_PACKAGE = "com.instagram.android";

    private static final String ID = INSTAGRAM_PACKAGE + ":id/";

    /** Pager Reels viewerja: njegov TYPE_VIEW_SCROLLED (indeks strani) šteje pogledane reele. */
    public static final String REELS_PAGER_ID = ID + "clips_viewer_view_pager";

    /** Viden katerikoli od teh = Reels viewer ali zavihek. Prvi je glavni, drugi rezervni. */
    public static final Set<String> REELS_IDS = setOf(
            REELS_PAGER_ID,
            ID + "clips_expanded_touch_view");

    /** Zavihek Reels; če je izbran (selected), smo na zavihku Reels. */
    public static final String REELS_TAB_ID = ID + "clips_tab";

    /** Vrstica za odgovor pošiljatelju: samo na reelu, odprtem iz DM pogovora. */
    public static final Set<String> DM_REEL_IDS = setOf(
            ID + "reel_viewer_message_composer",
            ID + "reply_bar_container");

    /** Zavihek Home (feed) — kam pošljemo uporabnika z zavihka Reels. */
    public static final String HOME_TAB_ID = ID + "feed_tab";

    /** Zavihek DM-ji (v tej verziji je v spodnji vrstici). */
    public static final String DIRECT_TAB_ID = ID + "direct_tab";

    private DetectionRules() {
    }

    private static Set<String> setOf(String... ids) {
        return Collections.unmodifiableSet(new HashSet<>(Arrays.asList(ids)));
    }
}
