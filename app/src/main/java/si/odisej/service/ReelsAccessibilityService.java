package si.odisej.service;

import android.accessibilityservice.AccessibilityService;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import si.odisej.detect.DetectionRules;

/**
 * Prejme dogodke iz Instagrama in (v debug) izpiše drevo. Zaznava pride v F1/F2.
 */
public class ReelsAccessibilityService extends AccessibilityService {

    private final TreeDumper dumper = new TreeDumper();

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        dumper.attach(this);
    }

    @Override
    public void onDestroy() {
        dumper.detach(this);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Z2, obramba v globino: XML packageNames že filtrira, a ne zaupamo samo njemu.
        CharSequence pkg = event.getPackageName();
        if (pkg == null || !DetectionRules.INSTAGRAM_PACKAGE.contentEquals(pkg)) {
            return;
        }
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            return;
        }
        // Q4: aktivno okno je lahko tuje (npr. prekrivno okno druge aplikacije),
        // čeprav je dogodek prišel iz Instagrama. Takrat drevesa ne beremo.
        CharSequence rootPkg = root.getPackageName();
        if (rootPkg == null || !DetectionRules.INSTAGRAM_PACKAGE.contentEquals(rootPkg)) {
            dumper.onForeignRoot(rootPkg);
            return;
        }
        dumper.onEvent(event.getEventType(), root);
    }

    @Override
    public void onInterrupt() {
        // Nimamo povratne informacije (zvok, vibracija), ki bi jo bilo treba prekiniti.
    }
}
