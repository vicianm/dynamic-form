package sk.plaut.dynamicformheader;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.Collection;
import java.util.LinkedList;

import plaut.sk.dynamic_form_header.R;

public class DynamicHeaderLayout extends LinearLayout implements View.OnScrollChangeListener {

    private boolean inflateFinished = false;

    private LinearLayout headerLayout;
    private LinearLayout footerLayout;
    private ScrollView formLayoutScrollView;
    private LinearLayout formLayout;

    private Collection<PinnableViewData> pinnableViewData = new LinkedList<>();

    public DynamicHeaderLayout(Context context) {
        super(context);
        initLayout();
    }

    public DynamicHeaderLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        initLayout();
    }

    public DynamicHeaderLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initLayout();
    }

    public DynamicHeaderLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initLayout();
    }

    @Override
    public void addView(View child) {
        super.addView(child);
    }

    public LinearLayout getFormLayout() {
        return formLayout;
    }

    public ScrollView getFormLayoutScrollView() {
        return formLayoutScrollView;
    }

    protected void initLayout() {

        FrameLayout container = new FrameLayout(getContext());

        formLayout = createFormLayout();
        formLayoutScrollView = createFormLayoutScrollView();
        headerLayout = createHeaderLayout();
        footerLayout = createFooterLayout();

        // ScrollView
        formLayoutScrollView.addView(formLayout);
        container.addView(formLayoutScrollView, 0, new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));

        // Header
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        container.addView(headerLayout, 1, headerParams);

        // Footer
        FrameLayout.LayoutParams footerParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        );
        footerParams.gravity = Gravity.BOTTOM;
        container.addView(footerLayout, 1, footerParams);

        super.addView(container, 0, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
        ));
    }

    protected LinearLayout createHeaderLayout() {
        LinearLayout headerLayout = new LinearLayout(getContext());
        headerLayout.setOrientation(LinearLayout.VERTICAL);
        return headerLayout;
    }

    protected LinearLayout createFooterLayout() {
        LinearLayout footerLayout = new LinearLayout(getContext());
        footerLayout.setOrientation(LinearLayout.VERTICAL);
        return footerLayout;
    }

    protected ScrollView createFormLayoutScrollView() {
        ScrollView scrollView = new ScrollView(getContext());
        scrollView.setOnScrollChangeListener(this);
        return scrollView;
    }

    protected LinearLayout createFormLayout() {
        LinearLayout formLayout = new LinearLayout(getContext());
        formLayout.setOrientation(LinearLayout.VERTICAL);
        return formLayout;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        if (inflateFinished) {
            super.addView(child, index, params);
        } else {
            formLayout.addView(child, index, params);
            updateHeaderData(child);
        }
    }

    @Override
    public void addView(View child, int index) {
        if (inflateFinished) {
            super.addView(child, index);
        } else {
            formLayout.addView(child, index);
            updateHeaderData(child);
        }
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (inflateFinished) {
            super.addView(child, params);
        } else {
            formLayout.addView(child, params);
            updateHeaderData(child);
        }
    }

    @Override
    public void addView(View child, int width, int height) {
        if (inflateFinished) {
            super.addView(child, width, height);
        } else {
            formLayout.addView(child, width, height);
            updateHeaderData(child);
        }
    }

    /**
     * Adds <code>child</code> to header data if child's
     * <code>pinAllowed</code> attribute is set.
     * @param child
     */
    private void updateHeaderData(View child) {
        ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
        if (layoutParams instanceof DynamicHeaderLayout.LayoutParams) {
            boolean pinAllowed = ((LayoutParams) layoutParams).pinAllowed;
            if (pinAllowed) {
                PinnableViewData headerData = new PinnableViewData(child);
                pinnableViewData.add(headerData);
            }
        }
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        inflateFinished = true;
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LinearLayout.LayoutParams;
    }

    @Override
    protected DynamicHeaderLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public DynamicHeaderLayout.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected DynamicHeaderLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

        // Check 'pin allowed' views visibility
        boolean updateHeaders = false;
        for (PinnableViewData data : pinnableViewData) {
            float viewY = data.getFormView().getY();
            if (scrollY > viewY + data.getFormView().getHeight()) {
                updateHeaders |= data.update(PinnableViewData.State.PINNED_UP);
            } else if (scrollY+v.getHeight() < viewY) {
                updateHeaders |= data.update(PinnableViewData.State.PINNED_DOWN);
            } else {
                updateHeaders |= data.update(PinnableViewData.State.UNPINNED);
            }
        }

        Log.i("DynamicHeaderLayout", "updateHeaders: " + updateHeaders);

        // Update header/footer containers if any change occurred
        if (updateHeaders) {
            for (PinnableViewData data : pinnableViewData) {
                if (data.isUpdate()) {

                    // Ensure child is removed from parent
                    headerLayout.removeView(data.getPinnedView());
                    footerLayout.removeView(data.getPinnedView());

                    switch(data.getState()) {
                        case PINNED_UP:
                            headerLayout.addView(data.getPinnedView());
                            break;
                        case PINNED_DOWN:
                            footerLayout.addView(data.getPinnedView(), 0);
                            break;
                        case UNPINNED:
                            // nothing to do
                            break;
                    }
                }
            }
        }
    }

    private static class LayoutParams extends LinearLayout.LayoutParams {

        private boolean pinAllowed = false;

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.readCustomParams(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
        }

        private void readCustomParams(Context c, AttributeSet attrs){
            TypedArray typedArray = c.obtainStyledAttributes(attrs, R.styleable.DynamicHeaderLayoutParams);
            try {
                this.pinAllowed = typedArray.getBoolean(R.styleable.DynamicHeaderLayoutParams_pinAllowed, false);
            } finally {
                typedArray.recycle();
            }
        }

        public boolean isPinAllowed() {
            return pinAllowed;
        }
    }

}
