package si.odisej.service;

import android.graphics.Rect;
import android.os.SystemClock;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;

/**
 * DEBUG različica (src/debug): izpiše drevo v Logcat s tagom OdisejDump (CONTEXT.md §6.4).
 * Release različica (src/release) je prazna, zato ta koda v release APK ne obstaja (Z5).
 *
 * Izpiše SAMO ID, razred, meje in zastavice — nikoli getText() ali contentDescription (Z3).
 * Oblika vrstice (ista kot testna drevesa v app/src/test/resources/trees/):
 *   &lt;2 presledka na globino&gt;&lt;id ali -&gt; &lt;razred&gt; [l,t,r,b] [sel] [inv] [imp0] [clk]
 */
final class TreeDumper {

    private static final String TAG = "OdisejDump";
    /** Video pošilja content change večkrat na sekundo; dovolj je en izpis na sekundo. */
    private static final long CONTENT_THROTTLE_MS = 1000;
    private static final int MAX_DEPTH = 80;
    /** Logcat odreže sporočila nad ~4 KB, zato vrstice združujemo v kose. */
    private static final int CHUNK_CHARS = 3000;

    private long lastDumpAt;
    private int dumpCount;

    void onEvent(int eventType, AccessibilityNodeInfo root) {
        boolean stateChange = eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        long now = SystemClock.uptimeMillis();
        if (!stateChange && now - lastDumpAt < CONTENT_THROTTLE_MS) {
            return;
        }
        lastDumpAt = now;
        dump(root, stateChange ? "state" : "content");
    }

    void onForeignRoot(CharSequence rootPackage) {
        Log.d(TAG, "Q4: aktivno okno je iz paketa " + rootPackage + " -> drevesa ne berem");
    }

    void log(String message) {
        Log.d(TAG, message);
    }

    private void dump(AccessibilityNodeInfo root, String reason) {
        int id = ++dumpCount;
        Log.d(TAG, "BEGIN #" + id + " " + reason);
        StringBuilder chunk = new StringBuilder();
        ArrayDeque<Object[]> stack = new ArrayDeque<>();
        stack.push(new Object[]{root, 0});
        Rect bounds = new Rect();
        int count = 0;
        while (!stack.isEmpty()) {
            Object[] top = stack.pop();
            AccessibilityNodeInfo node = (AccessibilityNodeInfo) top[0];
            int depth = (Integer) top[1];
            count++;
            appendLine(chunk, node, depth, bounds);
            if (chunk.length() > CHUNK_CHARS) {
                Log.d(TAG, chunk.toString());
                chunk.setLength(0);
            }
            if (depth >= MAX_DEPTH) {
                continue;
            }
            // Otroke potisnemo v obratnem vrstnem redu, da izpis teče od prvega do zadnjega.
            for (int i = node.getChildCount() - 1; i >= 0; i--) {
                AccessibilityNodeInfo child = node.getChild(i);
                if (child != null) {
                    stack.push(new Object[]{child, depth + 1});
                }
            }
        }
        if (chunk.length() > 0) {
            Log.d(TAG, chunk.toString());
        }
        Log.d(TAG, "END #" + id + " nodes=" + count);
    }

    private static void appendLine(StringBuilder out, AccessibilityNodeInfo node, int depth,
                                   Rect bounds) {
        if (out.length() > 0) {
            out.append('\n');
        }
        for (int i = 0; i < depth; i++) {
            out.append("  ");
        }
        String viewId = node.getViewIdResourceName();
        CharSequence cls = node.getClassName();
        node.getBoundsInScreen(bounds);
        out.append(viewId != null ? viewId : "-")
                .append(' ')
                .append(cls != null ? cls : "-")
                .append(" [").append(bounds.left).append(',').append(bounds.top)
                .append(',').append(bounds.right).append(',').append(bounds.bottom).append(']');
        if (node.isSelected()) out.append(" sel");
        if (!node.isVisibleToUser()) out.append(" inv");
        if (!node.isImportantForAccessibility()) out.append(" imp0");
        if (node.isClickable()) out.append(" clk");
    }
}
