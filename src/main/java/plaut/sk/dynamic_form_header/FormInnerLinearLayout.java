package plaut.sk.dynamic_form_header;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

/**
 * Created by loch on 7/25/2017.
 */

public class FormInnerLinearLayout extends LinearLayout {

    private View freezedView = null;
    private View loosedView = null;

    public FormInnerLinearLayout(Context context) {
        super(context);
    }

    public FormInnerLinearLayout(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public FormInnerLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public FormInnerLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);

        if (getChildCount() > 0) {
            int childCount = getChildCount();
            for (int i = 0; i < childCount; i++){
                View child = getChildAt(i);

                CustomLayoutPrams layoutParams = (CustomLayoutPrams) child.getLayoutParams();
                boolean freezed = layoutParams.isFreezed();
                if(freezed){
                    if(freezedView != null && freezedView != child){
                        //TODO
                        throw new RuntimeException("TODO");
                    }
                    freezedView = child;
                }
                boolean loosed = layoutParams.isLoosed();
                if(loosed){
                    if(loosedView != null && loosedView != child){
                        //TODO
                        throw new RuntimeException("TODO");
                    }
                    loosedView = child;
                }
            }
        }
    }

    @Override
    public void requestLayout() {
        super.requestLayout();
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LinearLayout.LayoutParams;
    }

    @Override
    protected LinearLayout.LayoutParams generateDefaultLayoutParams() {
        return new CustomLayoutPrams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public LinearLayout.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new CustomLayoutPrams(this.getContext(), attributeSet);
    }

    @Override
    protected LinearLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new CustomLayoutPrams(p);
    }

    //mysli na to, ze su to layoutparams z LinearLayout
    private static class CustomLayoutPrams extends LinearLayout.LayoutParams {

        private boolean freezed = false;
        private boolean loosed = false;

        public CustomLayoutPrams(Context c, AttributeSet attrs) {
            super(c, attrs);
            this.readCustomParams(c, attrs);
        }

        public CustomLayoutPrams(int width, int height) {
            super(width, height);
        }

        public CustomLayoutPrams(ViewGroup.LayoutParams layoutParams) {
            super(layoutParams);
        }

        private void readCustomParams(Context c, AttributeSet attrs){
            TypedArray typedArray = c.obtainStyledAttributes(attrs, R.styleable.DynamicHeaderParams);
            try {
                this.freezed = typedArray.getBoolean(R.styleable.DynamicHeaderParams_freezed, false);
                this.loosed = typedArray.getBoolean(R.styleable.DynamicHeaderParams_loosed, false);
            } finally {
                typedArray.recycle();
            }
        }

        public boolean isFreezed() {
            return freezed;
        }

        public boolean isLoosed() {
            return loosed;
        }
    }

    public View getFreezedView() {
        return freezedView;
    }

    public View getLoosedView() {
        return loosedView;
    }
}
