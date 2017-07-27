package sk.plaut.dynamicformheader;

import android.view.View;

public class PinnableViewData {

    public enum State {
        PINNED_UP,
        PINNED_DOWN,
        UNPINNED
    }

    private State state;

    private boolean update;

    /**
     * View with 'pinAllowed' attribute set.
     */
    private View formView;

    private View pinnedViewHeader;
    private View pinnedViewFooter;

    public PinnableViewData(View formView) {
        this.formView = formView;
    }

    public State getState() {
        return state;
    }

    public boolean isUpdate() {
        return update;
    }

    public View getFormView() {
        return formView;
    }

    public void setPinnedViewHeader(View pinnedViewHeader) {
        this.pinnedViewHeader = pinnedViewHeader;
    }

    public void setPinnedViewFooter(View pinnedViewFooter) {
        this.pinnedViewFooter = pinnedViewFooter;
    }

    public View getPinnedViewFooter() {
        return pinnedViewFooter;
    }

    public View getPinnedViewHeader() {
        return pinnedViewHeader;
    }

    public boolean update(State state) {
        update = this.state != state;
        if (update) this.state = state;
        return update;
    }

}
