package si.odisej.detect;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prebere testno drevo v obliki izpisa debug dumperja (TreeDumper):
 * <pre>&lt;2 presledka na globino&gt;&lt;id ali -&gt; &lt;razred&gt; [l,t,r,b] [sel] [inv] [imp0] [clk]</pre>
 */
final class TreeParser {

    /**
     * Edina dovoljena oblika vrstice. Nima prostora za besedilo, zato je tudi varovalka
     * zasebnosti (TreeFilesPrivacyTest). ID je lahko brez paketa (Compose testTag).
     */
    static final Pattern LINE = Pattern.compile(
            "^( *)(-|[a-z][a-z0-9_.]*:id/[A-Za-z0-9_]+|[a-z][a-z0-9_]*) ([A-Za-z0-9_.$]+|-)"
                    + " \\[-?\\d+,-?\\d+,-?\\d+,-?\\d+\\]((?: sel)?(?: inv)?(?: imp0)?(?: clk)?)$");

    private TreeParser() {
    }

    static FakeUiNode parseResource(String name) throws IOException {
        InputStream in = TreeParser.class.getClassLoader().getResourceAsStream("trees/" + name);
        if (in == null) {
            throw new IOException("Ni testnega drevesa trees/" + name);
        }
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    lines.add(line);
                }
            }
            return parse(lines);
        }
    }

    static FakeUiNode parse(List<String> lines) {
        List<FakeUiNode> pathByDepth = new ArrayList<>();
        FakeUiNode root = null;
        for (String line : lines) {
            Matcher m = LINE.matcher(line);
            if (!m.matches()) {
                throw new IllegalArgumentException("Neveljavna vrstica: " + line);
            }
            int depth = m.group(1).length() / 2;
            String id = "-".equals(m.group(2)) ? null : m.group(2);
            String cls = "-".equals(m.group(3)) ? null : m.group(3);
            String flags = m.group(4);
            FakeUiNode node = new FakeUiNode(id, cls);
            node.visible = !flags.contains(" inv");
            node.selected = flags.contains(" sel");

            if (depth == 0) {
                if (root != null) {
                    throw new IllegalArgumentException("Več korenov");
                }
                root = node;
            } else {
                if (depth > pathByDepth.size()) {
                    throw new IllegalArgumentException("Preskočena globina: " + line);
                }
                pathByDepth.get(depth - 1).children.add(node);
            }
            while (pathByDepth.size() > depth) {
                pathByDepth.remove(pathByDepth.size() - 1);
            }
            pathByDepth.add(node);
        }
        if (root == null) {
            throw new IllegalArgumentException("Prazno drevo");
        }
        return root;
    }
}
