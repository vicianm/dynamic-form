package plaut.sk.dynamic_form_header;

import android.content.Context;
import android.support.annotation.AttrRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by loch on 7/25/2017.
 */

public class DynamicFormLayout extends LinearLayout {

    private LayoutInflater inflater;

    private LinearLayout formSkeletCenter;
    private ScrollView formSkeletScrollView;

    private LinearLayout formSketelHeader;
    private LinearLayout formSketelfooter;

    private List<DynamicFormInfo> forms;

    public DynamicFormLayout(@NonNull Context context) {
        super(context);
    }

    public DynamicFormLayout(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        this.forms = new ArrayList<>();

        this.inflater = (LayoutInflater) context.getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.dynamic_form_layout, null);

        this.formSkeletCenter = (LinearLayout) view.findViewById(R.id.form_skelet_center);
        this.formSkeletScrollView = (ScrollView) view.findViewById(R.id.form_skelet_center_scroll);
        this.formSketelHeader = (LinearLayout) view.findViewById(R.id.form_skelet_header);
        this.formSketelfooter = (LinearLayout) view.findViewById(R.id.form_skelet_footer);

        this.formSkeletScrollView.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
            DynamicFormLayout.this.onScrollChange(v, scrollY);
            }
        });

        this.addView(view);
    }

    public DynamicFormLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public DynamicFormLayout(@NonNull Context context, @Nullable AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void addFormView(DynamicFormInfo formInfo){
        this.forms.add(formInfo);

        // tu alebo az cele on layout???
        formInfo.init(this.inflater);
        this.formSkeletCenter.addView(formInfo.getView());
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
    }

    private void onScrollChange(View v, int scrollY) {
        int scrollHeight = v.getHeight();

        int currentHeightPoiner = 0;
        // find visible forms  -- sorted view list
        for (DynamicFormInfo form: forms) {
            View freezedView = form.getView().getFreezedView();
            if(freezedView == null){
                return;
            }
            int formViewHeight = form.getView().getFreezedView().getHeight();

            int absoluteFormY0 = currentHeightPoiner;
            int absoluteFormYS = currentHeightPoiner + formViewHeight;

            //TODO check if state isnt same....
            if(scrollY > absoluteFormYS - formSketelHeader.getHeight()) {
                if(form.state != 0) {
                    //hide up
                    form.state = 0;
                    form.shouldUpdate = true;
                }
            } else if (scrollY + scrollHeight < absoluteFormY0 + formSketelfooter.getHeight()){
                if(form.state != 2) {
                    //hide down
                    form.state = 2;
                    form.shouldUpdate = true;
                }
            } else {
                if(form.state != 3) {
                    //it is visible
                    form.state = 1;
                    form.shouldUpdate = true;
                }
            }

            //Log.i(TAG, "state " + form.state + " formHeight: " + formViewHeight + " y pos: "+  absoluteFormY0);
            currentHeightPoiner += formViewHeight + form.getView().getHeight();
        }

        //TODO was update
        reCheckView(formSketelHeader, formSketelfooter);
    }

    private void reCheckView(LinearLayout form_skelet_header, LinearLayout form_skelet_footer) {
        for (DynamicFormInfo form: forms) {
            if(!form.shouldUpdate){
                continue;
            }
            form.shouldUpdate = false;

            Log.i("Info: DynamicFormLayout", "state " + form.state);

            View freezedView = form.getView().getFreezedView();
            if(freezedView != null) {
                form_skelet_header.removeView(freezedView);
                form_skelet_footer.removeView(freezedView);
            }

            switch (form.state){
                case 0: {
                    form_skelet_header.addView(freezedView);
                    continue;
                }
                case 1: {
//                            centerScrollView.addView(form.getFormView());
                    continue;
                }
                case 2: {
                    form_skelet_footer.addView(freezedView);
                    continue;
                }
            }
        }
    }

}
