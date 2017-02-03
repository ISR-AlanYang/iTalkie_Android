package com.itsmartreach.iTalkieDaemon;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.itsmartreach.libzm.Constants;


public class MainActivity extends ActionBarActivity {


    //UI
    Button mDiagnoseButton;
    TextView mSppStatus, mPttStatus, mAdvise;
    ProgressBar mBatteryBar, mScoVolBar, mA2dpVolBar;
    AudioManager mAudioManager;

    //service interaction
    ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.v(Constants.TAG, "Activity : service connected");

            // We've bound to LocalService, cast the IBinder and get LocalService instance
            iTalkieMonitorService.LocalBinder binder = (iTalkieMonitorService.LocalBinder) service;
            mZmCmdLinkService = binder.getService();
            mBound = true;

            boolean isSppConnected = mZmCmdLinkService.isSppConnected();
            freshDisplayOnConnectionStatus(isSppConnected);

            mSppStatus.setText(isSppConnected?"Connected":"Disconnected");

            mZmCmdLinkService.getBatteryLevel();

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

            mBound = false;
        }
    };
    iTalkieMonitorService mZmCmdLinkService;
    boolean mBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);

        mSppStatus = (TextView)findViewById(R.id.spp_status);
        mPttStatus = (TextView)findViewById(R.id.ptt_status);
        mAdvise = (TextView)findViewById(R.id.advise);
        mAdvise.setVisibility(View.GONE);
        mDiagnoseButton = (Button)findViewById(R.id.button);
        mDiagnoseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mAdvise.setText(doDiagnose());
                mAdvise.setVisibility(View.VISIBLE);
            }
        });
        mBatteryBar = (ProgressBar) findViewById(R.id.battery_bar);
        mScoVolBar = (ProgressBar) findViewById(R.id.sco_vol_bar);
        mA2dpVolBar = (ProgressBar) findViewById(R.id.a2dp_vol_bar);


        LocalBroadcastManager.getInstance(this).registerReceiver(
                mMessageReceiver, new IntentFilter("intentKey"));

        startService(new Intent(this,iTalkieMonitorService.class));



    }

    @Override
    protected void onStart() {
        super.onStart();
        // Bind to LocalService
        Intent intent = new Intent(this, iTalkieMonitorService.class);
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);

        refreshVolumeDisplay();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Unbind from the service
        if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            startActivity(new Intent(this,SettingsActivity.class));
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private BroadcastReceiver mMessageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            // Get extra data included in the Intent
            if ( intent.hasExtra("battery_level") ) {
                int level = intent.getIntExtra("battery_level", 0);
                if (level > 0) {
                    Log.v(Constants.TAG, "Activity : battery level = " + level);
                    mBatteryBar.setProgress(level);
                }
            } else if ( intent.hasExtra("ptt_status") ) {
                int ptt_status = intent.getIntExtra("ptt_status", 0);
                Log.v(Constants.TAG, "Activity : ptt_status = " + ptt_status);
                mPttStatus.setText(ptt_status==1?"Pressed":"Released");


            }else if ( intent.hasExtra("spp_connected") ) {
                int spp_connected = intent.getIntExtra("spp_connected", 0);
                Log.v(Constants.TAG, "Activity : spp_connected = " + spp_connected);
                mSppStatus.setText(spp_connected==1?"Connected":"Disconnected");

                freshDisplayOnConnectionStatus(spp_connected==1);
            }else if ( intent.hasExtra("volume_changed") ) {
                refreshVolumeDisplay();
            }
            return;
        }
    };
    private void refreshVolumeDisplay(){
        int scoMax = mAudioManager.getStreamMaxVolume(6/*AudioManager.STREAM_BLUETOOTH_SCO*/);
        int scoVol = mAudioManager.getStreamVolume(6);
        mScoVolBar.setMax(scoMax);
        mScoVolBar.setProgress(scoVol);

        int a2dpMax = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int a2dpVol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        mA2dpVolBar.setMax(a2dpMax);
        mA2dpVolBar.setProgress(a2dpVol);
    }

    private String doDiagnose(){
        String advise = "";
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (!mBluetoothAdapter.isEnabled()) {
            advise = "It seems that Bluetooth was disabled on your phone, Could you please enable it in order to use the iTalkie?";
        }
        else if ( !mZmCmdLinkService.isPaired() ){
            advise = "It seems that iTalkie was not paired with your phone, Could you please pair it before using the iTalkie?";
        }
        else if ( !mZmCmdLinkService.isConnected() ){
            advise = "Yeah, Bluetooth is enabled and the iTalkie was successfully paired. Could you please make sure iTalkie is powered on and within 10 meters to your phone?";
        }
        else{
            advise = "Everything looks fine, Could you please reboot the iTalkie or your phone to have a try?";

        }
        return advise;
    }

    private void freshDisplayOnConnectionStatus(boolean isConnected){
        mDiagnoseButton.setVisibility(isConnected?View.INVISIBLE:View.VISIBLE);
        mAdvise.setVisibility(View.GONE);

        mBatteryBar.setVisibility(isConnected?View.VISIBLE:View.INVISIBLE);
        mPttStatus.setVisibility(isConnected?View.VISIBLE:View.INVISIBLE);
        mScoVolBar.setVisibility(isConnected?View.VISIBLE:View.INVISIBLE);
        mA2dpVolBar.setVisibility(isConnected?View.VISIBLE:View.INVISIBLE);

    }

}
