package sk.plaut.dynamicformheader;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import plaut.sk.dynamic_form_header.R;

public class DynamicHeaderLayout extends LinearLayout implements View.OnScrollChangeListener {

    private boolean inflateFinished = false;

    private LinearLayout headerLayout;
    private LinearLayout footerLayout;
    private ScrollView formLayoutScrollView;
    private LinearLayout formLayout;

    public DynamicHeaderLayout(Context context) {
        super(context);
        Log.i("DynamicHeaderLayout", "DynamicHeaderLayout(Context context)");
    }

    public DynamicHeaderLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        Log.i("DynamicHeaderLayout", "DynamicHeaderLayout(Context context, @Nullable AttributeSet attrs)");
        initLayout();
    }

    public DynamicHeaderLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        Log.i("DynamicHeaderLayout", "DynamicHeaderLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr)");
    }

    public DynamicHeaderLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        Log.i("DynamicHeaderLayout", "DynamicHeaderLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)");
    }

    @Override
    public void addView(View child) {
        Log.i("DynamicHeaderLayout", "child");
        super.addView(child);
    }

    public LinearLayout getFormLayout() {
        return formLayout;
    }

    public ScrollView getFormLayoutScrollView() {
        return formLayoutScrollView;
    }

    protected void initLayout() {
        formLayout = createFormLayout();
        formLayoutScrollView = createFormLayoutScrollView();
        headerLayout = createHeaderLayout();
        footerLayout = createFooterLayout();

        formLayout.setZ(0);
        headerLayout.setZ(1);
        footerLayout.setZ(1);

        formLayoutScrollView.addView(formLayout);
        super.addView(formLayoutScrollView, 0, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        ));
        super.addView(headerLayout, 1, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
        ));
        super.addView(footerLayout, 2, new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT
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
        }
    }

    @Override
    public void addView(View child, int index) {
        if (inflateFinished) {
            super.addView(child, index);
        } else {
            formLayout.addView(child, index);
        }
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (inflateFinished) {
            super.addView(child, params);
        } else {
            formLayout.addView(child, params);
        }
    }

    @Override
    public void addView(View child, int width, int height) {
        if (inflateFinished) {
            super.addView(child, width, height);
        } else {
            formLayout.addView(child, width, height);
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
        int viewportHeight = v.getHeight();
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
