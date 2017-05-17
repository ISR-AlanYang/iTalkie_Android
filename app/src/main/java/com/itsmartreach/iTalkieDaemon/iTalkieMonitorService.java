package com.itsmartreach.iTalkieDaemon;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Binder;
import android.os.IBinder;
import android.preference.Preference;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.content.LocalBroadcastManager;

import com.itsmartreach.libzm.ZmCmdLink;

import java.util.Locale;

public class iTalkieMonitorService extends Service {
    // Binder given to clients
    private final IBinder mBinder = new LocalBinder();

    private ZmCmdLink mZmCmdLink;
    private NotificationManager mNM;

    private TextToSpeech t1;
    AudioManager mAudioManager;


    @Override
    public void onCreate() {
        super.onCreate();

        mZmCmdLink = new ZmCmdLink(this, new ZmCmdLink.ZmEventListener() {
            @Override
            public void onScoStateChanged(boolean isConnected) {
                //sendMessageToActivity("sco_connected",isConnected?1:0);
                if (t1.isSpeaking()) {
                    t1.stop();
                }

            }

            @Override
            public void onSppStateChanged(boolean isConnected) {
                sendMessageToActivity("spp_connected",isConnected?1:0);
                if (t1.isSpeaking()) {
                    t1.stop();
                }
                String announcement = (isConnected?"connected" :"disconnected");
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                boolean play = prefs.getBoolean("play_announcement",true);
                if ( play ) {
                    t1.speak(announcement, TextToSpeech.QUEUE_FLUSH, null);
                }

            }

            @Override
            public void onUserEvent(ZmCmdLink.ZmUserEvent event) {
                //for Zello APP
                Intent intent = new Intent(event== ZmCmdLink.ZmUserEvent.zmEventPttPressed?"com.zello.ptt.down":"com.zello.ptt.up");
                intent.putExtra("com.zello.stayHidden", true);
                getApplicationContext().sendBroadcast(intent);

                //for VoicePing APP
                Intent intent2voiceping = new Intent(event== ZmCmdLink.ZmUserEvent.zmEventPttPressed?"android.intent.action.PTT.down":"android.intent.action.PTT.up");
                getApplicationContext().sendBroadcast(intent2voiceping);


                sendMessageToActivity("ptt_status",event== ZmCmdLink.ZmUserEvent.zmEventPttPressed?1:0);

                if ( event== ZmCmdLink.ZmUserEvent.zmEventPttPressed ){
                    if ( t1.isSpeaking() ){
                        t1.stop();
                    }
                }

            }

            @Override
            public void onBatteryLevelChanged(int level) {

                sendMessageToActivity("battery_level",level);
            }

            @Override
            public void onVolumeChanged(boolean isSco) {
                sendMessageToActivity("volume_changed",0);

                int vol = mAudioManager.getStreamVolume(isSco?6:AudioManager.STREAM_MUSIC);
                int max = mAudioManager.getStreamMaxVolume(isSco?6:AudioManager.STREAM_MUSIC);


                if ( true ) {

                    String announcement = "current volume is " + vol + ", and the maximum one is " + max;
                    if (t1.isSpeaking()) {
                        t1.stop();
                    }
                    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
                    boolean play = prefs.getBoolean("play_announcement",true);
                    if ( play ) {
                        t1.speak(announcement, TextToSpeech.QUEUE_FLUSH, null);
                    }
                }

            }
        },true);


        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });
        mAudioManager = (AudioManager)getSystemService(AUDIO_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mZmCmdLink.destroy();
        t1.shutdown();
    }

    /**
     * Class used for the client Binder.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        iTalkieMonitorService getService() {
            // Return this instance of LocalService so clients can call public methods
            return iTalkieMonitorService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    /** method for clients */
    public void getBatteryLevel(){

        mZmCmdLink.requestBatteryLevel();
    }

    public boolean isScoConnected(){
        return mZmCmdLink.isScoConnected();
    }


    public boolean isSppConnected(){
        return mZmCmdLink.isSppConnected();
    }

    public boolean isPaired(){
        return mZmCmdLink.isPaired();
    }
    public boolean isConnected(){
        return mZmCmdLink.isConnected();
    }



    public iTalkieMonitorService() {
    }


    private void sendMessageToActivity(String key, int value) {
        Intent intent = new Intent("intentKey");

        intent.putExtra(key, value);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
