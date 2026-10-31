package si.odisej.detect;

import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Ovoj okoli {@link AccessibilityNodeInfo}. Živi samo med enim klicem classify(); vozlišč ne
 * hranimo dlje. Bere samo ID, razred, vidnost in izbranost — nikoli getText() (Z3).
 * recycle() ne kličemo: od API 33 ne naredi nič, pod tem vozlišča pobere GC (izgubimo le pool).
 */
public final class AccessibilityUiNode implements UiNode {

    private final AccessibilityNodeInfo node;

    public AccessibilityUiNode(AccessibilityNodeInfo node) {
        this.node = node;
    }

    @Override
    public String getViewId() {
        return node.getViewIdResourceName();
    }

    @Override
    public String getClassName() {
        CharSequence cls = node.getClassName();
        return cls != null ? cls.toString() : null;
    }

    @Override
    public boolean isVisibleToUser() {
        return node.isVisibleToUser();
    }

    @Override
    public boolean isSelected() {
        return node.isSelected();
    }

    @Override
    public int getChildCount() {
        return node.getChildCount();
    }

    @Override
    public UiNode getChild(int index) {
        AccessibilityNodeInfo child = node.getChild(index);
        return child != null ? new AccessibilityUiNode(child) : null;
    }
}
