package si.odisej.detect;

/**
 * VSA pravila zaznave na enem mestu (CLAUDE.md pravilo 6). Ko Instagram ob posodobitvi
 * zamenja ID-je, se popravi samo ta datoteka in testi.
 */
public final class DetectionRules {

    public static final String INSTAGRAM_PACKAGE = "com.instagram.android";

    private DetectionRules() {
    }
}
