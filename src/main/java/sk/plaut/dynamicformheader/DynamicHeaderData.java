package sk.plaut.dynamicformheader;

import android.view.View;

public class DynamicHeaderData {

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
    private View view;

    public DynamicHeaderData(View view) {
        this.view = view;
    }

    public State getState() {
        return state;
    }

    public boolean isUpdate() {
        return update;
    }

    public View getView() {
        return view;
    }

    public void update(State state) {
        update = this.state != state;
        if (update) this.state = state;
    }

}
