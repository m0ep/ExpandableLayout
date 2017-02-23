package de.florianm.android.expandablelayout;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.AttrRes;
import android.support.annotation.RequiresApi;
import android.support.annotation.StyleRes;
import android.util.AttributeSet;
import android.view.View;
import android.widget.LinearLayout;

/**
 * An View that can expand and collapse its content when the header was clicked.
 */
public class ExpandableLayout extends LinearLayout {
    private int headerId;
    private int contentId;

    private View headerView;
    private View contentView;

    private boolean isInitialOpen = false;
    private boolean isAnimationRunning = false;

    private ExpandListener expandListener;

    private int animationDuration = 400;

    public ExpandableLayout(Context context) {
        super(context);
        initView(context);
    }

    public ExpandableLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        readAttributes(context, attrs, 0, 0);
        initView(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public ExpandableLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readAttributes(context, attrs, defStyleAttr, 0);
        initView(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ExpandableLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        readAttributes(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    /**
     * Read XML attributes. Must be called before {@link #initView(Context)}.
     *
     * @param context
     *         The context that created this View.
     * @param attrs
     *         The XML attributes set.
     * @param defStyleAttr
     *         The theme attribute that hold the default style for this View.
     * @param defStyleRes
     *         The default style resource for this View.
     */
    private void readAttributes(Context context, AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandableLayout, defStyleAttr, defStyleRes);
        try {
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (R.styleable.ExpandableLayout_el_headerId == attr) {
                    headerId = a.getResourceId(attr, 0);
                } else if (R.styleable.ExpandableLayout_el_contentId == attr) {
                    contentId = a.getResourceId(attr, 0);
                } else if (R.styleable.ExpandableLayout_el_initialState == attr) {
                    isInitialOpen = 0 != a.getInteger(attr, 0);
                }
            }
        } finally {
            a.recycle();
        }

        if (0 == headerId) {
            throw new IllegalArgumentException("The attribute 'el_headerId' is mandatory for this layout");
        }

        if (0 == contentId) {
            throw new IllegalArgumentException("The attribute 'el_contentId' is mandatory for this layout");
        }
    }

    /**
     * Initialize the View and inflate all needed view.
     *
     * @param context
     *         The context that created this View.
     */
    private void initView(Context context) {
        setOrientation(VERTICAL);

        animationDuration = getResources().getInteger(android.R.integer.config_shortAnimTime);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();

        View view = findViewById(headerId);
        if (null == view) {
            throw new IllegalArgumentException("No view found with that header id");
        }
        setHeaderView(view);

        view = findViewById(contentId);
        if (null == view) {
            throw new IllegalArgumentException("No view found with that header id");
        }
        setContentVieW(view);

        if (isInitialOpen) {
            expandContentInstantly();
        } else {
            collapseContentInstantly();
        }
    }

    /**
     * Set the View that should be used as header and expand/collapsed trigger when clicked.
     *
     * @param view
     *         The View that should be used as header.
     */
    private void setHeaderView(View view) {
        headerView = view;
        headerView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onHeaderViewClicked();
            }
        });
        headerView.setSelected(isInitialOpen);
    }

    /**
     * Callback method that will be called if the header was clicked.
     */
    private void onHeaderViewClicked() {
        if (!isAnimationRunning) {
            if (View.VISIBLE == contentView.getVisibility()) {
                //collapseContentInstantly();
                collapseContentAnimated();
            } else {
                //expandContentInstantly();
                expandContentAnimated();
            }
        }
    }

    /**
     * Expand the content with an animation.
     */
    private void expandContentAnimated() {
        isAnimationRunning = true;

        int widthSpec = MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.UNSPECIFIED);

        contentView.measure(widthSpec, heightSpec);
        final int targetHeight = contentView.getMeasuredHeight();

        contentView.getLayoutParams().height = 0;
        contentView.setVisibility(View.VISIBLE);

        ValueAnimator valueAnimator = ValueAnimator.ofInt(0, targetHeight);
        valueAnimator.setDuration(animationDuration);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                contentView.getLayoutParams().height = (int) animation.getAnimatedValue();
                contentView.requestLayout();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                isAnimationRunning = false;
                onContentExpanded();
            }
        });
        valueAnimator.start();
    }

    /**
     * Collapse the content with an animation.
     */
    private void collapseContentAnimated() {
        isAnimationRunning = true;

        final int initialHeight = contentView.getMeasuredHeight();
        ValueAnimator valueAnimator = ValueAnimator.ofInt(initialHeight, 0);
        valueAnimator.setDuration(animationDuration);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                contentView.getLayoutParams().height = (int) animation.getAnimatedValue();
                contentView.requestLayout();
            }
        });
        valueAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                contentView.setVisibility(View.GONE);
                isAnimationRunning = false;
                onContentCollapsed();
            }
        });
        valueAnimator.start();

    }

    /**
     * Set the content that can be expanded and collapsed.
     *
     * @param view
     *         The view that should be used as collapsable content.
     */
    private void setContentVieW(View view) {
        contentView = view;
    }

    /**
     * Callback method that will be called of the content is fully expanded.
     */
    private void onContentExpanded() {
        isInitialOpen = true;
        headerView.setSelected(true);

        if (null != expandListener) {
            expandListener.onExpanded();
        }
    }

    /**
     * Callback method that will be called of the content is fully collapsed.
     */
    private void onContentCollapsed() {
        isInitialOpen = false;
        headerView.setSelected(false);


        if (null != expandListener) {
            expandListener.onCollapsed();
        }
    }

    /**
     * Expand the content
     */
    public void expandContent() {
        expandContentAnimated();
    }

    /**
     * Collapse the content.
     */
    public void collapseContent() {
        collapseContentAnimated();
    }

    /**
     * Set a listener to get notified if the content was expanded or collapsed.
     *
     * @param expandListener
     *         The listener.
     */
    public void setExpandListener(ExpandListener expandListener) {
        this.expandListener = expandListener;
    }

    /**
     * Expand the content instantly without any animation.
     */
    private void expandContentInstantly() {
        contentView.setVisibility(View.VISIBLE);
        onContentExpanded();
    }

    /**
     * Collapse the content instantly without any animation.
     */
    private void collapseContentInstantly() {
        contentView.setVisibility(View.GONE);
        onContentCollapsed();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.setOpen(View.VISIBLE == contentView.getVisibility());
        return savedState;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());


        boolean isOpen = ss.isOpen();
        if (isOpen) {
            expandContentInstantly();
        } else {
            collapseContentInstantly();
        }
    }

    /**
     * Listener to get notified if the content was expanded or collapsed.
     */
    public interface ExpandListener {
        /**
         * Method that will be called if the collapsed content is fully expanded.
         */
        void onExpanded();

        /**
         * Method that will be called if the expanded content is fully collapsed.
         */
        void onCollapsed();
    }

    /**
     * User interface state that is stored by {@link ExpandableLayout} for implementing {@link
     * View#onSaveInstanceState}.
     */
    public static class SavedState extends BaseSavedState {

        @SuppressWarnings("hiding")
        public static final Parcelable.Creator<SavedState> CREATOR
                = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };

        private boolean isOpen;

        public SavedState(Parcel source) {
            super(source);

            isOpen = 0 != source.readInt();
        }

        public SavedState(Parcelable superState) {
            super(superState);
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(isOpen ? 1 : 0);
        }

        public void setOpen(boolean open) {
            isOpen = open;
        }

        public boolean isOpen() {
            return isOpen;
        }
    }
}
