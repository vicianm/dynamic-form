package sk.plaut.dynamicformheader;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class DynamicHeaderLayout extends FrameLayout {

    private boolean inflateFinished = false;

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

    private void initLayout() {
        formLayout = createFormLayout();
        super.addView(formLayout, 0, formLayout.getLayoutParams());
    }

    public LinearLayout createFormLayout() {
        LinearLayout formLayout = new LinearLayout(getContext());
        formLayout.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT
        );
        formLayout.setLayoutParams(layoutParams);
        return formLayout;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        Log.i("DynamicHeaderLayout", "View child, int index, ViewGroup.LayoutParams params");

        if (inflateFinished) {
            super.addView(child, index, params);
        } else {
            formLayout.addView(child, index, params);
        }
    }

    @Override
    public void addView(View child, int index) {
        Log.i("DynamicHeaderLayout", "View child, int index");
        super.addView(child, index);
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        Log.i("DynamicHeaderLayout", "View child, ViewGroup.LayoutParams params");
        super.addView(child, params);
    }

    @Override
    public void addView(View child, int width, int height) {
        Log.i("DynamicHeaderLayout", "View child, int width, int height");
        super.addView(child, width, height);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        inflateFinished = true;
    }
}
