package fr.chronosweb.android.wanted;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.os.Vibrator;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.google.android.gms.wearable.PutDataMapRequest;

import fr.chronosweb.android.wanted.common.Constants;
import fr.chronosweb.android.wanted.common.WearSyncActivityTools;
import fr.chronosweb.android.wanted.R;

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


public class MainActivity extends ActionBarActivity implements View.OnClickListener, WearSyncActivityTools.WearSyncToolsCallback {
    private final static String TAG = "PhoneWhereAreYouHandset";

    private static MainActivity sSingleton = null;
    private static boolean sIsOnForeground = false;

    private boolean mIsRealResume = false;
    private Handler mUIHandler;

    private WearSyncActivityTools mWearSyncActivityTools;
    private Vibrator mVibrator;
    private Ringtone mRingtone;
    private int mDefaultRingMode = 0;
    private int mDefaultRingVolume = 0;
    private boolean mOnFlash = false;

    private ViewGroup mMainView;
    private ToggleButton mBtnRing;
    private ToggleButton mBtnVibrate;
    private ToggleButton mBtnFlash;
    private TextView mTextViewStatus;

    private PowerManager.WakeLock mWakeLock;

    private PutDataMapRequest mChangeStatusMap = PutDataMapRequest.create(Constants.CHANGE_STATUS_PATH);

    private ChangeStatusOnUIThread mChangeStatusOnUIThread = new ChangeStatusOnUIThread();

    public static boolean isOpened(){
        return sSingleton != null;
    }

    public static boolean isOnForeground(){
        return sIsOnForeground;
    }

    public static MainActivity getSingleton(){
        return sSingleton;
    }

    public static void close(){
        if (sSingleton != null && !sSingleton.isFinishing()){
            sSingleton.finish();
        }
    }

    public static void open(Context context){
        if (sSingleton == null || !sIsOnForeground){
            Intent startIntent = new Intent(context, MainActivity.class);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(startIntent);
        }else{
            sSingleton.onConnected();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        sSingleton = this;
        setContentView(R.layout.activity_main);

        mUIHandler = new Handler(this.getMainLooper());

        mMainView = (ViewGroup) this.findViewById(R.id.mainView);
        mBtnRing = (ToggleButton) this.findViewById(R.id.btnRing);
        mBtnVibrate = (ToggleButton) this.findViewById(R.id.btnVibrate);
        mBtnFlash = (ToggleButton) this.findViewById(R.id.btnFlash);
        mTextViewStatus = (TextView) this.findViewById(R.id.textViewStatus);

        mBtnRing.setOnClickListener(this);
        mBtnVibrate.setOnClickListener(this);
        mBtnFlash.setOnClickListener(this);
        mTextViewStatus.setOnClickListener(this);

        mChangeStatusMap.getDataMap().putBoolean(Constants.STATUS_RING_KEY, false);
        mChangeStatusMap.getDataMap().putBoolean(Constants.STATUS_FLASH_KEY, false);
        mChangeStatusMap.getDataMap().putBoolean(Constants.STATUS_VIBRATE_KEY, false);

        mWearSyncActivityTools = new WearSyncActivityTools(this, this);
    }

    public void turnScreenOn(){
        final PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if (mWakeLock != null){
            mWakeLock.release();
        }

        mWakeLock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,"");
        mWakeLock.acquire();
    }

    @Override
    protected void onResume() {
        super.onResume();
        sIsOnForeground = true;
        mWearSyncActivityTools.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sIsOnForeground = false;
        this.stopAll();
        if (mWakeLock != null){
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    protected void onDestroy() {
        this.stopAll();
        mWearSyncActivityTools.disconnect();
        sSingleton = null;
        super.onDestroy();
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
        if (id == R.id.action_about) {
            this.startActivity(new Intent(this, AboutActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Runnable mRunnableFlashBlink = new Runnable() {
        public void run() {
            mUIHandler.removeCallbacks(mRunnableFlashBlink);
            mUIHandler.postDelayed(mRunnableFlashBlink, 300);
            Drawable background = mMainView.getBackground();
            if (background != null && background instanceof TransitionDrawable){
                TransitionDrawable transitionDrawable = (TransitionDrawable)background;
                transitionDrawable.reverseTransition(100);
            }
        }
    };

    private Runnable mSwitchVibrateOnUIThread = new Runnable() {
        public void run(){
            switchVibrate();
        }
    };

    private Runnable mSwitchRingOnUIThread = new Runnable() {
        public void run(){
            switchRing();
        }
    };

    private Runnable mSwitchFlashOnUIThread = new Runnable() {
        public void run(){
            switchFlash();
        }
    };

    public void stopAll(){
        if (this.ringIsStarted()){
            this.stopRing();
        }

        if (this.vibrateIsStarted()){
            this.stopVibrate();
        }

        if (this.flashIsStarted()) {
            this.stopFlash();
        }
    }

    public void startRing(){
        AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        mDefaultRingMode = manager.getRingerMode();
        mDefaultRingVolume = manager.getStreamVolume(AudioManager.STREAM_RING);

        manager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        manager.setStreamVolume(AudioManager.STREAM_RING, manager.getStreamMaxVolume(AudioManager.STREAM_RING), 0);

        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        mRingtone = RingtoneManager.getRingtone(getApplicationContext(), alert);
        mRingtone.play();

        mBtnRing.setChecked(true);
        mChangeStatusMap.getDataMap().putBoolean(Constants.STATUS_RING_KEY, true);
        mWearSyncActivityTools.putDataItem(mChangeStatusMap);
    }

    public void stopRing(){
        mRingtone.stop();
        mRingtone = null;

        AudioManager manager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        manager.setStreamVolume(AudioManager.STREAM_RING, mDefaultRingVolume, 0);
        manager.setRingerMode(mDefaultRingMode);

        mBtnRing.setChecked(false);
        mChangeStatusMap.getDataMap().putBoolean(Constants.STATUS_RING_KEY, false);
        mWearSyncActivityTools.putDataItem(mChangeStatusMap);
    }

    public boolean ringIsStarted(){
        return (mRingtone != null);
    }

    public void switchRing(){
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            if (!this.ringIsStarted()){
                this.startRing();
            }else{
                this.stopRing();
            }
        }else{
            mUIHandler.post(mSwitchRingOnUIThread);
        }
    }

    public void startVibrate(){
        mVibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0, 200, 100};
        mVibrator.vibrate(pattern, 0);
        mBtnVibrate.setChecked(true);
        mChangeStatusMap.getDataMap().putBoolean(Constants.STATUS_VIBRATE_KEY, true);
        mWearSyncActivityTools.putDataItem(mChangeStatusMap);
    }

    public void stopVibrate(){
        mVibrator.cancel();
        mVibrator = null;
        mBtnVibrate.setChecked(false);
        mChangeStatusMap.getDataMap().putBoolean(Constants.STATUS_VIBRATE_KEY, false);
        mWearSyncActivityTools.putDataItem(mChangeStatusMap);
    }

    public boolean vibrateIsStarted(){
        return (mVibrator != null);
    }

    public void switchVibrate(){
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            if (!this.vibrateIsStarted()){
                this.startVibrate();
            }else{
                this.stopVibrate();
            }
        }else{
            mUIHandler.post(mSwitchVibrateOnUIThread);
        }
    }


    public void startFlash(){
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = 1F;
        getWindow().setAttributes(layout);

        mMainView.setBackgroundResource(R.drawable.blink);
        mUIHandler.post(mRunnableFlashBlink);
        mOnFlash = true;
        mBtnFlash.setChecked(true);
        mChangeStatusMap.getDataMap().putBoolean(Constants.STATUS_FLASH_KEY, true);
        mWearSyncActivityTools.putDataItem(mChangeStatusMap);
    }

    public void stopFlash(){
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = -1F;
        getWindow().setAttributes(layout);

        mUIHandler.removeCallbacks(mRunnableFlashBlink);
        mMainView.setBackgroundResource(R.drawable.grey);
        mOnFlash = false;
        mBtnFlash.setChecked(false);
        mChangeStatusMap.getDataMap().putBoolean(Constants.STATUS_FLASH_KEY, false);
        mWearSyncActivityTools.putDataItem(mChangeStatusMap);
    }

    public boolean flashIsStarted(){
        return mOnFlash;
    }

    public void switchFlash(){
        if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
            if (!this.flashIsStarted()) {
                this.startFlash();
            } else {
                this.stopFlash();
            }
        }else{
            mUIHandler.post(mSwitchFlashOnUIThread);
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.btnRing:
                this.switchRing();
                break;
            case R.id.btnVibrate:
                this.switchVibrate();
                break;
            case R.id.btnFlash:
                this.switchFlash();
                break;
            case R.id.textViewStatus:
                this.mWearSyncActivityTools.connect();
                break;
        }
    }

    private class ChangeStatusOnUIThread implements Runnable{
        private int mTextRes = R.string.connecting;
        private int mTextColorRes = R.color.darkgrey;

        public void setStatus(int textRes, int textColorRes){
            mTextRes = textRes;
            mTextColorRes = textColorRes;
        }

        public void run(){
            mTextViewStatus.setText(mTextRes);
            mTextViewStatus.setTextColor(getResources().getColor(mTextColorRes));
        }
    }



    @Override
    public void onConnecting() {
        mChangeStatusOnUIThread.setStatus(R.string.connecting, R.color.darkgrey);
        mUIHandler.post(mChangeStatusOnUIThread);
    }

    @Override
    public void onConnected() {
        mChangeStatusOnUIThread.setStatus(R.string.connected, R.color.green);
        mUIHandler.post(mChangeStatusOnUIThread);
    }

    @Override
    public void onConnectionFailed() {
        mChangeStatusOnUIThread.setStatus(R.string.notconnected, R.color.red);
        mUIHandler.post(mChangeStatusOnUIThread);
    }

    @Override
    public void onDisconnected() {
        onConnecting();
    }
}
