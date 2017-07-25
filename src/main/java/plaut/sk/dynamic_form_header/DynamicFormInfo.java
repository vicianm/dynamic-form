package plaut.sk.dynamic_form_header;

import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

/**
 * Created by loch on 7/21/2017.
 */

public abstract class DynamicFormInfo {

    private int resourceViewID;

    public int state;
    public boolean shouldUpdate;

    private FormInnerLinearLayout formLayout;
    //TODO
//        private AponetActvityBase baseLogic;

    public DynamicFormInfo(@LayoutRes int resourceViewID) {
        this.resourceViewID = resourceViewID;
    }

    public final void init(LayoutInflater inflater) {
        View view = inflater.inflate(resourceViewID, null);
        if(view instanceof FormInnerLinearLayout){
            formLayout = (FormInnerLinearLayout) view;
        } else {
            throw new RuntimeException("View must be instance of FormInnerLinearLayout");
        }
        this.onCreate(view);
    }

    protected void onCreate(){
        this.onCreate(this.formLayout);
    }
    public abstract void onCreate(View view);

    public FormInnerLinearLayout getView() {
        return formLayout;
    }

}
