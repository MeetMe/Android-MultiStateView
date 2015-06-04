package com.meetme.android.multistateview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import java.util.Locale;

/**
 * A view designed to wrap a single child (the "content") and hide/show that content based on the current "state" (see {@link ContentState}) of this
 * View. Note that this layout can only have one direct descendant which is used as the "content" view
 */
@SuppressWarnings("NullableProblems")
public class MultiStateView extends FrameLayout {
    private static final String TAG = "MultiStateView";
    private final MultiStateViewData mViewState = new MultiStateViewData(ContentState.CONTENT);

    private View mContentView;
    private View mLoadingView;
    private View mNetworkErrorView;
    private View mGeneralErrorView;
    private OnClickListener mTapToRetryClickListener;

    public MultiStateView(Context context) {
        this(context, null);
    }

    public MultiStateView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public MultiStateView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        // Start out with a default handler/looper
        parseAttrs(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public boolean canScrollVertically(int direction) {
        // This allows us to pass along whether our child is vertically scrollable or not (useful for SwipeRefreshLayout, for example)
        return super.canScrollVertically(direction)
                || (getState() == ContentState.CONTENT && mContentView != null && mContentView.canScrollVertically(direction));
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

            setState(a.getInt(R.styleable.MultiStateView_msvState, ContentState.CONTENT.nativeInt));
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

    /**
     * @return the {@link ContentState} the view is currently in
     */
    @NonNull
    public ContentState getState() {
        return mViewState.state != null ? mViewState.state : ContentState.CONTENT;
    }

    /**
     * Configures the view to be in the given state. This method is an internal method used for parsing the native integer value used in attributes
     * in XML.
     *
     * @param nativeInt
     * @see ContentState
     * @see #setState(ContentState)
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
            if (BuildConfig.DEBUG) Log.v(TAG, "Already in state " + mViewState.state);
            // No change
            return;
        }

        final View contentView = getContentView();

        if (contentView == null) {
            if (BuildConfig.DEBUG) Log.v(TAG, "Content not yet set, waiting...");
            return;
        }

        // Hide the previous state view
        final ContentState previousState = mViewState.state;
        View previousView = getStateView(previousState);

        if (previousView != null) {
            if (BuildConfig.DEBUG) Log.v(TAG, "Hiding previous state " + previousState);
            previousView.setVisibility(View.GONE);
        }

        // Show the new state view
        View newStateView = getStateView(state);

        if (newStateView != null) {
            if (state == ContentState.ERROR_GENERAL) {
                TextView view = ((TextView) newStateView.findViewById(R.id.error_title));

                if (view != null) {
                    view.setText(getGeneralErrorTitleString());
                }
            }

            if (BuildConfig.DEBUG) Log.v(TAG, "Showing new state " + state);
            newStateView.setVisibility(View.VISIBLE);
        }

        mViewState.state = state;

        if (BuildConfig.DEBUG) {
            dumpState();

            // Now check if there are multiple visible children and emit a warning if so
            boolean hasVisible = false;

            for (int i = 0; i < getChildCount(); i++) {
                View v = getChildAt(i);

                if (v != null && v.getVisibility() != View.GONE) {
                    // This should not happen
                    if (hasVisible) Log.w(TAG, "MultiStateView has multiple visible children!");
                    hasVisible = true;
                }
            }
        }
    }

    /** Dump the current state of the view. Requires {@link BuildConfig#DEBUG}. */
    public void dumpState() {
        if (!BuildConfig.DEBUG) return;

        Log.v(TAG, "/-- Start Dump State ---");
        Log.v(TAG, "| Current state = " + mViewState.state);
        Log.v(TAG, "| Children: " + getChildCount());

        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            ContentState state = null;

            if (child == mContentView) {
                state = ContentState.CONTENT;
            } else if (child == mGeneralErrorView) {
                state = ContentState.ERROR_GENERAL;
            } else if (child == mNetworkErrorView) {
                state = ContentState.ERROR_NETWORK;
            } else if (child == mLoadingView) {
                state = ContentState.LOADING;
            }

            Log.v(TAG, String.format(Locale.US, "| - #%d: %s (%s) -> %s",
                    i, child, state, (child != null && child.getVisibility() == View.VISIBLE ? "visible" : "gone")));
        }

        Log.v(TAG, "\\-- End Dump State ---");
    }

    /**
     * Returns the given view corresponding to the specified {@link ContentState}
     *
     * @param state
     * @return
     */
    @Nullable
    public View getStateView(ContentState state) {
        if (state == null) return null;

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
    @NonNull
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
    @NonNull
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
    @NonNull
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
    @Nullable
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
    protected Parcelable onSaveInstanceState() {
        Parcelable state = super.onSaveInstanceState();
        SavedState myState = new SavedState(state);
        myState.state = mViewState;
        if (BuildConfig.DEBUG) Log.v(TAG, "Saved state: " + myState.state);
        return myState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        if (state instanceof SavedState) {
            SavedState myState = (SavedState) state;
            setViewState(myState.state);
            state = myState.getSuperState();
        }

        super.onRestoreInstanceState(state);
    }

    private void setViewState(MultiStateViewData state) {
        if (BuildConfig.DEBUG) Log.v(TAG, "Restoring state: " + state);
        setState(state.state);
        setTapToRetryString(state.tapToRetryString);
        setGeneralErrorTitleString(state.generalErrorTitleString);
        setNetworkErrorTitleString(state.networkErrorTitleString);
        setGeneralErrorLayoutResourceId(state.generalErrorLayoutResId);
        setNetworkErrorLayoutResourceId(state.networkErrorLayoutResId);
        setLoadingLayoutResourceId(state.loadingLayoutResId);
        setCustomErrorString(state.customErrorString);
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
    public enum ContentState {
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
        private static final SparseArray<ContentState> sStates = new SparseArray<>();

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

        ContentState(int nativeValue) {
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
            state = in.readParcelable(MultiStateViewData.class.getClassLoader());
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
        public ContentState state;

        public MultiStateViewData(ContentState contentState) {
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
            state = ContentState.valueOf(in.readString());
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
            dest.writeString(state.name());
        }

        public static final Parcelable.Creator<MultiStateViewData> CREATOR = new Parcelable.Creator<MultiStateViewData>() {
            public MultiStateViewData createFromParcel(Parcel in) {
                return new MultiStateViewData(in);
            }

            public MultiStateViewData[] newArray(int size) {
                return new MultiStateViewData[size];
            }
        };

        @Override
        public String toString() {
            if (BuildConfig.DEBUG) {
                return String.format(Locale.US, "MultiStateViewData{state=%s}", state);
            }

            return super.toString();
        }
    }
}