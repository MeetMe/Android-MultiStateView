package com.meetme.android.multistateview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * A view designed to wrap a single child (the "content") and hide/show that content based on the current "state" (see {@link ContentState}) of this
 * View. Note that this layout can only have one direct descendant which is used as the "content" view
 */
public class MultiStateView extends FrameLayout {
    private MultiStateViewData mViewState = new MultiStateViewData(ContentState.CONTENT);

    private View mContentView;
    private View mLoadingView;
    private View mNetworkErrorView;
    private View mGeneralErrorView;
    private OnClickListener mTapToRetryClickListener;

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
            setErrorUnknownLayoutResourceId(a.getResourceId(R.styleable.MultiStateView_msvErrorUnknownLayout, R.layout.msv__error_unknown));
            setErrorNetworkLayoutResourceId(a.getResourceId(R.styleable.MultiStateView_msvErrorNetworkLayout, R.layout.msv__error_network));

            String tmpString;

            tmpString = a.getString(R.styleable.MultiStateView_msvErrorTitleNetworkStringId);

            if (tmpString == null) {
                tmpString = context.getString(R.string.error_title_network);
            }

            setErrorNetworkTitleString(tmpString);

            tmpString = a.getString(R.styleable.MultiStateView_msvErrorTitleUnknownStringId);

            if (tmpString == null) {
                tmpString = context.getString(R.string.error_title_unknown);
            }

            setErrorUnknownTitleString(tmpString);

            tmpString = a.getString(R.styleable.MultiStateView_msvErrorTapToRetryStringId);

            if (tmpString == null) {
                tmpString = context.getString(R.string.tap_to_retry);
            }

            setTapToRetryString(tmpString);

            setState(a.getInt(R.styleable.MultiStateView_msvState, ContentState.CONTENT.nativeInt));
        } finally {
            a.recycle();
        }
    }

    private void setErrorNetworkLayoutResourceId(int resourceId) {
        mViewState.networkErrorLayoutResId = resourceId;
    }

    private void setErrorUnknownLayoutResourceId(int resourceId) {
        mViewState.generalErrorLayoutResId = resourceId;
    }

    private void setErrorNetworkTitleString(String string) {
        mViewState.networkErrorTitleString = string;
    }

    public String getNetworkErrorTitleString() {
        return mViewState.networkErrorTitleString;
    }

    private void setErrorUnknownTitleString(String string) {
        mViewState.generalErrorTitleString = string;
    }

    public void setCustomErrorString(String string) {
        mViewState.customErrorString = string;

        if (mGeneralErrorView != null) {
            TextView view = ((TextView) mGeneralErrorView.findViewById(R.id.error_title));

            if (view != null) {
                view.setText(string);
            }
        }
    }

    public String getGeneralErrorTitleString() {
        return mViewState.generalErrorTitleString;
    }

    private void setTapToRetryString(String string) {
        mViewState.tapToRetryString = string;
    }

    public String getTapToRetryString() {
        return mViewState.tapToRetryString;
    }

    public int getLoadingLayoutResourceId() {
        return mViewState.loadingLayoutResId;
    }

    public void setLoadingLayoutResourceId(int loadingLayout) {
        this.mViewState.loadingLayoutResId = loadingLayout;
    }

    /**
     * @return the {@link ContentState} the view is currently in
     */
    public ContentState getState() {
        return mViewState.state != null ? mViewState.state : ContentState.CONTENT;
    }

    /**
     * Configures the view to be in the given state. This method is an internal method used for parsing the native integer value used in attributes in
     * XML
     *
     * @see ContentState
     * @see #setState(ContentState)
     * @param nativeInt
     */
    private void setState(int nativeInt) {
        setState(ContentState.getState(nativeInt));
    }

    /**
     * Configures the view to be in the given state, hiding and showing internally maintained-views as needed
     *
     * @param state
     */
    public void setState(final ContentState state) {
        if (state == mViewState.state) {
            // No change
            return;
        }

        final View contentView = getContentView();

        if (contentView == null) {
            return;
        }

        final ContentState previousState = mViewState.state;

        // Only change visibility after other UI tasks have been performed
        this.post(new Runnable() {
            @Override
            public void run() {
                Log.v("DALLAS", "Attempting to change state from " + previousState);
                // Hide the current state
                getStateView(previousState).setVisibility(View.GONE);
            }
        });

        mViewState.state = state;

        View newStateView = getStateView(state);

        if (newStateView != null) {
            if (state == ContentState.ERROR_GENERAL) {
                TextView view = ((TextView) newStateView.findViewById(R.id.error_title));
                if (view != null) {
                    view.setText(getGeneralErrorTitleString());
                }
            }

            newStateView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Returns the given view corresponding to the specified {@link ContentState}
     *
     * @param state
     * @return
     */
    public View getStateView(ContentState state) {
        switch (state) {
            case ERROR_NETWORK:
                return getNetworkErrorView();

            case ERROR_GENERAL:
                return getGeneralErrorView();

            case LOADING:
                return getLoadingView();

            case CONTENT:
                return getContentView();
        }

        return null;
    }

    /**
     * Returns the view to be displayed for the case of a network error
     *
     * @return
     */
    public View getNetworkErrorView() {
        if (mNetworkErrorView == null) {
            mNetworkErrorView = View.inflate(getContext(), mViewState.networkErrorLayoutResId, null);

            ((TextView) mNetworkErrorView.findViewById(R.id.error_title)).setText(getNetworkErrorTitleString());
            ((TextView) mNetworkErrorView.findViewById(R.id.tap_to_retry)).setText(getTapToRetryString());

            mNetworkErrorView.setOnClickListener(mTapToRetryClickListener);

            addView(mNetworkErrorView);
        }

        return mNetworkErrorView;
    }

    /**
     * Returns the view to be displayed for the case of an unknown error
     *
     * @return
     */
    public View getGeneralErrorView() {
        if (mGeneralErrorView == null) {
            mGeneralErrorView = View.inflate(getContext(), mViewState.generalErrorLayoutResId, null);

            ((TextView) mGeneralErrorView.findViewById(R.id.error_title)).setText(getGeneralErrorTitleString());
            ((TextView) mGeneralErrorView.findViewById(R.id.tap_to_retry)).setText(getTapToRetryString());

            mGeneralErrorView.setOnClickListener(mTapToRetryClickListener);

            addView(mGeneralErrorView);
        }

        return mGeneralErrorView;
    }

    /**
     * Builds the loading view if not currently built, and returns the view
     */
    public View getLoadingView() {
        if (mLoadingView == null) {
            mLoadingView = View.inflate(getContext(), mViewState.loadingLayoutResId, null);

            addView(mLoadingView);
        }

        return mLoadingView;
    }

    public void setOnTapToRetryClickListener(View.OnClickListener listener) {
        mTapToRetryClickListener = listener;

        if (mNetworkErrorView != null) {
            mNetworkErrorView.setOnClickListener(listener);
        }

        if (mGeneralErrorView != null) {
            mGeneralErrorView.setOnClickListener(listener);
        }
    }

    /**
     * Adds the given view as content, throwing an {@link IllegalStateException} if a content view is already set (this layout can only have one
     * direct descendant)
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

        setState(mViewState.state);
    }

    private boolean isViewInternal(View view) {
        return view == mNetworkErrorView || view == mGeneralErrorView || view == mLoadingView;
    }

    @Override
    public void addView(View child) {
        if (!isViewInternal(child)) {
            addContentView(child);
        }

        super.addView(child);
    }

    @Override
    public void addView(View child, int index) {
        if (!isViewInternal(child)) {
            addContentView(child);
        }

        super.addView(child, index);
    }

    @Override
    public void addView(View child, int index, android.view.ViewGroup.LayoutParams params) {
        if (!isViewInternal(child)) {
            addContentView(child);
        }

        super.addView(child, index, params);
    }

    @Override
    public void addView(View child, int width, int height) {
        if (!isViewInternal(child)) {
            addContentView(child);
        }

        super.addView(child, width, height);
    }

    @Override
    public void addView(View child, android.view.ViewGroup.LayoutParams params) {
        if (!isViewInternal(child)) {
            addContentView(child);
        }

        super.addView(child, params);
    }

    /**
     * States of the MultiStateView
     */
    public static enum ContentState {
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
        LOADING(0x01),
        /**
         * Used to indicate that the Network Error indication should be displayed to the user
         *
         * @see R.attr#msvState
         */
        ERROR_NETWORK(0x02),
        /**
         * Used to indicate that the Unknown Error indication should be displayed to the user
         *
         * @see R.attr#msvState
         */
        ERROR_GENERAL(0x03);

        public final int nativeInt;
        private final static SparseArray<ContentState> sStates = new SparseArray<ContentState>();

        static {
            for (ContentState scaleType : values()) {
                sStates.put(scaleType.nativeInt, scaleType);
            }
        }

        public static ContentState getState(int nativeInt) {
            if (nativeInt >= 0) {
                return sStates.get(nativeInt);
            }

            return null;
        }

        private ContentState(int nativeValue) {
            this.nativeInt = nativeValue;
        }
    }

    public static class MultiStateViewData {
        public String customErrorString;
        public int loadingLayoutResId;
        public int generalErrorLayoutResId;
        public int networkErrorLayoutResId;
        public String networkErrorTitleString;
        public String generalErrorTitleString;
        public String tapToRetryString;
        public ContentState state;

        public MultiStateViewData(ContentState mState) {
            this.state = mState;
        }
    }
}
