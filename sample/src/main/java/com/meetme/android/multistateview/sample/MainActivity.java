package com.meetme.android.multistateview.sample;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.meetme.android.multistateview.MultiStateView;


public class MainActivity extends ActionBarActivity {
    MultiStateView.ContentState mState;

    private MultiStateView mMultiStateView;

    private TextView mExampleOfHowToGetContentView;

    private TextView mStateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mStateView = (TextView) findViewById(R.id.state);
        mMultiStateView = (MultiStateView) findViewById(R.id.content);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        mState = mMultiStateView.getState();
        mExampleOfHowToGetContentView = (TextView) mMultiStateView.getContentView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setStateText(mState);
    }

    private void setStateText(MultiStateView.ContentState state) {
        mStateView.setText(String.format("State: %s", state));
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_rotate_state) {
            // This is only done because we're rotating state; you'd typically just call direct to mMultiStateView#setState(ContentState)
            MultiStateView.ContentState newState = MultiStateView.ContentState.values()[(mState.ordinal() + 1) % MultiStateView.ContentState.values().length];

            setState(newState);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setState(MultiStateView.ContentState state) {
        setStateText(state);
        mMultiStateView.setState(state);
        mState = state;
    }
}