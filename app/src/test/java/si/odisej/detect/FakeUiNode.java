package si.odisej.detect;

import java.util.ArrayList;
import java.util.List;

/** Ponarejeno vozlišče za teste. */
final class FakeUiNode implements UiNode {

    final String viewId;
    final String className;
    boolean visible = true;
    boolean selected;
    final List<FakeUiNode> children = new ArrayList<>();

    FakeUiNode(String viewId, String className) {
        this.viewId = viewId;
        this.className = className;
    }

    static FakeUiNode node(String shortId, FakeUiNode... children) {
        FakeUiNode n = new FakeUiNode(
                shortId == null ? null : "com.instagram.android:id/" + shortId,
                "android.widget.FrameLayout");
        for (FakeUiNode c : children) {
            n.children.add(c);
        }
        return n;
    }

    FakeUiNode hidden() {
        visible = false;
        return this;
    }

    FakeUiNode selected() {
        selected = true;
        return this;
    }

    @Override
    public String getViewId() {
        return viewId;
    }

    @Override
    public String getClassName() {
        return className;
    }

    @Override
    public boolean isVisibleToUser() {
        return visible;
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public int getChildCount() {
        return children.size();
    }

    @Override
    public UiNode getChild(int index) {
        return children.get(index);
    }
}
