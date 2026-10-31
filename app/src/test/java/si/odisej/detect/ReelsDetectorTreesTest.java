package si.odisej.detect;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

/**
 * Vsako posneto drevo (Instagram 453.0.0.57.79) mora dati pričakovano stanje.
 * Ob posodobitvi Instagrama: novi dumpi → nova drevesa → ta test pove, kaj se je zlomilo.
 */
@RunWith(Parameterized.class)
public class ReelsDetectorTreesTest {

    @Parameterized.Parameters(name = "{0} -> {1}")
    public static Collection<Object[]> trees() {
        return Arrays.asList(new Object[][]{
                {"reels-tab.txt", ScreenState.REELS_TAB},
                {"reels-viewer-iz-feeda.txt", ScreenState.REELS_VIEWER},
                {"reels-viewer-iz-explore.txt", ScreenState.REELS_VIEWER},
                {"reel-iz-dm-naslednji.txt", ScreenState.REELS_VIEWER},
                {"reel-iz-dm.txt", ScreenState.REELS_FROM_DM},
                {"feed.txt", ScreenState.OTHER},
                {"feed-po-back.txt", ScreenState.OTHER},
                {"reel-v-feedu.txt", ScreenState.OTHER},
                {"story.txt", ScreenState.OTHER},
                {"dm-seznam.txt", ScreenState.OTHER},
                {"dm-pogovor.txt", ScreenState.OTHER},
                {"explore.txt", ScreenState.OTHER},
                {"profil.txt", ScreenState.OTHER},
                {"profil-reels-mreza.txt", ScreenState.OTHER},
                {"nova-objava.txt", ScreenState.OTHER},
        });
    }

    private final String file;
    private final ScreenState expected;

    public ReelsDetectorTreesTest(String file, ScreenState expected) {
        this.file = file;
        this.expected = expected;
    }

    @Test
    public void classifiesRecordedTree() throws Exception {
        assertEquals(expected, new ReelsDetector().classify(TreeParser.parseResource(file)));
    }
}
