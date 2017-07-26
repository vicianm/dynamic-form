package sk.plaut.dynamicformheader;

import android.view.View;
import android.widget.Button;

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

    private View pinnedView;

    public PinnableViewData(View formView) {

        Button button = new Button(formView.getContext());
        button.setText(((Button)formView).getText());
        this.pinnedView = button;

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

    public View getPinnedView() {
        return pinnedView;
    }

    public boolean update(State state) {
        update = this.state != state;
        if (update) this.state = state;
        return update;
    }

}
