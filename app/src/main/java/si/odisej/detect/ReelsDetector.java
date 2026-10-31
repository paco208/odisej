package si.odisej.detect;

import java.util.ArrayDeque;

/**
 * Čista Java: drevo zaslona → {@link ScreenState}. Brez Android odvisnosti, zato se testira
 * na JVM s ponarejenimi drevesi (D6).
 */
public final class ReelsDetector {

    /** Varovalka pred patološkimi drevesi; Instagramova imajo 100–750 vozlišč. */
    private static final int MAX_NODES = 5000;

    public ScreenState classify(UiNode root) {
        if (root == null) {
            return ScreenState.OTHER;
        }
        boolean reels = false;
        boolean dmReel = false;
        int visited = 0;

        ArrayDeque<UiNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty() && visited++ < MAX_NODES) {
            UiNode node = stack.pop();
            // Skrito vozlišče ima skrite tudi otroke: celo poddrevo preskočimo. Tako skrit
            // fragment Reels zavihka ne sproži zaznave, iskanje pa je hitrejše.
            if (!node.isVisibleToUser()) {
                continue;
            }
            String id = node.getViewId();
            if (id != null) {
                if (DetectionRules.REELS_TAB_ID.equals(id) && node.isSelected()) {
                    return ScreenState.REELS_TAB;
                }
                if (DetectionRules.REELS_IDS.contains(id)) {
                    reels = true;
                } else if (DetectionRules.DM_REEL_IDS.contains(id)) {
                    dmReel = true;
                }
                if (reels && dmReel) {
                    return ScreenState.REELS_FROM_DM;
                }
            }
            for (int i = node.getChildCount() - 1; i >= 0; i--) {
                UiNode child = node.getChild(i);
                if (child != null) {
                    stack.push(child);
                }
            }
        }
        return reels ? ScreenState.REELS_VIEWER : ScreenState.OTHER;
    }
}
