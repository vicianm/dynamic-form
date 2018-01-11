package com.github.vicianm.stickylinearlayout;

import android.content.Context;
import android.content.ContextWrapper;
import android.content.res.TypedArray;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

/**
 * <p>Layout which recognizes <code>sectionHeader</code> attributes
 * in views defined in layout XML file.</p>
 *
 * <p>View with <code>sectionHeader="true"</code> will be pinned to
 * footer or header of the form is it is no longer visible.</p>
 */
public class StickyLinearLayout extends LinearLayout implements View.OnScrollChangeListener, ViewTreeObserver.OnGlobalFocusChangeListener {

    public static final String TAG = StickyLinearLayout.class.getSimpleName();

    private boolean inflateFinished = false;

    private ScrollView headerScrollView;
    private LinearLayout headerLayout;
    private LinearLayout footerLayout;
    private ScrollView formLayoutScrollView;
    private LinearLayout formLayout;

    /**
     * Default 'scroll to section' margin values used
     * by all sections until not overiden by a specific section.
     * @see com.github.vicianm.stickylinearlayout.R.attr#scrollToSectionMargin
     */
    private int defaultScrollToSectionMargin = 0;
    private int implicitScrollToY = -1;

    private int previousScrollY = -1;
    private int previousScrollX = -1;

    private boolean delegatedFormPaddingSet = false;
    private int delegatedFormPaddingLeft = -1;
    private int delegatedFormPaddingTop = -1;
    private int delegatedFormPaddingRight = -1;
    private int delegatedFormPaddingBottom = -1;

    private List<SectionData> sectionsData = new LinkedList<>();

    private SectionData activeSectionAfterScroll = null;
    private SectionData activeSection = null;
    private int activeSectionIndex = -1;

    private MethodWithContext onCreateHeaderMethod;
    private MethodWithContext onCreateFooterMethod;
    private MethodWithContext onActiveSectionChangedMethod;

    // TODO -1 default; specify as XML attribute
    private int maxHeaderRows = 2;
    // TODO -1 default; specify as XML attribute
    private int maxFooterRows = -1;

    public StickyLinearLayout(Context context) {
        this(context, null);
    }

    public StickyLinearLayout(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public StickyLinearLayout(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public StickyLinearLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        this.resolveAttributes(context, attrs, defStyleAttr, defStyleRes);
        initLayout();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        // We no longer need to focus for focus changes.
        // See #onGlobalFocusChanged(oldFocus, newFocus)
        getViewTreeObserver().removeOnGlobalFocusChangeListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();

        // Start listening for global focus changes. This
        // way we always keep track of currently active form section.
        // See #onGlobalFocusChanged(oldFocus, newFocus)
        getViewTreeObserver().addOnGlobalFocusChangeListener(this);
    }

    private void resolveAttributes(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes){
        final TypedArray a = context.obtainStyledAttributes(
                attrs,
                R.styleable.StickyLinearLayoutAttrs,
                defStyleAttr, defStyleRes);
        try {

            if (context.isRestricted()) {
                throw new IllegalStateException("The customAttribute: onCreateFooter / onCreateHeader cannot be used within a restricted context");
            }

            final String onCreateHeaderReference = a.getString(R.styleable.StickyLinearLayoutAttrs_onCreateHeader);
            if (onCreateHeaderReference != null) {
                this.onCreateHeaderMethod = resolveMethod(this, onCreateHeaderReference, View.class);
            }

            final String onCreateFooterReference = a.getString(R.styleable.StickyLinearLayoutAttrs_onCreateFooter);
            if (onCreateFooterReference != null) {
                this.onCreateFooterMethod = resolveMethod(this, onCreateFooterReference, View.class);
            }

            final String onActiveSectionChangedReference = a.getString(R.styleable.StickyLinearLayoutAttrs_onActiveSectionChanged);
            if (onActiveSectionChangedReference != null) {
                this.onActiveSectionChangedMethod = resolveMethod(this, onActiveSectionChangedReference, List.class, int.class, int.class);
            }

            defaultScrollToSectionMargin = a.getDimensionPixelSize(R.styleable.StickyLinearLayoutAttrs_defaultScrollToSectionMargin, 0);

            resolvePaddingAttributes(a);

        } finally {
            a.recycle();
        }
    }

    private void resolvePaddingAttributes(TypedArray a) {
        int delegatedFormPadding = a.getDimensionPixelSize(R.styleable.StickyLinearLayoutAttrs_formPadding, -1);
        delegatedFormPaddingLeft = a.getDimensionPixelSize(R.styleable.StickyLinearLayoutAttrs_formPaddingLeft, -1);
        delegatedFormPaddingTop = a.getDimensionPixelSize(R.styleable.StickyLinearLayoutAttrs_formPaddingTop, -1);
        delegatedFormPaddingRight = a.getDimensionPixelSize(R.styleable.StickyLinearLayoutAttrs_formPaddingRight, -1);
        delegatedFormPaddingBottom = a.getDimensionPixelSize(R.styleable.StickyLinearLayoutAttrs_formPaddingBottom, -1);

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
        int paramHeaderHeight = maxHeaderRows <= 0
                ? LayoutParams.WRAP_CONTENT
                : maxHeaderRows * 99; // TODO calculate 99 from header view
        FrameLayout.LayoutParams headerParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                paramHeaderHeight
        );

        headerScrollView = new ScrollView(getContext());
        headerScrollView.setSmoothScrollingEnabled(false);
        headerScrollView.setHorizontalScrollBarEnabled(false);
        headerScrollView.setVerticalScrollBarEnabled(false);
        headerScrollView.addView(headerLayout);

        container.addView(headerScrollView, 1, headerParams);

        // Footer
        int paramFooterHeight = maxFooterRows <= 0
                ? LayoutParams.WRAP_CONTENT
                : maxFooterRows * 99; // TODO calculate 99 from footer view
        FrameLayout.LayoutParams footerParams = new FrameLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                paramFooterHeight
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

        final ScrollView scrollView = new ScrollView(getContext()) {
            @Override
            protected void onSizeChanged(int w, int h, int oldw, int oldh) {

                Log.d(TAG, "onSizeChanged(int w, int h, int oldw, int oldh)");

                activeSectionAfterScroll = activeSection;

                super.onSizeChanged(w, h, oldw, oldh);


                // Update section data and views after container size is changed.
                // The size change might cause that different headers are pinned/unpinned
                // in header/footer container views.
                // Note: ScrollView size is typically changed after soft keyboard is
                //       shown or hidden. Unfortunately there is no keyboard show/hide
                //       callback in Android. So far updating UI in post(...) method works.
                //       For problems with soft keyboard show/hide detection see following link:
                //       - https://stackoverflow.com/questions/4745988/how-do-i-detect-if-software-keyboard-is-visible-on-android-device
                //       - https://groups.google.com/forum/#!topic/android-platform/FyjybyM0wGA
                post(new Runnable(){
                    @Override
                    public void run() {
                        Log.d(TAG, "updateSectionDataAndUi(getScrollY(), getScrollY(), true)");

                        // Size of our container (ScrollView) has changed.
                        // Reset UI = recalculate section data and update UI of headers and footers.
                        updateSectionDataAndUi(getScrollY(), getScrollY(), true);

                        if (activeSectionAfterScroll != null) {
                            // Ensure ensure that previously active section is fully visibile.
                            // If part of the section is hidden then we need to scroll the viewport.
                            boolean scrollNeeded = scrollToSection(activeSectionAfterScroll);
                            if (!scrollNeeded) {
                                resetImplicitScrollParams();
                            }
                        }
                    }
                });
            }
        };
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
            initSectionDataForView(child);
        }
    }

    @Override
    public void addView(View child, int index) {
        if (inflateFinished) {
            super.addView(child, index);
        } else {
            formLayout.addView(child, index);
            initSectionDataForView(child);
        }
    }

    @Override
    public void addView(View child, ViewGroup.LayoutParams params) {
        if (inflateFinished) {
            super.addView(child, params);
        } else {
            formLayout.addView(child, params);
            initSectionDataForView(child);
        }
    }

    @Override
    public void addView(View child, int width, int height) {
        if (inflateFinished) {
            super.addView(child, width, height);
        } else {
            formLayout.addView(child, width, height);
            initSectionDataForView(child);
        }
    }

    /**
     * Initializes section data for child view if child's
     * <code>sectionHeader</code> attribute is set <code>true</code>.
     * View's without <code>sectionHeader</code> attribute are ignored.
     * @param child View for which <code>SectionData</code> should be initialized.
     */
    private void initSectionDataForView(View child) {

        ViewGroup.LayoutParams layoutParams = child.getLayoutParams();
        if (layoutParams instanceof StickyLinearLayout.LayoutParams) {

            boolean sectionHeader = ((LayoutParams) layoutParams).sectionHeader;

            // Use global/default margin if
            // component does not define one itself.
            int scrollToSectionMargin = ((LayoutParams) layoutParams).scrollToSectionMargin;
            if (scrollToSectionMargin < 0) {
                scrollToSectionMargin = defaultScrollToSectionMargin;
            }

            if (sectionHeader) {
                SectionData sectionData = new SectionData(child, scrollToSectionMargin);
                View formView = sectionData.getUnpinnedHeader();

                // Get header view instance from callback method
                if(this.onCreateHeaderMethod != null){
                    Object result = this.onCreateHeaderMethod.invoke(formView);
                    if(result instanceof  View){
                        sectionData.setPinnedUpHeader((View) result);
                    } else {
                        throw new RuntimeException("Method invocation of onCreateHeaderMethod did not return new view");
                    }
                }

                // Get footer view instance from callback method
                if(this.onCreateFooterMethod != null){
                    Object result = this.onCreateFooterMethod.invoke(formView);
                    if(result instanceof  View){
                        sectionData.setPinnedDownHeader((View)result);
                    } else {
                        throw new RuntimeException("Method invocation of onCreateFooterMethod did not return new view");
                    }
                }

                // Configure on click listeners which
                // scrolls the form in a way that section
                // under the header/footer will be visible.
                setHeaderOnClickListener(sectionData.getPinnedUpHeader(), sectionData);
                setHeaderOnClickListener(sectionData.getUnpinnedHeader(), sectionData);
                setHeaderOnClickListener(sectionData.getPinnedDownHeader(), sectionData);

                sectionsData.add(sectionData);
            }
        }
    }

    /**
     * Sets click listener for <code>pinnedView</code> which scrolls the form
     * in a way that section under the header/footer will be visible.
     */
    protected void setHeaderOnClickListener(View sectionHeader, final SectionData sectionData) {
        sectionHeader.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                setActiveSectionAfterScroll(sectionData);
            }
        });
    }

    private boolean scrollToSection(SectionData sectionData) {

        // Calculate header height at time user scrolls the
        // form in a such way that <code>formView</code> is
        // the first visible component of the form.
        // We can rely on the fact that <code>sectionsData</code>
        // are ordered the same way as views are shown in the header/footer.
        int headerHeigthAfterScroll = 0;
        int scrollToSectionMargin = 0;
        for (SectionData data : sectionsData) {
            // We are done if we reached the view on which user clicked
            if (data == sectionData) {
                scrollToSectionMargin = sectionData.getScrollToSectionMargin();
                break;
            }
            headerHeigthAfterScroll += data.getUnpinnedHeader().getHeight();
        }

        // Location where we should first
        // scroll the form in order to make the section active
        // (Y value of section header)
        int sectionY =
                (int)sectionData.getUnpinnedHeader().getY()
                - headerHeigthAfterScroll
                - scrollToSectionMargin;

        // First check if any scroll is needed or possible.
        // We might already be scrolled at given position or
        // no scroll is posible (we are at the very top/bottom).
        int currentScrollY = getFormLayoutScrollView().getScrollY();
        if (sectionY > currentScrollY)
        {   // should scroll down
            boolean alreadyScrolledToBottom = currentScrollY >= getMaxScrollY();
            if (alreadyScrolledToBottom) {
                return false;
            }
        } else if (sectionY < currentScrollY)
        {   // should scroll up
            boolean alreadyScrolledToTop = currentScrollY <= getMinScrollY();
            if (alreadyScrolledToTop) {
                return false;
            }
        } else {
            // no scroll needed, position is already OK
            return false;
        }

        // Some scroll is needed before making the section active
        this.implicitScrollToY = sectionY;
        this.formLayoutScrollView.smoothScrollTo(0, implicitScrollToY);
        return true;
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
    protected StickyLinearLayout.LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    @Override
    public StickyLinearLayout.LayoutParams generateLayoutParams(AttributeSet attributeSet) {
        return new LayoutParams(getContext(), attributeSet);
    }

    @Override
    protected StickyLinearLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    public void updateViewLayout(View view, ViewGroup.LayoutParams params) {
        super.updateViewLayout(view, params);
    }

    @Override
    public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

        // We encountered a case that this method had been
        // called multiple times with the same attributes.
        // Skip any duplicate call.
        if (scrollX == previousScrollX && scrollY == previousScrollY) {
            return;
        }
        previousScrollX = scrollX;
        previousScrollY = scrollY;

        Log.d(TAG, "onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY)");

        updateSectionDataAndUi(scrollY, oldScrollY, false);
    }

    protected void updateSectionDataAndUi(int scrollY, int oldScrollY, boolean forceUpdateUi) {

        // Calculate current header size and footer size from sectionsData,
        // headerLayout.getHeight can't be used due inconsistent data after addView when swipe is used
        int headerSizeByData = 0;
        int footerSizeByData = 0;
        for (SectionData data : sectionsData) {
            if (data.getHeaderState() == SectionData.HeaderState.PINNED_UP) {
                headerSizeByData += data.getPinnedUpHeader().getHeight();
            } else if (data.getHeaderState() == SectionData.HeaderState.PINNED_DOWN) {
                footerSizeByData += data.getPinnedDownHeader().getHeight();
            }
        }
        int headerScrollSizeByData = headerSizeByData > maxHeaderRows*99 // TODO remove the 99 with calculated value
                ? maxHeaderRows*99 // TODO remove the 99 with calculated value
                : headerSizeByData;
        int footerScrollSizeByData = footerSizeByData > maxFooterRows*99
//                ? maxFooterRows*99
                ? footerSizeByData
                : footerSizeByData;

        // Update header views data - detect which headers are
        // hidden (PINNED_UP/PINNED_DOWN) and which are visible (UNPINNED)
        boolean updateUi = false;
        for (SectionData data : sectionsData) {
            float viewY = data.getUnpinnedHeader().getY();
            if (scrollY + headerScrollSizeByData > viewY) {
                updateUi |= data.update(SectionData.HeaderState.PINNED_UP);
            } else if (scrollY + formLayoutScrollView.getHeight() - footerScrollSizeByData < viewY + (data.getHeaderState() == SectionData.HeaderState.PINNED_DOWN ? 0 : data.getUnpinnedHeader().getHeight())) {
                updateUi |= data.update(SectionData.HeaderState.PINNED_DOWN);
            } else {
                updateUi |= data.update(SectionData.HeaderState.UNPINNED);
            }
        }

        // Update UI according to previously calculated SectionData
        if (updateUi || forceUpdateUi) {
            int pinnedDownIndex = 0;
            for (int i = 0; i < sectionsData.size(); i++) {
                SectionData section = sectionsData.get(i);

                if (section.isStateUpdated() || forceUpdateUi) {

                    // Ensure child is removed from parent
                    headerLayout.removeView(section.getPinnedUpHeader());
                    footerLayout.removeView(section.getPinnedDownHeader());

                    switch (section.getHeaderState()) {
                        case PINNED_UP:
                            headerLayout.addView(section.getPinnedUpHeader());
                            break;
                        case PINNED_DOWN:
                            footerLayout.addView(section.getPinnedDownHeader(), pinnedDownIndex);
                            pinnedDownIndex++;
                            break;
                        case UNPINNED:
                            // nothing to do with header/footer container
                            break;
                    }
                }
            }
        }

        // Sync header scroll position in a way that header view
        // is in sync with the very same form view.
        if (headerLayout.getChildCount() > 0) {




            View last = headerLayout.getChildAt(headerLayout.getChildCount()-1);
            SectionData section = (SectionData)last.getTag();

            Log.d(StickyLinearLayout.class.getSimpleName(),
                    String.format("@@@ unpinned [y = %s]",
                            (section == null ? null : section.getUnpinnedHeader().getY())));
            Log.d(StickyLinearLayout.class.getSimpleName(),
                    String.format("@@@ pinned [y = %s]",
                            (section == null ? null : section.getPinnedUpHeader().getY() - headerScrollView.getScrollY() + scrollY)));

            int newScroll = (int)
                    (scrollY
                    +section.getPinnedUpHeader().getY()
                    -section.getUnpinnedHeader().getY());

            Log.d(StickyLinearLayout.class.getSimpleName(),
                    String.format("@@@ newScroll [scrollY = %s]", newScroll));

            headerScrollView.scrollTo(0, newScroll);
        }

        // Detect if 'active section' has changed.
        // If so then notify listener registered layout XML file.
        for (SectionData section : sectionsData) {
            if (onActiveSectionChangedMethod != null) {
                if (isScrollImplicit() && isImplicitScrollFinished(scrollY, oldScrollY)) {
                    setActiveSection(activeSectionAfterScroll);
                    resetImplicitScrollParams();
                    break;
                } else if (!isScrollImplicit() &&
                        section.getHeaderState() == SectionData.HeaderState.UNPINNED) {
                    setActiveSection(section); // This calls onActiveSectionChanged(...) callback
                    break;
                }

            }
        }
    }

    private void resetImplicitScrollParams() {

        Log.d(TAG, "resetImplicitScrollParams()");

        activeSectionAfterScroll = null;
        implicitScrollToY = -1;
    }

    /**
     * @return <code>true</code> if
     * the scroll is caused implicitly by user clicking
     * on header or by view (section) focus change.
     * Scroll did not occur because user explicitly scrolled the ScrollView.
     */
    private boolean isScrollImplicit() {
        return activeSectionAfterScroll != null;
    }

    private boolean isImplicitScrollFinished(int scrollY, int oldScrollY) {
        if (scrollY < oldScrollY) {
            // scrolling up
            return
                scrollY <= getMinScrollY() ||
                scrollY == implicitScrollToY;
        } else {
            // scrolling down
            return
                scrollY >= getMaxScrollY() ||
                scrollY == implicitScrollToY;
        }
    }

    private int getMinScrollY() {
        return 0;
    }

    private int getMaxScrollY() {
        return getFormLayout().getHeight() - getFormLayoutScrollView().getHeight();
    }

    private void setActiveSectionAfterScroll(SectionData section) {
        this.activeSectionAfterScroll = section;
        boolean scrollTriggered = scrollToSection(section);
        if (!scrollTriggered) {
            resetImplicitScrollParams();
            setActiveSection(section);
        }
    }

    /**
     * <p>
     * Marks the provided <code>section</code> as active and calls
     * <code>onActiveSectionChanged()</code> callback method
     * registered/specified in layout XML file.
     * </p><p>
     * Does nothing if provided <code>section</code> is already active
     * (see {@link #isSectionActive(SectionData)}).
     * </p>
     * @param section Section to be set active
     */
    private void setActiveSection(SectionData section) {

        // Ignore if section is already active
        if (isSectionActive(section)) return;

        int newIndex = sectionsData.indexOf(section);
        int previousIndex = activeSectionIndex;

        activeSection = section;
        activeSectionIndex = newIndex;

        onActiveSectionChangedMethod.invoke(sectionsData, newIndex, previousIndex);
    }

    private boolean isSectionActive(SectionData section) {
        return activeSection == section;
    }

    public List<SectionData> getSectionsData() {
        return sectionsData;
    }

    private static class LayoutParams extends LinearLayout.LayoutParams {

        private boolean sectionHeader = false;

        private int scrollToSectionMargin = -1;

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
            TypedArray typedArray = c.obtainStyledAttributes(attrs, R.styleable.StickyLinearLayoutParams);
            try {
                this.sectionHeader = typedArray.getBoolean(R.styleable.StickyLinearLayoutParams_sectionHeader, false);
                this.scrollToSectionMargin = typedArray.getDimensionPixelSize(R.styleable.StickyLinearLayoutParams_scrollToSectionMargin, -1);
            } finally {
                typedArray.recycle();
            }
        }

        public boolean isSectionHeader() {
            return sectionHeader;
        }

        public int getScrollToSectionMargin() {
            return scrollToSectionMargin;
        }

    }

    private static class MethodWithContext {

        private Method method;
        private Context context;

        public MethodWithContext(Method method, Context context) {
            this.method = method;
            this.context = context;
        }

        private Object invoke(Object... args) {
            try {
                return method.invoke(context, args);
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

    @Override
    public void onGlobalFocusChanged(View oldFocus, View newFocus) {

        // Traverse view hierarchy down to 'formLayout'
        // to find View which is a direct child of 'formLayout'.
        // This way we detect if 'newFocus' is part of 'formLayout'
        // and also we detect a View/ViewGroup instance to which 'newFocus'
        // belongs to.
        View child = newFocus;
        for (;;) {
            if (child == null || child.getParent() == getFormLayout()) {
                break;
            }
            if (child.getParent() instanceof View) {
                child = (View) child.getParent();
            } else {
                // We are probably in different hierarchy (not a form layout)
                return;
            }
        }

        if (child != null && child.getParent() == getFormLayout()) {
            // At this point
            // - 'newFocus' is from form layout
            // - 'child' is direct child of 'formLayout'

            // Detect section to which 'child' belongs by traversing
            // 'formLayout' upwards ('formLayout' is LinearLayout therefore
            // we can traverse its children by index until child with
            // 'sectionHeader' attribute is found).

            View sectionFormView = null;
            int viewIndex = getFormLayout().indexOfChild(child);
            for (int i = viewIndex-1; i>=0; i--) {
                child = getFormLayout().getChildAt(i);
                LayoutParams layoutParams = (LayoutParams) child.getLayoutParams();
                if (layoutParams.isSectionHeader()) {
                    sectionFormView = child;
                    break;
                }
            }

            if (sectionFormView != null) {
                for (SectionData sectionData : sectionsData) {
                    if (sectionData.getUnpinnedHeader() == sectionFormView) {
                        setActiveSectionAfterScroll(sectionData);
                    }
                }
            }
        }
    }

}
