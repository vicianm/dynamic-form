package sk.plaut.dynamicformheader;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Layout which recognizes <code>pinAllowed</code> attributes
 * in views defined in layout XML file.</p>
 *
 * <p>View with <code>pinAllowed="true"</code> will be pinned to
 * footer or header of the form is it is no longer visible.</p>
 */
public class DynamicFormHeaderLayout extends LinearLayout implements View.OnScrollChangeListener {

    private boolean inflateFinished = false;

    private LinearLayout headerLayout;
    private LinearLayout footerLayout;
    private ScrollView formLayoutScrollView;
    private LinearLayout formLayout;

    private boolean delegatedFormPaddingSet = false;
    private int delegatedFormPaddingLeft = -1;
    private int delegatedFormPaddingTop = -1;
    private int delegatedFormPaddingRight = -1;
    private int delegatedFormPaddingBottom = -1;

    private List<PinnableViewData> pinnableViewData = new LinkedList<>();

    /**
     * Immutable list of formSectionHeaders which is passed to
     * onActiveSectionChanged(...) callback if user defined on in layout XML file.
     * @see sk.plaut.dynamicformheader.R.styleable#DynamicFormHeaderLayoutAttrs_onActiveSectionChanged
     */
    private List<View> formSectionHeaders;

    private View currentActiveSectionHeader = null;

    private MethodWithContext onCreateHeaderMethod;
    private MethodWithContext onCreateFooterMethod;
    private MethodWithContext onActiveSectionChangedMethod;

    public DynamicFormHeaderLayout(Context context) {
        this(context, null);
    }

    public DynamicFormHeaderLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DynamicFormHeaderLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public DynamicFormHeaderLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.resolveAttributes(context, attrs, defStyleAttr, defStyleRes);
        initLayout();
    }

    private void resolveAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        final TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.DynamicFormHeaderLayoutAttrs,
                defStyleAttr, defStyleRes);
        try {

            if (context.isRestricted()) {
                throw new IllegalStateException("The customAttribute: onCreateFooter / onCreateHeader cannot be used within a restricted context");
            }

            final String onCreateHeaderReference = a.getString(R.styleable.DynamicFormHeaderLayoutAttrs_onCreateHeader);
            if (onCreateHeaderReference != null) {
                this.onCreateHeaderMethod = resolveMethod(this, onCreateHeaderReference, View.class);
            }

            final String onCreateFooterReference = a.getString(R.styleable.DynamicFormHeaderLayoutAttrs_onCreateFooter);
            if (onCreateFooterReference != null) {
                this.onCreateFooterMethod = resolveMethod(this, onCreateFooterReference, View.class);
            }

            final String onActiveSectionChangedReference = a.getString(R.styleable.DynamicFormHeaderLayoutAttrs_onActiveSectionChanged);
            if (onActiveSectionChangedReference != null) {
                this.onActiveSectionChangedMethod = resolveMethod(this, onActiveSectionChangedReference, List.class, int.class);
            }

            resolvePaddingAttributes(a);

        } finally {
            a.recycle();
        }
    }

    private void resolvePaddingAttributes(TypedArray a) {
        int delegatedFormPadding = a.getDimensionPixelSize(R.styleable.DynamicFormHeaderLayoutAttrs_formPadding, -1);
        delegatedFormPaddingLeft = a.getDimensionPixelSize(R.styleable.DynamicFormHeaderLayoutAttrs_formPaddingLeft, -1);
        delegatedFormPaddingTop = a.getDimensionPixelSize(R.styleable.DynamicFormHeaderLayoutAttrs_formPaddingTop, -1);
        delegatedFormPaddingRight = a.getDimensionPixelSize(R.styleable.DynamicFormHeaderLayoutAttrs_formPaddingRight, -1);
        delegatedFormPaddingBottom = a.getDimensionPixelSize(R.styleable.DynamicFormHeaderLayoutAttrs_formPaddingBottom, -1);

        // Check whether padding and formPaddingLeft/Top/Right/Bottom are used exclusively
        if (delegatedFormPaddingLeft > -1 || delegatedFormPaddingTop > -1 || delegatedFormPaddingRight > -1 || delegatedFormPaddingBottom > -1) {
            if (delegatedFormPadding > -1) {
                throw new IllegalStateException("Both 'formPadding' and 'formPaddingLeft/Top/Right/Bottom' cannot be defined simultaneously.");
            }
            if (delegatedFormPaddingLeft < 0) delegatedFormPaddingLeft = 0;
            if (delegatedFormPaddingTop < 0) delegatedFormPaddingTop = 0;
            if (delegatedFormPaddingRight < 0) delegatedFormPaddingRight = 0;
            if (delegatedFormPaddingBottom < 0) delegatedFormPaddingBottom = 0;
            delegatedFormPaddingSet = true;
        }
        if (delegatedFormPadding > -1) {
            delegatedFormPaddingLeft = delegatedFormPadding;
            delegatedFormPaddingTop = delegatedFormPadding;
            delegatedFormPaddingRight = delegatedFormPadding;
            delegatedFormPaddingBottom = delegatedFormPadding;
            delegatedFormPaddingSet = true;
        }
    }

    @Override
    public void addView(View child) {
        super.addView(child);
    }

    /**
     * Call this method if you need to further customize the container of form components.
     * E.g. by adding some additional padding, background color etc.
     * @return {@link LinearLayout} instance which actually holds
     * all the form components.
     */
    public LinearLayout getFormLayout() {
        return formLayout;
    }

    /**
     * Call this method if you need to further customize the header container.
     * E.g. by adding some additional padding, background color etc.
     * @return {@link LinearLayout} used as a container for header views.
     */
    public LinearLayout getHeaderLayout() {
        return headerLayout;
    }

    /**
     * Call this method if you need to further customize the footer container.
     * E.g. by adding some additional padding, background color etc.
     * @return {@link LinearLayout} used as a container for footer views.
     */
    public LinearLayout getFooterLayout() {
        return footerLayout;
    }

    /**
     * @return {@link ScrollView} which holds the form container {@link ScrollView} which wraps the form container.
     * @see #getFormLayout()
     */
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
        setHorizontalFormPaddingIfSet(headerLayout);
        return headerLayout;
    }

    protected LinearLayout createFooterLayout() {
        LinearLayout footerLayout = new LinearLayout(getContext());
        footerLayout.setOrientation(LinearLayout.VERTICAL);
        setHorizontalFormPaddingIfSet(footerLayout);
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
        setFormPaddingIfSet(formLayout);
        return formLayout;
    }

    /**
     * Set form padding if defined in layout XML
     * (formPadding, formPaddingLeft, formPaddingTop, ...)
     **/
    protected void setHorizontalFormPaddingIfSet(View view) {
        if (delegatedFormPaddingSet) {
            view.setPadding(
                    delegatedFormPaddingLeft, 0,
                    delegatedFormPaddingRight, 0
            );
        }
    }

    /**
     * Set form padding if defined in layout XML
     * (formPadding, formPaddingLeft, formPaddingTop, ...)
     **/
    protected void setFormPaddingIfSet(View view) {
        if (delegatedFormPaddingSet) {
            view.setPadding(
                    delegatedFormPaddingLeft,
                    delegatedFormPaddingTop,
                    delegatedFormPaddingRight,
                    delegatedFormPaddingBottom
            );
        }
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
        if (layoutParams instanceof DynamicFormHeaderLayout.LayoutParams) {
            boolean pinAllowed = ((LayoutParams) layoutParams).pinAllowed;

            if (pinAllowed) {
                PinnableViewData headerData = new PinnableViewData(child);
                View formView = headerData.getFormView();

                // Get header view instance from callback method
                if(this.onCreateHeaderMethod != null){
                    Object result = this.onCreateHeaderMethod.invoke(formView);
                    if(result instanceof  View){
                        headerData.setPinnedViewHeader((View) result);
                    } else {
                        throw new RuntimeException("Method invocation of onCreateHeaderMethod did not return new view");
                    }
                }

                // Get footer view instance from callback method
                if(this.onCreateFooterMethod != null){
                    Object result = this.onCreateFooterMethod.invoke(formView);
                    if(result instanceof  View){
                        headerData.setPinnedViewFooter((View)result);
                    } else {
                        throw new RuntimeException("Method invocation of onCreateFooterMethod did not return new view");
                    }
                }

                // Configure on click listeners which
                // scrolls the form in a way that section
                // under the header/footer will be visible.
                setHeaderOnClickListener(headerData.getFormView(), headerData.getPinnedViewHeader());
                setHeaderOnClickListener(headerData.getFormView(), headerData.getPinnedViewFooter());

                pinnableViewData.add(headerData);
            }
        }
    }

    /**
     * Sets click listener for <code>pinnedView</code> which scrolls the form
     * in a way that section under the header/footer will be visible.
     */
    protected void setHeaderOnClickListener(final View formView, final View pinnedView) {
        pinnedView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                // Calculate header height at time user scrolls the
                // form in a such way that <code>formView</code> is
                // the first visible component of the form.
                // We can rely on the fact that <code>pinnableViewData</code>
                // are ordered the same way as views are shown in header/footer.
                int headerHeigthAfterScroll = 0;
                for (PinnableViewData data : pinnableViewData) {

                    // We are done if we reached the view on which user clicked
                    boolean done =
                            pinnedView == data.getPinnedViewHeader() ||
                            pinnedView == data.getPinnedViewFooter();
                    if (done) break;

                    headerHeigthAfterScroll += data.getPinnedViewHeader().getHeight();
                }
                formLayoutScrollView.smoothScrollTo(0,
                        (int)formView.getY() - headerHeigthAfterScroll);
            }
        });
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        inflateFinished = true;
        initFormSectionHeadersList();
    }

    /**
     * Called after XML layout is inflated to init unmodifiable
     * list of section headers which serves as a parameter for
     * {@link #onActiveSectionChangedMethod} callback.
     */
    private void initFormSectionHeadersList() {
        if (onActiveSectionChangedMethod != null) {
            List<View> formSectionHeaders = new LinkedList<>();
            for (PinnableViewData data : pinnableViewData) {
                formSectionHeaders.add(data.getFormView());
            }
            this.formSectionHeaders = Collections.unmodifiableList(formSectionHeaders);
        }
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LinearLayout.LayoutParams;
    }

    @Override
    protected DynamicFormHeaderLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public DynamicFormHeaderLayout.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected DynamicFormHeaderLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        super.updateViewLayout(view, params);
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

        // calculate current header size and footer size from pinnableViewData,
        // headerLayout.getHeight cant be used due inconsistent data after addView when swipe is used
        int headerSizeByData = 0;
        int footerSizeByData = 0;
        for (PinnableViewData data : pinnableViewData) {
            if(data.getState() == PinnableViewData.State.PINNED_UP){
                headerSizeByData += data.getPinnedViewHeader().getHeight();
            } else if(data.getState() == PinnableViewData.State.PINNED_DOWN){
                footerSizeByData += data.getPinnedViewFooter().getHeight();
            }
        }

        // Check 'pin allowed' views visibility
        boolean updateHeaders = false;
        for (PinnableViewData data : pinnableViewData) {
            float viewY = data.getFormView().getY();
            if (scrollY + headerSizeByData > viewY + (data.getState() == PinnableViewData.State.PINNED_UP ? data.getPinnedViewHeader().getHeight() : 0)) {
                updateHeaders |= data.update(PinnableViewData.State.PINNED_UP);
            } else if (scrollY + v.getHeight() - footerSizeByData < viewY + (data.getState() == PinnableViewData.State.PINNED_DOWN ? 0 : data.getPinnedViewFooter().getHeight())) {
                updateHeaders |= data.update(PinnableViewData.State.PINNED_DOWN);
            } else {
                updateHeaders |= data.update(PinnableViewData.State.UNPINNED);
            }
        }

        if (updateHeaders) {
            boolean firstUnpinnedViewEvaluated = false;
            for (int i = 0; i<pinnableViewData.size(); i++) {
                PinnableViewData data = pinnableViewData.get(i);

                // Update header/footer containers if any change occurred

                if (data.isUpdate()) {

                    // Ensure child is removed from parent
                    headerLayout.removeView(data.getPinnedViewHeader());
                    footerLayout.removeView(data.getPinnedViewFooter());

                    switch(data.getState()) {
                        case PINNED_UP:
                            headerLayout.addView(data.getPinnedViewHeader());
                            break;
                        case PINNED_DOWN:
                            footerLayout.addView(data.getPinnedViewFooter(), 0);
                            break;
                        case UNPINNED:
                            // nothing to do with header/footer container
                            break;
                    }
                }

                if (onActiveSectionChangedMethod != null) {

                    // Call onActiveSectionChanged(...) callback if there
                    // is a new 'active section'.
                    //
                    // Note: Active section of the form is the very first section of
                    //       the form which is not collapsed. Thus header of active section
                    //       is the very first one which is unpinned.

                    if (!firstUnpinnedViewEvaluated && data.getState() == PinnableViewData.State.UNPINNED) {
                        if (currentActiveSectionHeader != data.getFormView()) { // is there a new 'first unpinned view'?
                            currentActiveSectionHeader = data.getFormView();
                            onActiveSectionChangedMethod.invoke(formSectionHeaders, i);
                        }
                        firstUnpinnedViewEvaluated = true;
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
            TypedArray typedArray = c.obtainStyledAttributes(attrs, R.styleable.DynamicFormHeaderLayoutParams);
            try {
                this.pinAllowed = typedArray.getBoolean(R.styleable.DynamicFormHeaderLayoutParams_pinAllowed, false);
            } finally {
                typedArray.recycle();
            }
        }

        public boolean isPinAllowed() {
            return pinAllowed;
        }
    }

    private static class MethodWithContext {

        private Method method;
        private Context context;

        public MethodWithContext(Method method, Context context) {
            this.method = method;
            this.context = context;
        }

        private void invoke(Object... args) {
            try {
                method.invoke(context, args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not execute non-public method for customAttribute:" + method.getName(), e);
            } catch (InvocationTargetException e) {
                throw new IllegalStateException( "Could not execute method for customAttribure:" + method.getName(), e);
            }
        }
    }

    /**
     * Copy from {@link View}, DeclaredOnClickListener
     */
    private MethodWithContext resolveMethod(@NonNull View hostView, @NonNull String mMethodName, Class<?>... methodParameterTypes) {
        Context context = hostView.getContext();
        while (context != null) {
            try {
                if (!context.isRestricted()) {
                    final Method method = context.getClass().getMethod(mMethodName, methodParameterTypes);
                    if (method != null) {
                        return new MethodWithContext(method, context);
                    }
                }
            } catch (NoSuchMethodException e) {
                // Failed to find method, keep searching up the hierarchy.
            }

            if (context instanceof ContextWrapper) {
                context = ((ContextWrapper) context).getBaseContext();
            } else {
                // Can't search up the hierarchy, null out and fail.
                context = null;
            }
        }

        final int id = hostView.getId();
        final String idText = id == NO_ID ? "" : " with id '"
                + hostView.getContext().getResources().getResourceEntryName(id) + "'";
        throw new IllegalStateException("Could not find method " + mMethodName
                + "(View) in a parent or ancestor Context customAttribute "
                + "attribute defined on view " + hostView.getClass() + idText);
    }

}
