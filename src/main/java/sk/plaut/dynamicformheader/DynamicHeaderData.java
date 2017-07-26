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

    public DynamicHeaderData(State state, boolean update, View view) {
        this.state = state;
        this.update = update;
        this.view = view;
    }

}
