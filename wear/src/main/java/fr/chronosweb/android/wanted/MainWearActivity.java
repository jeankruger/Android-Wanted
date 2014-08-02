package fr.chronosweb.android.wanted;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ToggleButton;

import fr.chronosweb.android.wanted.R;
import fr.chronosweb.android.wanted.common.Constants;
import fr.chronosweb.android.wanted.common.WearSyncActivityTools;

/**
 * Created by Jean Kruger on 11/07/14.
 * http://www.chronos-web.fr
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/


public class MainWearActivity extends Activity implements View.OnClickListener, WearSyncActivityTools.WearSyncToolsCallback {
    private static MainWearActivity sSingleton = null;
    private static boolean sIsOnForeground = false;

    private WearSyncActivityTools mWearSyncActivityTools;
    private LinearLayout mLinearLayoutWaiter;
    private Handler mUiHandler;
    private ToggleButtonOnUI mRingButton;
    private ToggleButtonOnUI mFlashButton;
    private ToggleButtonOnUI mVibrateButton;

    public static boolean isOpened(){
        return sSingleton != null;
    }

    public static MainWearActivity getSingleton(){
        return sSingleton;
    }

    public static void close(){
        if (sSingleton != null && !sSingleton.isFinishing()){
            sSingleton.finish();
        }
    }

    public static void open(Context context){
        if (!sIsOnForeground) {
            Intent startIntent = new Intent(context, MainWearActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(startIntent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sSingleton = this;

        setContentView(R.layout.activity_main_wear);

        mUiHandler = new Handler(this.getMainLooper());
        mWearSyncActivityTools = new WearSyncActivityTools(this, this);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mLinearLayoutWaiter = (LinearLayout) stub.findViewById(R.id.linearLayoutWaiter);

                mRingButton = new ToggleButtonOnUI((ToggleButton)stub.findViewById(R.id.btnRing));
                mFlashButton = new ToggleButtonOnUI((ToggleButton)stub.findViewById(R.id.btnFlash));
                mVibrateButton = new ToggleButtonOnUI((ToggleButton)stub.findViewById(R.id.btnVibrate));

                mRingButton.getButton().setOnClickListener(MainWearActivity.this);
                mFlashButton.getButton().setOnClickListener(MainWearActivity.this);
                mVibrateButton.getButton().setOnClickListener(MainWearActivity.this);

                if (mWearSyncActivityTools.isConnected()){
                    mLinearLayoutWaiter.setVisibility(View.GONE);
                }
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        sIsOnForeground = false;
        mUiHandler.removeCallbacks(mAutoConnect);
        mWearSyncActivityTools.disconnect();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sIsOnForeground = true;
        mWearSyncActivityTools.connect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sSingleton = null;
    }

    @Override
    public void onClick(View v) {
        if (v instanceof ToggleButton){
            ToggleButton btn = (ToggleButton)v;
            btn.setChecked(!btn.isChecked());
        }

        switch (v.getId()){
            case R.id.btnRing:
                mWearSyncActivityTools.sendMessage(Constants.SWITCH_RING_PATH);
                break;
            case R.id.btnVibrate:
                mWearSyncActivityTools.sendMessage(Constants.SWITCH_VIBRATE_PATH);
                break;
            case R.id.btnFlash:
                mWearSyncActivityTools.sendMessage(Constants.SWITCH_FLASH_PATH);
                break;
        }
    }

    public void setRingStatus(boolean enabled){
        if (mRingButton != null){
            mRingButton.setValue(enabled);
        }
    }

    public void setFlashStatus(boolean enabled){
        if (mFlashButton != null){
            mFlashButton.setValue(enabled);
        }
    }

    public void setVibrateStatus(boolean enabled){
        if (mVibrateButton != null){
            mVibrateButton.setValue(enabled);
        }
    }

    private Runnable mShowWaiter = new Runnable() {
        public void run(){
            if (mLinearLayoutWaiter != null){
                mLinearLayoutWaiter.setVisibility(View.VISIBLE);
            }
        }
    };

    private Runnable mHideWaiter = new Runnable() {
        public void run(){
            if (mLinearLayoutWaiter != null){
                mLinearLayoutWaiter.setVisibility(View.GONE);
            }
        }
    };

    private Runnable mAutoConnect = new Runnable() {
        @Override
        public void run() {
            mUiHandler.removeCallbacks(mAutoConnect);
            mWearSyncActivityTools.connect();
        }
    };

    @Override
    public void onConnecting() {
        mUiHandler.post(mShowWaiter);
    }

    @Override
    public void onConnected() {
        mUiHandler.post(mHideWaiter);
    }

    @Override
    public void onConnectionFailed() {
        mUiHandler.postDelayed(mAutoConnect, 500);
    }

    @Override
    public void onDisconnected() {
        mUiHandler.post(mShowWaiter);
    }

    private class ToggleButtonOnUI implements Runnable{
        private boolean mValue;
        private ToggleButton mButton;

        public ToggleButtonOnUI(ToggleButton button){
            mButton = button;
        }

        public void setValue(boolean value){
            mValue = value;
            mUiHandler.post(this);
        }

        public ToggleButton getButton(){
            return mButton;
        }

        @Override
        public void run() {
            mButton.setChecked(mValue);
        }
    }
}
