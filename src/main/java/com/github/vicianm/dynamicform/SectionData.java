package com.github.vicianm.dynamicform;

import android.view.View;

/**
 * Form sections data holder.
 * <p>This data is used to hold state of form sections (section header is pinned up etc.)
 * and to speed up calculations in {@link DynamicFormLayout}.</p>
 */
public class SectionData {

    /**
     * Current state of section header.
     */
    public enum HeaderState {
        PINNED_UP,
        PINNED_DOWN,
        UNPINNED
    }

    private HeaderState headerState;

    private boolean stateUpdate;

    private View unpinnedHeader;
    private View pinnedUpHeader;
    private View pinnedDownHeader;

    public SectionData(View formView) {
        this.unpinnedHeader = formView;
    }

    public HeaderState getHeaderState() {
        return headerState;
    }

    /**
     * @return <code>true</code> if the most recent call to {@link #update(HeaderState)}
     * changed the state of section header and these changes should be reflected by UI.
     */
    public boolean isStateUpdated() {
        return stateUpdate;
    }

    /**
     * @return Reference to unpinned section header, i.e. the <code>View</code>
     * header directly included in form, i.e. the header View declared in
     * layout XML file (the on with <code>pin="true"</code> attribute).
     */
    public View getUnpinnedHeader() {
        return unpinnedHeader;
    }

    public void setPinnedUpHeader(View pinnedUpHeader) {
        this.pinnedUpHeader = pinnedUpHeader;
    }

    public void setPinnedDownHeader(View pinnedDownHeader) {
        this.pinnedDownHeader = pinnedDownHeader;
    }

    /**
     * @return Section header instance displayed at the top of the form when
     * section is 'pinned up' (see {@link HeaderState}).
     */
    public View getPinnedDownHeader() {
        return pinnedDownHeader;
    }

    /**
     * @return Section header instance displayed at the bottom of the form when
     * section is 'pinned down' (see {@link HeaderState}).
     */
    public View getPinnedUpHeader() {
        return pinnedUpHeader;
    }

    public boolean update(HeaderState headerState) {
        stateUpdate = this.headerState != headerState;
        if (stateUpdate) this.headerState = headerState;
        return stateUpdate;
    }

}
