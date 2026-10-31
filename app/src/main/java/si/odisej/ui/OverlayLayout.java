package si.odisej.ui;

/**
 * Kam postaviti blok (krog + stavek + akcije) po dihanju. Čista Java (vse v pikslih), da je
 * testabilno brez naprave.
 */
public final class OverlayLayout {

    /** Središče bloka kot delež višine okna: "malce nad sredino". */
    public static final float SETTLED_CENTER_FRACTION = 0.45f;

    private OverlayLayout() {
    }

    /**
     * @param windowHeight višina okna (sega pod sistemske vrstice)
     * @param blockHeight  končna višina bloka
     * @param insetTop     statusna vrstica / izrez
     * @param insetBottom  navigacijska vrstica (kretnje ali trije gumbi)
     * @param bottomGap    najmanjši razmik do navigacijske vrstice (48dp)
     * @param topGap       najmanjši razmik pod statusno vrstico
     * @return zgornji rob bloka; spodnji razmik ima prednost pred centriranjem
     */
    public static float settledTop(float windowHeight, float blockHeight, float insetTop,
                                   float insetBottom, float bottomGap, float topGap) {
        float top = SETTLED_CENTER_FRACTION * windowHeight - blockHeight / 2f;
        float maxTop = windowHeight - insetBottom - bottomGap - blockHeight;
        top = Math.min(top, maxTop);
        return Math.max(top, insetTop + topGap);
    }
}
