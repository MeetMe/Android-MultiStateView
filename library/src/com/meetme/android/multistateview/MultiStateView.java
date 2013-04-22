package com.meetme.android.multistateview;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

/**
 * A view designed to wrap a single child (the "content") and hide/show that content based on the current "state" (see {@link State}) of this View.
 * Note that this layout can only have one direct descendant which is used as the "content" view
 */
public class MultiStateView extends FrameLayout {

    private int mLoadingLayoutResId;
    private State mState;
    private View mContentView;
    private View mLoadingView;

    public MultiStateView(Context context) {
        super(context);
        parseAttrs(context, null);
    }

    public MultiStateView(Context context, AttributeSet attrs) {
        super(context, attrs);
        parseAttrs(context, attrs);
    }

    public MultiStateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        parseAttrs(context, attrs);
    }

    /**
     * Parses the incoming attributes from XML inflation
     *
     * @param context
     * @param attrs
     */
    private void parseAttrs(Context context, AttributeSet attrs) {
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.MultiStateView, 0, 0);

        try {
            setLoadingLayoutResourceId(a.getResourceId(R.styleable.MultiStateView_msvLoadingLayout, R.layout.msv__loading));
            setState(a.getInt(R.styleable.MultiStateView_msvState, State.CONTENT.nativeInt));
        } finally {
            a.recycle();
        }
    }

    public int getLoadingLayoutResourceId() {
        return mLoadingLayoutResId;
    }

    public void setLoadingLayoutResourceId(int loadingLayout) {
        this.mLoadingLayoutResId = loadingLayout;
    }

    /**
     * @return the {@link State} the view is currently in
     */
    public State getState() {
        return mState != null ? mState : State.CONTENT;
    }

    /**
     * Configures the view to be in the given state. This method is an internal method used for parsing the native integer value used in attributes in
     * XML
     *
     * @see State
     * @see #setState(State)
     * @param nativeInt
     */
    private void setState(int nativeInt) {
        setState(State.getState(nativeInt));
    }

    /**
     * Configures the view to be in the given state, hiding and showing internally maintained-views as needed
     *
     * @param state
     */
    public void setState(final State state) {
        mState = state;

        final View contentView = getContentView();

        if (contentView == null) {
            return;
        }

        switch (getState()) {
            case LOADING:
                contentView.setVisibility(View.GONE);
                getLoadingView().setVisibility(View.VISIBLE);
                break;

            default:
            case CONTENT:
                contentView.setVisibility(View.VISIBLE);
                getLoadingView().setVisibility(View.GONE);
                break;
        }
    }

    /**
     * Builds the loading view if not currently built, and returns the view
     */
    public View getLoadingView() {
        if (mLoadingView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            mLoadingView = inflater.inflate(mLoadingLayoutResId, null);

            addView(mLoadingView);
        }

        return mLoadingView;
    }

    /**
     * Adds the given view as content, throwing an {@link IllegalStateException} if a content view is already set (this layout can only have on direct
     * descendant)
     *
     * @param contentView
     */
    private void addContentView(View contentView) {
        if (mContentView != null && mContentView != contentView) {
            throw new IllegalStateException("Can't add more than one view to MultiStateView");
        }

        setContentView(contentView);
    }

    /**
     * @return the view being used as "content" within the view (the developer-provided content -- doesn't ever give back internally maintained views
     * (like the loading layout))
     */
    public View getContentView() {
        return mContentView;
    }

    /**
     * Sets the content view of this view. This does nothing to eradicate the inflated or any pre-existing descendant
     *
     * @param contentView
     */
    public void setContentView(View contentView) {
        mContentView = contentView;

        setState(mState);
    }

    @Override
    public void addView(View child) {
        if (!isInternalView(child)) {
            addContentView(child);
        }

        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (!isInternalView(child)) {
            addContentView(child);
        }

        super.addView(child, index);
    }

    private boolean isInternalView(View child) {
        return mLoadingView == child;
    }

    @Override
    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        if (!isInternalView(child)) {
            addContentView(child);
        }

        super.addView(child, index, params);
    }

    @Override
    public void addView(View child, int width, int height) {
        if (!isInternalView(child)) {
            addContentView(child);
        }

        super.addView(child, width, height);
    }

    @Override
    public void addView(View child, android.view.ViewGroup.LayoutParams params) {
        if (!isInternalView(child)) {
            addContentView(child);
        }

        super.addView(child, params);
    }

    /**
     * States of the MultiStateView
     */
    public static enum State {
        /**
         * Used to indicate that content should be displayed to the user
         *
         * @see R.attr#msvState
         */
        CONTENT(0x00),
        /**
         * Used to indicate that the Loading indication should be displayed to the user
         *
         * @see R.attr#msvState
         */
        LOADING(0x01);

        public final int nativeInt;
        private final static SparseArray<State> sStates = new SparseArray<State>();

        static {
            for (State scaleType : values()) {
                sStates.put(scaleType.nativeInt, scaleType);
            }
        }

        public static State getState(int nativeInt) {
            if (nativeInt >= 0) {
                return sStates.get(nativeInt);
            }

            return null;
        }

        private State(int nativeValue) {
            this.nativeInt = nativeValue;
        }
    }

}
