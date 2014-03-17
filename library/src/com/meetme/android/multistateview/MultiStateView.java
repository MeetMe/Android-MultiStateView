package com.meetme.android.multistateview;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * A view designed to wrap a single child (the "content") and hide/show that content based on the current "state" (see {@link ContentState}) of this
 * View. Note that this layout can only have one direct descendant which is used as the "content" view
 */
public class MultiStateView extends FrameLayout {

    public static final int CONTENT_STATE_ID_CONTENT = 0;

    public static final int CONTENT_STATE_ID_LOADING = 1;

    public static final int CONTENT_STATE_ID_ERROR_NETWORK = 2;

    public static final int CONTENT_STATE_ID_ERROR_GENERAL = 3;

    public final static int MIN_CONTENT_STATE_ID = CONTENT_STATE_ID_ERROR_GENERAL + 1;

    private MultiStateViewData mViewState = new MultiStateViewData(CONTENT_STATE_ID_CONTENT);

    private View mContentView;

    private View mLoadingView;

    private View mNetworkErrorView;

    private View mGeneralErrorView;

    private OnClickListener mTapToRetryClickListener;

    private MultiStateHandler mHandler;

    private SparseArray<StateViewProvider> mProviders;

    private final StateViewProvider mBuiltinProvider = new StateViewProvider() {
        @Override
        public View onCreateStateView(Context context, ViewGroup container, int stateViewId) {
            switch (stateViewId) {
                case CONTENT_STATE_ID_ERROR_NETWORK:
                    return getNetworkErrorView();

                case CONTENT_STATE_ID_ERROR_GENERAL:
                    return getGeneralErrorView();

                case CONTENT_STATE_ID_LOADING:
                    return getLoadingView();

                case CONTENT_STATE_ID_CONTENT:
                    return getContentView();
            }

            return null;
        }

        @Override
        public void onBeforeViewShown(int stateViewId, View view) {
            if (stateViewId == CONTENT_STATE_ID_ERROR_GENERAL) {
                TextView textView = ((TextView) view.findViewById(R.id.error_title));

                if (textView != null) {
                    textView.setText(getGeneralErrorTitleString());
                }
            }
        }
    };

    private SparseArray<View> mStateViewCache = new SparseArray<View>();

    public static interface StateViewProvider<T extends View> {
        /**
         * Called when a View is needed for the given state, and no cached version exists
         *
         * @param context
         * @param container
         * @param stateViewId
         * @return
         */
        T onCreateStateView(Context context, ViewGroup container, int stateViewId);

        /**
         * Called just before the view is going to be shown to the user. Set any text or other things on the view at this point if variable
         *
         * @param stateViewId
         * @param view
         */
        void onBeforeViewShown(int stateViewId, T view);
    }


    /**
     * Registers the given provider for the given state id
     *
     * @param contentStateId
     * @param provider
     */
    public void registerStateViewProvider(int contentStateId, StateViewProvider provider) {
        mProviders.put(contentStateId, provider);
    }

    public MultiStateView(Context context) {
        this(context, null);
    }

    public MultiStateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiStateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // Start out with a default handler/looper
        mHandler = new MultiStateHandler();
        parseAttrs(context, attrs);
        initStateViewProvider();
    }

    private void initStateViewProvider() {
        mProviders = new SparseArray<StateViewProvider>(ContentState.values().length);

        for (ContentState value : ContentState.values()) {
            mProviders.put(value.nativeInt, mBuiltinProvider);
        }
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
            setGeneralErrorLayoutResourceId(a.getResourceId(R.styleable.MultiStateView_msvErrorUnknownLayout, R.layout.msv__error_unknown));
            setNetworkErrorLayoutResourceId(a.getResourceId(R.styleable.MultiStateView_msvErrorNetworkLayout, R.layout.msv__error_network));

            String tmpString;

            tmpString = a.getString(R.styleable.MultiStateView_msvErrorTitleNetworkStringId);

            if (tmpString == null) {
                tmpString = context.getString(R.string.error_title_network);
            }

            setNetworkErrorTitleString(tmpString);

            tmpString = a.getString(R.styleable.MultiStateView_msvErrorTitleUnknownStringId);

            if (tmpString == null) {
                tmpString = context.getString(R.string.error_title_unknown);
            }

            setGeneralErrorTitleString(tmpString);

            tmpString = a.getString(R.styleable.MultiStateView_msvErrorTapToRetryStringId);

            if (tmpString == null) {
                tmpString = context.getString(R.string.tap_to_retry);
            }

            setTapToRetryString(tmpString);

            setContentState(a.getInt(R.styleable.MultiStateView_msvState, ContentState.CONTENT.nativeInt));
        } finally {
            a.recycle();
        }
    }

    private void setNetworkErrorLayoutResourceId(int resourceId) {
        mViewState.networkErrorLayoutResId = resourceId;
    }

    private void setGeneralErrorLayoutResourceId(int resourceId) {
        mViewState.generalErrorLayoutResId = resourceId;
    }

    private void setNetworkErrorTitleString(String string) {
        mViewState.networkErrorTitleString = string;
    }

    public String getNetworkErrorTitleString() {
        return mViewState.networkErrorTitleString;
    }

    private void setGeneralErrorTitleString(String string) {
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

    public int getContentState() {
        return mViewState.state;
    }

    /**
     * This is a legacy method and is deprecated in favor of using the integer forms. If you're using custom states and you attempt to use this
     * method, an IllegalStateException will be thrown as custom states cannot be converted to non-existent enumerated values
     *
     * @return the {@link ContentState} the view is currently in
     * @deprecated
     */
    public ContentState getState() {
        if (mViewState.state < MIN_CONTENT_STATE_ID) {
            ContentState.getState(mViewState.state);
        }

        throw new IllegalStateException("Attempting to get a state for a custom state");
    }

    /**
     * Configures the view to be in the given state.
     *
     * @param state
     * @see #CONTENT_STATE_ID_CONTENT
     * @see #CONTENT_STATE_ID_ERROR_GENERAL
     * @see #CONTENT_STATE_ID_ERROR_NETWORK
     * @see #CONTENT_STATE_ID_LOADING
     * @see #registerStateViewProvider(int, com.meetme.android.multistateview.MultiStateView.StateViewProvider)
     */
    public void setContentState(int state) {
        if (state == mViewState.state) {
            // No change
            return;
        }

        final View contentView = getContentView();

        if (contentView == null) {
            return;
        }

        final int previousState = mViewState.state;

        // Remove any previously pending hide events for the to-be-shown state
        mHandler.removeMessages(MultiStateHandler.MESSAGE_HIDE_PREVIOUS);
        // Only change visibility after other UI tasks have been performed
        mHandler.sendMessage(mHandler.obtainMessage(MultiStateHandler.MESSAGE_HIDE_PREVIOUS, previousState, -1));

        mViewState.state = state;

        View newStateView = getStateView(state);

        if (newStateView != null) {
            if (newStateView.getParent() == null) {
                addView(newStateView);
            }

            mProviders.get(state).onBeforeViewShown(state, newStateView);
            newStateView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Configures the view to be in the given state, hiding and showing internally maintained-views as needed
     *
     * @param state
     * @deprecated
     */
    public void setState(final ContentState state) {
        setContentState(state.nativeInt);
    }

    private View getStateView(int stateViewId) {
        // Check if we have it cached first (we only need to create once per config)
        View view = mStateViewCache.get(stateViewId);

        if (view == null) {
            // Not cached, pull from the provider
            view = mProviders.get(stateViewId).onCreateStateView(getContext(), this, stateViewId);

            // And store in cache
            mStateViewCache.put(stateViewId, view);
        }

        return view;
    }

    /**
     * Returns the given view corresponding to the specified {@link ContentState}
     *
     * @param state
     * @return
     */
    public View getStateView(ContentState state) {
        return getStateView(state.nativeInt);
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
        }

        return mGeneralErrorView;
    }

    /**
     * Builds the loading view if not currently built, and returns the view
     */
    public View getLoadingView() {
        if (mLoadingView == null) {
            mLoadingView = View.inflate(getContext(), mViewState.loadingLayoutResId, null);
        }

        return mLoadingView;
    }

    @SuppressWarnings("unused")
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

        setContentState(mViewState.state);
    }

    private boolean isViewInternal(View view) {
        return mStateViewCache.indexOfValue(view) >= 0;
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable state = super.onSaveInstanceState();

        SavedState myState = new SavedState(state);

        myState.state = mViewState;

        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (!(state instanceof SavedState)) {
            super.onRestoreInstanceState(state);
            return;
        }

        SavedState myState = (SavedState) state;

        setViewState(myState.state);

        super.onRestoreInstanceState(myState.getSuperState());
    }

    private void setViewState(MultiStateViewData state) {
        setContentState(state.state);
        setTapToRetryString(state.tapToRetryString);
        setGeneralErrorTitleString(state.generalErrorTitleString);
        setNetworkErrorTitleString(state.networkErrorTitleString);
        setGeneralErrorLayoutResourceId(state.generalErrorLayoutResId);
        setNetworkErrorLayoutResourceId(state.networkErrorLayoutResId);
        setLoadingLayoutResourceId(state.loadingLayoutResId);
        setCustomErrorString(state.customErrorString);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        // Prefer the AttachInfo handler on attach:
        mHandler = new MultiStateHandler(getHandler().getLooper());
    }

    @Override
    protected void onDetachedFromWindow() {
        mHandler.removeMessages(MultiStateHandler.MESSAGE_HIDE_PREVIOUS);
        // Reset it to a default looper
        mHandler = new MultiStateHandler();
        super.onDetachedFromWindow();
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
     *
     * @deprecated
     */
    public static enum ContentState {
        /**
         * Used to indicate that content should be displayed to the user
         *
         * @see R.attr#msvState
         */
        CONTENT(CONTENT_STATE_ID_CONTENT),
        /**
         * Used to indicate that the Loading indication should be displayed to the user
         *
         * @see R.attr#msvState
         */
        LOADING(CONTENT_STATE_ID_LOADING),
        /**
         * Used to indicate that the Network Error indication should be displayed to the user
         *
         * @see R.attr#msvState
         */
        ERROR_NETWORK(CONTENT_STATE_ID_ERROR_NETWORK),
        /**
         * Used to indicate that the Unknown Error indication should be displayed to the user
         *
         * @see R.attr#msvState
         */
        ERROR_GENERAL(CONTENT_STATE_ID_ERROR_GENERAL);

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

    public static class SavedState extends View.BaseSavedState {
        MultiStateViewData state;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel in) {
            super(in);
            state = (MultiStateViewData) in.readParcelable(MultiStateViewData.class.getClassLoader());
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeParcelable(state, flags);
        }

        public static final Parcelable.Creator<SavedState> CREATOR = new Parcelable.Creator<SavedState>() {
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }

    public static class MultiStateViewData implements Parcelable {
        public String customErrorString;

        public int loadingLayoutResId;

        public int generalErrorLayoutResId;

        public int networkErrorLayoutResId;

        public String networkErrorTitleString;

        public String generalErrorTitleString;

        public String tapToRetryString;

        public int state;

        public MultiStateViewData(int contentState) {
            state = contentState;
        }

        private MultiStateViewData(Parcel in) {
            customErrorString = in.readString();
            loadingLayoutResId = in.readInt();
            generalErrorLayoutResId = in.readInt();
            networkErrorLayoutResId = in.readInt();
            networkErrorTitleString = in.readString();
            generalErrorTitleString = in.readString();
            tapToRetryString = in.readString();
            state = in.readInt();
        }

        public int describeContents() {
            return 0;
        }

        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(customErrorString);
            dest.writeInt(loadingLayoutResId);
            dest.writeInt(generalErrorLayoutResId);
            dest.writeInt(networkErrorLayoutResId);
            dest.writeString(networkErrorTitleString);
            dest.writeString(generalErrorTitleString);
            dest.writeString(tapToRetryString);
            dest.writeInt(state);
        }

        public static final Parcelable.Creator<MultiStateViewData> CREATOR = new Parcelable.Creator<MultiStateViewData>() {
            public MultiStateViewData createFromParcel(Parcel in) {
                return new MultiStateViewData(in);
            }

            public MultiStateViewData[] newArray(int size) {
                return new MultiStateViewData[size];
            }
        };
    }

    /**
     * Handler used to hide the previous state when switching to a new state
     *
     * @author jhansche
     */
    private class MultiStateHandler extends Handler {
        public static final int MESSAGE_HIDE_PREVIOUS = 0;

        public MultiStateHandler() {
            super();
        }

        public MultiStateHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_HIDE_PREVIOUS:
                    int previousState = msg.arg1;
                    View previousView = getStateView(previousState);
                    if (previousView != null) previousView.setVisibility(View.GONE);
                    break;
            }
        }
    }
}
