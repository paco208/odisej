package si.odisej.action;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import java.util.List;

import si.odisej.R;
import si.odisej.detect.DetectionRules;

/** v1 ukrep: uporabnika odstrani iz Reelsov in pokaže toast. */
public final class BackAction {

    private BackAction() {
    }

    /** @return true, če je bil ukrep izveden. */
    public static boolean perform(AccessibilityService service, AccessibilityNodeInfo root,
                                  BlockAction action) {
        boolean done;
        switch (action) {
            case GO_HOME:
                done = clickById(root, DetectionRules.HOME_TAB_ID)
                        || service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                break;
            case BACK:
                done = service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                break;
            case NONE:
            default:
                return false;
        }
        if (done) {
            Toast.makeText(service, R.string.toast_blocked, Toast.LENGTH_SHORT).show();
        }
        return done;
    }

    /** Klikne prvo vidno, klikljivo vozlišče z danim ID-jem. */
    public static boolean clickById(AccessibilityNodeInfo root, String viewId) {
        if (root == null) {
            return false;
        }
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByViewId(viewId);
        for (AccessibilityNodeInfo node : nodes) {
            if (node.isVisibleToUser() && node.isClickable()
                    && node.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
                return true;
            }
        }
        return false;
    }
}
