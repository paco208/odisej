package si.odisej.detect;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

/**
 * Z7 / CLAUDE.md pravilo 5: testna drevesa smejo vsebovati SAMO ID-je, razrede, meje in
 * zastavice. Če kdo po nesreči kopira dump z besedilom (DM-ji, imena), ta test pade.
 */
public class TreeFilesPrivacyTest {

    @Test
    public void everyLineOfEveryTreeMatchesStrictFormat() throws IOException, URISyntaxException {
        URL dir = getClass().getClassLoader().getResource("trees");
        assertTrue("Ni mape trees/", dir != null);
        File[] files = new File(dir.toURI()).listFiles();
        assertTrue("Ni testnih dreves", files != null && files.length > 0);
        for (File file : files) {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (!line.isEmpty() && !TreeParser.LINE.matcher(line).matches()) {
                    fail(file.getName() + ":" + (i + 1) + " ni v dovoljeni obliki");
                }
            }
        }
    }
}
