package si.odisej.detect;

/**
 * Minimalen pogled na vozlišče drevesa, ki ga detektor potrebuje (D6).
 * Namenoma brez besedila in brez contentDescription (Z3): zaznava jih ne rabi.
 */
public interface UiNode {

    /** Polno ime vira, npr. "com.instagram.android:id/clips_tab", ali null. */
    String getViewId();

    String getClassName();

    boolean isVisibleToUser();

    boolean isSelected();

    int getChildCount();

    /** Lahko vrne null (vozlišče je med branjem izginilo). */
    UiNode getChild(int index);
}
