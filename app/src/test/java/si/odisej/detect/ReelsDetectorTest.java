package si.odisej.detect;

import static org.junit.Assert.assertEquals;
import static si.odisej.detect.FakeUiNode.node;

import org.junit.Test;

/** Robni primeri na ročno zgrajenih drevesih. */
public class ReelsDetectorTest {

    private final ReelsDetector detector = new ReelsDetector();

    @Test
    public void nullRootIsOther() {
        assertEquals(ScreenState.OTHER, detector.classify(null));
    }

    @Test
    public void emptyTreeIsOther() {
        assertEquals(ScreenState.OTHER, detector.classify(node(null)));
    }

    @Test
    public void visiblePagerIsViewer() {
        assertEquals(ScreenState.REELS_VIEWER,
                detector.classify(node(null, node("clips_viewer_view_pager"))));
    }

    @Test
    public void backupSignalAloneIsViewer() {
        assertEquals(ScreenState.REELS_VIEWER,
                detector.classify(node(null, node("clips_expanded_touch_view"))));
    }

    @Test
    public void hiddenPagerIsOther() {
        // Tako je v feedu, DM-jih, profilu: fragment Reels zavihka je v drevesu, a skrit.
        assertEquals(ScreenState.OTHER,
                detector.classify(node(null, node("clips_viewer_view_pager").hidden())));
    }

    @Test
    public void pagerUnderHiddenParentIsOther() {
        assertEquals(ScreenState.OTHER, detector.classify(
                node(null, node("root_clips_layout", node("clips_viewer_view_pager")).hidden())));
    }

    @Test
    public void selectedReelsTabIsTabEvenWithoutPager() {
        assertEquals(ScreenState.REELS_TAB, detector.classify(
                node(null, node("tab_bar", node("feed_tab"), node("clips_tab").selected()))));
    }

    @Test
    public void unselectedReelsTabIsOther() {
        assertEquals(ScreenState.OTHER, detector.classify(
                node(null, node("tab_bar", node("feed_tab").selected(), node("clips_tab")))));
    }

    @Test
    public void pagerWithReplyBarIsReelFromDm() {
        assertEquals(ScreenState.REELS_FROM_DM, detector.classify(
                node(null, node("clips_viewer_view_pager"), node("reply_bar_container"))));
    }

    @Test
    public void replyBarWithoutPagerIsOther() {
        assertEquals(ScreenState.OTHER,
                detector.classify(node(null, node("reply_bar_container"))));
    }

    @Test
    public void hiddenReplyBarIsPlainViewer() {
        assertEquals(ScreenState.REELS_VIEWER, detector.classify(
                node(null, node("clips_viewer_view_pager"), node("reply_bar_container").hidden())));
    }

    @Test
    public void idsFromOtherPackagesAreIgnored() {
        FakeUiNode foreign = new FakeUiNode("com.example:id/clips_viewer_view_pager", "x");
        assertEquals(ScreenState.OTHER, detector.classify(node(null, foreign)));
    }
}
