package de.florianm.android.expandablelayout;

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
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.LinearLayout;

/**
 * An View that can expand and collapse its content when the header was clicked.
 */
public class ExpandableView extends LinearLayout {
    private int headerLayoutId;
    private int contentLayoutId;

    private ViewGroup headerContainer;
    private ViewGroup contentContainer;

    private View headerView;
    private View contentView;

    private boolean isOpen = false;
    private boolean isAnimationRunning = false;

    private ExpandListener expandListener;

    public ExpandableView(Context context) {
        super(context);
        initView(context);
    }

    public ExpandableView(Context context, AttributeSet attrs) {
        super(context, attrs);
        readAttributes(context, attrs, 0, 0);
        initView(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    public ExpandableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        readAttributes(context, attrs, defStyleAttr, 0);
        initView(context);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ExpandableView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        readAttributes(context, attrs, defStyleAttr, defStyleRes);
        initView(context);
    }

    /**
     * Read XML attributes. Must be called before {@link #initView(Context)}.
     *
     * @param context      The context that created this View.
     * @param attrs        The XML attributes set.
     * @param defStyleAttr The theme attribute that hold the default style for this View.
     * @param defStyleRes  The default style resource for this View.
     */
    private void readAttributes(Context context, AttributeSet attrs, @AttrRes int defStyleAttr, @StyleRes int defStyleRes) {
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ExpandableView, defStyleAttr, defStyleRes);
        try {
            final int N = a.getIndexCount();
            for (int i = 0; i < N; i++) {
                int attr = a.getIndex(i);
                if (R.styleable.ExpandableView_el_headerLayout == attr) {
                    headerLayoutId = a.getResourceId(attr, 0);
                } else if (R.styleable.ExpandableView_el_contentLayout == attr) {
                    contentLayoutId = a.getResourceId(attr, 0);
                } else if (R.styleable.ExpandableView_el_initialState == attr) {
                    isOpen = 0 != a.getInteger(attr, 0);
                }
            }
        } finally {
            a.recycle();
        }
    }

    /**
     * Initialize the View and inflate all needed view.
     *
     * @param context The context that created this View.
     */
    private void initView(Context context) {
        View root = View.inflate(context, R.layout.view_expandable_layout, this);

        headerContainer = (ViewGroup) root.findViewById(R.id.container_header);
        contentContainer = (ViewGroup) root.findViewById(R.id.container_content);

        if (0 != headerLayoutId) {
            headerView = View.inflate(context, headerLayoutId, null);
            setHeaderView(headerView);
        }

        if (0 != contentLayoutId) {
            contentView = View.inflate(context, contentLayoutId, null);
            setContentVieW(contentView);
        }

        if (isOpen) {
            expandContentInstantly();
        } else {
            collapseContentInstantly();
        }
    }

    /**
     * Set the View that should be used as header and expand/collapsed trigger when clicked.
     *
     * @param view The View that should be used as header.
     */
    public void setHeaderView(View view) {
        if (0 < headerContainer.getChildCount()) {
            headerContainer.removeAllViewsInLayout();
        }

        headerView = view;
        if (null != this.headerView) {
            headerView.setLayoutParams(
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );

            headerView.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    onHeaderViewClicked();
                }
            });
            headerView.setSelected(isOpen);

            headerContainer.addView(headerView);
        }
    }

    /**
     * Callback method that will be called if the header was clicked.
     */
    private void onHeaderViewClicked() {
        if (!isAnimationRunning) {
            if (isOpen) {
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

        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.expandable_layout_show);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                contentContainer.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimationRunning = false;
                onContentExpanded();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        contentContainer.startAnimation(animation);
    }

    /**
     * Collapse the content with an animation.
     */
    private void collapseContentAnimated() {
        isAnimationRunning = true;

        Animation animation = AnimationUtils.loadAnimation(getContext(), R.anim.expandable_layout_hide);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isAnimationRunning = false;
                onContentCollapsed();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        contentContainer.startAnimation(animation);
    }

    /**
     * Set the content that can be expanded and collapsed.
     *
     * @param view The view that should be used as collapsable content.
     */
    public void setContentVieW(View view) {
        if (0 < contentContainer.getChildCount()) {
            contentContainer.removeAllViewsInLayout();
        }

        contentView = view;
        if (null != view) {
            contentView.setLayoutParams(
                    new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                    )
            );
            contentContainer.addView(contentView);
        }
    }

    /**
     * Callback method that will be called of the content is fully expanded.
     */
    private void onContentExpanded() {
        isOpen = true;
        headerView.setSelected(true);

        if (null != expandListener) {
            expandListener.onExpanded();
        }
    }

    /**
     * Callback method that will be called of the content is fully collapsed.
     */
    private void onContentCollapsed() {
        isOpen = false;
        headerView.setSelected(false);
        contentContainer.setVisibility(View.GONE);

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
     * @param expandListener The listener.
     */
    public void setExpandListener(ExpandListener expandListener) {
        this.expandListener = expandListener;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState savedState = new SavedState(superState);
        savedState.setOpen(isOpen);
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

        isOpen = ss.isOpen();
        if (isOpen) {
            expandContentInstantly();
        } else {
            collapseContentInstantly();
        }
    }

    /**
     * Expand the content instantly without any animation.
     */
    private void expandContentInstantly() {
        contentContainer.setScaleY(1.0f);
        contentContainer.setAlpha(1.0f);
        contentContainer.setVisibility(View.VISIBLE);
        onContentExpanded();
    }

    /**
     * Collapse the content instantly without any animation.
     */
    private void collapseContentInstantly() {
        contentContainer.setScaleY(0.0f);
        contentContainer.setAlpha(0.0f);
        contentContainer.setVisibility(View.GONE);
        onContentCollapsed();
    }

    /**
     * User interface state that is stored by {@link ExpandableView} for implementing
     * {@link View#onSaveInstanceState}.
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
}
