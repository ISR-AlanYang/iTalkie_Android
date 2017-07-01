package com.itsmartreach.libzm;

import android.bluetooth.BluetoothA2dp;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHeadset;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.support.annotation.Keep;
import android.util.Log;
import android.widget.Toast;

import com.itsmartreach.libzm.gaia.SPP;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Alan on 15/1/1.
 */
public class ZmCmdLink  {
    public static final int AH_AUDIO_ROUTE_UNKNOWN = 0;
    public static final int AH_AUDIO_ROUTE_SPEAKER = 1;
    public static final int AH_AUDIO_ROUTE_A2DP = 2;
    public static final int AH_AUDIO_ROUTE_SPP = 3;
    public static final int AH_AUDIO_ROUTE_SPP_STANDBY = 46;

    private int mLastVoiceRoute = AH_AUDIO_ROUTE_UNKNOWN;

    private Context mContext;
    private AudioManager mAudioManager;



    private boolean mIsBTDeviceConnected = false;
    private boolean mIsBTScoAudioConnected = false;
    private boolean mIsBTSppConnected = false;
    private int mAudioRouteMode = AH_AUDIO_ROUTE_UNKNOWN;

    @Keep
    public enum ZmUserEvent{
        zmEventPttPressed,
        zmEventPttReleased,
        //add + and - key event
        zmEventVolumeUpPressed,
        zmEventVolumeUpReleased,
        zmEventVolumeDownPressed,
        zmEventVolumeDownReleased
    }

    @Keep
    public interface ZmEventListener {
        //语音通道状态变化
        public void onScoStateChanged(boolean isConnected);
        //控制通道状态变化
        public void onSppStateChanged(boolean isConnected);
        //对讲按键状态变化
        public void onUserEvent(ZmUserEvent event);
        //电池电量变化
        public void onBatteryLevelChanged(int level);
        //音量变化
        public void onVolumeChanged(boolean isSco);
    }
    private ZmEventListener mListener;
    
    public ZmCmdLink(Context context , ZmEventListener listener, boolean isAutoConnectSppEnabled) {
        mContext = context;
        mListener = (ZmEventListener) listener;
        mIsAutoConnectSppEnabled = isAutoConnectSppEnabled;


        IntentFilter intentFilter = new IntentFilter();
        //监测 蓝牙手咪
        intentFilter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED);//BT device power ON/OFF
        intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);//BT sco channel established/not
        //是否开启蓝牙
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);//BT enable/disable
        mContext.registerReceiver(mBluetoothReceiver, intentFilter);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        init();
    }

    //配置：启用智咪
    private boolean mIsBtEnabled = false;
    //配置：启用电量显示
    private boolean mIsSppEnabled = false;
    private SPP mSpp;


    public void requestBatteryLevel(){
        if ( mSpp != null ) {
            mSpp.requestBtDeviceBatteryLevel();
        }
    }

    private void init(  ){
        mIsBtEnabled = true;
        mIsSppEnabled = true;

        //创建SPP实例
        if ( mSpp == null ){
            mSpp = new SPP(new SPP.SppListener() {
                @Override
                public void onUserEvent(int event) {
                    if ( !mIsBTSppConnected ){
                        mIsBTSppConnected = true;
                        //mListener.onSppStateChanged(true);
                        Log.v(Constants.TAG, "SPP : make spp on, mZmNotFoundInConnectedDevices="+mZmNotFoundInConnectedDevices+" mIsBTDeviceConnected="+mIsBTDeviceConnected);

                    }
                    switch ( event ){
                        case 0x602a:
                            Log.v(Constants.TAG, "SPP : mic in mute state");
                            break;

                        case 0x6029:
                            Log.d(Constants.TAG, "SPP : mute de-active <<<");

                            break;

                        case 0x6028:
                            Log.d(Constants.TAG, "SPP : mute active >>>");
                            break;


                        case 0x60c2:
                            //PTT按下
                            Log.d(Constants.TAG, "SPP : PTT pressed");
                            if ( mListener != null ) {
                                mListener.onUserEvent(ZmUserEvent.zmEventPttPressed);
                            }
                            break;
                        case 0x60c3:
                            //PTT松开
                            Log.d(Constants.TAG, "SPP : PTT released");
                            if ( mListener != null ) {
                                mListener.onUserEvent(ZmUserEvent.zmEventPttReleased);
                            }
                            break;
                        case 0x60c5:
                            Log.v(Constants.TAG, "Audio | SPP :  VOL－  UP");
                            if ( mListener != null ) {
                                mListener.onUserEvent(ZmUserEvent.zmEventVolumeDownReleased);
                            }
                            break;
                        case 0x60c4:
                            Log.v(Constants.TAG, "Audio | SPP :  VOL－  DOWN");
                            if ( mListener != null ) {
                                mListener.onUserEvent(ZmUserEvent.zmEventVolumeDownPressed);
                            }
                            break;

                        case 0x60c1:
                            Log.v(Constants.TAG, "Audio | SPP :  VOL+ UP");
                            if ( mListener != null ) {
                                mListener.onUserEvent(ZmUserEvent.zmEventVolumeUpReleased);
                            }

                            break;
                        case 0x60c0:
                            Log.v(Constants.TAG, "Audio | SPP :  VOL+  DOWN");
                            if ( mListener != null ) {
                                mListener.onUserEvent(ZmUserEvent.zmEventVolumeUpPressed);
                            }
                            break;

                        case 0x600c:
                            Log.d(Constants.TAG, "Audio | SPP :  VOL- ");
                            boolean isSco = mAudioManager.isBluetoothScoOn();
                            if ( !isSco ) {
                                //int maxVol = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                                int vol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                if (vol > 1) {
                                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol - 1, 0);
                                }

                                Log.v(Constants.TAG, "SPP : --------------" + "MUSIC" + "--------------");
                                Log.v(Constants.TAG, "SPP : VOL = " + vol );
                                Log.v(Constants.TAG, "SPP : ----------------------------------");
                            }
                            mListener.onVolumeChanged(isSco);
                            break;
                        case 0x600b:
                            Log.d(Constants.TAG, "Audio | SPP :  VOL+ ");
                            Log.d(Constants.TAG, "Audio | SPP :  mIsBTScoAudioConnected = "+mIsBTScoAudioConnected);
                            isSco = mAudioManager.isBluetoothScoOn();
                            if ( !isSco ) {
                                int maxVol = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

                                int vol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                if (vol < maxVol) {
                                    mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol + 1, 0);
                                }

                                Log.v(Constants.TAG, "SPP : --------------" + "MUSIC" + "--------------");
                                Log.v(Constants.TAG, "SPP : VOL = " + vol + " / " + maxVol);
                                Log.v(Constants.TAG, "SPP : ----------------------------------");
                            }
                            mListener.onVolumeChanged(isSco);
                            break;
                        default:
                            break;
                    }

                }

                @Override
                public void onSppStateChanged(int state) {

                    Log.d(Constants.TAG, "SPP : "
                                    + (state==SPP.SPP_STATE_CONNECTED?"连接建立 ":"连接断开 ")
                    );

                    mIsBTSppConnected = (state==SPP.SPP_STATE_CONNECTED);

                    if ( mListener != null ) {
                        mListener.onSppStateChanged(state==SPP.SPP_STATE_CONNECTED);
                    }

                }

                @Override
                public void onBatteryLevelChanged(int level) {
                    if ( mListener != null ) {
                        mListener.onBatteryLevelChanged(level);
                        Log.d(Constants.TAG, "Audio | SPP :  onBatteryLevelChanged "+level);

                    }
                }
            });
        }

        a2dpInit();
    }



    public void destroy(){
        disconnectSpp();
        mContext.unregisterReceiver(mBluetoothReceiver);

        if ( mIsA2dpInitialized ) {
            mContext.unregisterReceiver(mA2dpReceiver);
        }

        if ( mBluetoothA2dp != null ) {
            mBluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, mBluetoothA2dp);
        }
    }

    private boolean mIsAutoConnectSppEnabled = true;




    public void enterSppMode(){
        mAudioManager.setSpeakerphoneOn(false);
        mAudioManager.setBluetoothScoOn(true);
        mAudioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        mAudioManager.setWiredHeadsetOn(false);

        mAudioRouteMode = AH_AUDIO_ROUTE_SPP;

    }

    public void bypassPhoneCall( boolean bypass ){
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (!bypass) {
            if (!mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.enable();

            }
        }
        else {
            if (mBluetoothAdapter.isEnabled()) {
                mBluetoothAdapter.disable();
            }
        }
    }

    //SPP 控制发言模式下，SCO断开状态（发言时才连接SCO）
    public void enterSppStandbyMode(){
        //todo
        mAudioManager.setSpeakerphoneOn(false);//?
        mAudioManager.setBluetoothScoOn(false);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mAudioManager.setWiredHeadsetOn(false);

        mAudioRouteMode = AH_AUDIO_ROUTE_SPP_STANDBY;
    }

    public void enterSpeakMode(){
        mAudioManager.setBluetoothScoOn(false);
        mAudioManager.setSpeakerphoneOn(true);
        mAudioManager.setMode(AudioManager.MODE_NORMAL);
        mAudioManager.setWiredHeadsetOn(false);
        mAudioRouteMode = AH_AUDIO_ROUTE_SPEAKER;
    }



    public String getAudioRouteModeString(){
        String str = "<unknown>";
        switch (mAudioRouteMode){

            case AH_AUDIO_ROUTE_SPEAKER:
                str = "<SPK>";
                break;
            case AH_AUDIO_ROUTE_A2DP:
                str = "<A2DP>";
                break;
            case AH_AUDIO_ROUTE_SPP:
                str = "<SPP>";
                break;
            case AH_AUDIO_ROUTE_SPP_STANDBY:
                str = "<SPP_STANDBY>";
                break;
        }
        return str;
    }

    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


            String action = intent.getAction();
            if(action.equals(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)){
                int deviceState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE,BluetoothHeadset.STATE_DISCONNECTED);
                switch (deviceState){
                    case BluetoothHeadset.STATE_CONNECTED:

                        mIsBTDeviceConnected = true;

                        String mac = String.valueOf(intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
                        mLastConnectedAddr = mac;

                        Log.i(Constants.TAG,"SPP : BT device connected: "+mac+"["+intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)+"]");


                        break;

                    case BluetoothHeadset.STATE_DISCONNECTED:

                        mIsBTDeviceConnected = false;
                        //蓝牙手咪关机情况下，系统未通知SCO_AUDIO_STATE_DISCONNECTED
                        mIsBTScoAudioConnected = false;

                        break;
                }

            }
            else if(action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {

                int audioState = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);

                if ( mListener != null ){
                    mListener.onScoStateChanged(audioState == AudioManager.SCO_AUDIO_STATE_CONNECTED);
                    if ( audioState == AudioManager.SCO_AUDIO_STATE_CONNECTED ){
                        //enterSppMode();
                        mIsBTScoAudioConnected = true;
                        Log.i(Constants.TAG,"SPP : SCO_AUDIO_STATE_CONNECTED");

                    }
                    else{
                        //enterSppStandbyMode();
                        mIsBTScoAudioConnected = false;
                        Log.i(Constants.TAG,"SPP : audioState = "+audioState);

                    }
                }

            }
            else if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)){
                int btEnableState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,BluetoothAdapter.ERROR);
                Log.v(Constants.TAG,"SPP : btEnableState ="+btEnableState);
                switch (btEnableState){
                    case BluetoothAdapter.STATE_ON:
                        Log.i(Constants.TAG,"SPP : BT power on");
                        //TODO: auto connect to our device ...


                        break;
                    case BluetoothAdapter.STATE_OFF:
                        Log.i(Constants.TAG,"SPP : BT power off");
                        mIsBTScoAudioConnected = false;
                        mIsBTDeviceConnected = false;


                        break;
                }
            }

        }
    };

    private String mLastSppAddr = null;
    private String mLastConnectedAddr = null;

    private void connectSpp( String addr ){
        if ( addr != null && mSpp != null ){
            mLastSppAddr = addr;
            mSpp.connect(addr);
        }
    }
    public boolean connectSpp( ){
        if ( mLastSppAddr != null && mSpp != null ){
            mSpp.connect(mLastSppAddr);
            return true;
        }
        else{
            Log.w(Constants.TAG,"SPP : BT address null");
            return false;
        }
    }

    public void disconnectSpp(){
        if ( mSpp != null ){
            mSpp.disconnect();
        }
    }



    //------------------------------------------------------------------------------------------
    //                                      SPP控制发言
    //------------------------------------------------------------------------------------------


    //------------------------------------------------------------------------------------------
    //                                      蓝牙音箱
    //------------------------------------------------------------------------------------------
    static final int JS_A2DP_STATE_IDLE = 0;
    static final int JS_A2DP_STATE_CONNECTING = 1;
    static final int JS_A2DP_STATE_CONNECTED = 2;
    private int mA2dpDeviceState;
    private String mA2dpDeviceName;
    private UUID MY_UUID = UUID.fromString("0000110A-0000-1000-8000-00805F9B34FB");
    private BluetoothAdapter mBluetoothAdapter;
    /*private Runnable mAutoConnectA2dpDeviceRunable = new Runnable() {
        @Override
        public void run() {
            if( mA2dpDeviceState == JS_A2DP_STATE_IDLE ){
                Log.v(Constants.TAG,"Audio : time up to connect A2DP ");
                a2dpConnect();
            }
        }
    };*/


    //------------------------------------接口函数------------------------------------
    private boolean mIsA2dpInitialized = false;
    private void a2dpInit(){
            mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            mA2dpDeviceState = JS_A2DP_STATE_IDLE;
            //Alan add 监测A2DP(蓝牙音箱)状态
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED);
            intentFilter.addAction(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED);
            mContext.registerReceiver(mA2dpReceiver, intentFilter);

            //启动后，查询A2DP状态
            mA2dpDeviceState = mAudioManager.isBluetoothA2dpOn() ? JS_A2DP_STATE_CONNECTED : JS_A2DP_STATE_IDLE;
            mIsA2dpInitialized = true;
            Log.v(Constants.TAG, "SPP : A2DP state = " + mA2dpDeviceState);

            if ( mA2dpDeviceState == JS_A2DP_STATE_CONNECTED ){
                onA2dpConnected();
            }

            mZmNotFoundInPairedDevices = (a2dpFindDevice()==null?true:false);

    }

    public boolean isA2dpConnected(){
        return (mA2dpDeviceState == JS_A2DP_STATE_CONNECTED);
    }
    public boolean isSppConnected(){
        return mIsBTSppConnected;
    }

    public boolean isScoConnected(){
        return mIsBTScoAudioConnected;
    }



    private BroadcastReceiver mA2dpReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED)) {
                int deviceState = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, BluetoothHeadset.STATE_DISCONNECTED);
                switch (deviceState) {
                    case BluetoothA2dp.STATE_CONNECTED:
                        Log.d(Constants.TAG, "Audio : A2DP connected");
                        mA2dpDeviceState = JS_A2DP_STATE_CONNECTED;

                        checkConnectivity();

                        break;

                    case BluetoothA2dp.STATE_DISCONNECTED:

                        checkConnectivity();
                        if ( mZmNotFoundInConnectedDevices ) {
                            Log.d(Constants.TAG, "SPP : ZM A2DP disconnected");
                            mA2dpDeviceState = JS_A2DP_STATE_IDLE;
                            mListener.onSppStateChanged(false);
                            mIsBTSppConnected = false;
                            mIsBTDeviceConnected = false;
                        }

                        break;
                }


            } else if (action.equals(BluetoothA2dp.ACTION_PLAYING_STATE_CHANGED)) {
                int playingState = intent.getIntExtra(BluetoothA2dp.EXTRA_STATE, BluetoothA2dp.STATE_NOT_PLAYING);
                Log.v(Constants.TAG, "SPP : A2DP state = " + playingState);

            }
        }

    };


    private BluetoothDevice a2dpFindDevice( ){
        if ( mBluetoothAdapter != null ) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            // If there are paired devices
            if (pairedDevices.size() > 0) {
                // Loop through paired devices
                for (BluetoothDevice device : pairedDevices) {
                    // Add the name and address to an array adapter to show in a ListView
                    //Log.d(Constants.TAG,"Audio : paired BT device: "+device.getName()+" [MAC:"+device.getAddress()+"]");
                    if ( device.getName().contains("智咪") ){
                        //Log.i(Constants.TAG,"Audio : found ours: "+device.getName()+" [MAC:"+device.getAddress()+"]");

                        return device;
                    }
                }
            }

        }
        return null;
    }


    BluetoothDevice mLastDev;
    BluetoothA2dp mBluetoothA2dp;
    private BluetoothProfile.ServiceListener mProfileListener = new BluetoothProfile.ServiceListener() {
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            if (profile == BluetoothProfile.A2DP) {
                Log.v(Constants.TAG, "SPP : A2DP proxy connected");
                mBluetoothA2dp = (BluetoothA2dp) proxy;

                /*BluetoothDevice zm = a2dpFindDevice();
                if ( zm != null && mBluetoothA2dp.getConnectionState(zm) == BluetoothProfile.STATE_DISCONNECTED
                        && !isSppConnected() ) { // 不主动连第二个已配对智咪
                    Log.d(Constants.TAG, "SPP : connect to " + zm.getName() +" "+ zm.getAddress()+" "+mBluetoothA2dp.getConnectionState(zm));

                    try {
                        Method connect = BluetoothA2dp.class.getDeclaredMethod("connect", BluetoothDevice.class);
                        try {
                            connect.invoke(proxy, zm);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }
                }*/

                //todo
                //mBluetoothA2dp.getConnectedDevices();
                List<BluetoothDevice> devicelist = mBluetoothA2dp.getConnectedDevices();
                for(BluetoothDevice dev : devicelist) {
                    Log.i(Constants.TAG, "SPP : A2DP connected device : " + dev.getName() +"["+ dev.getAddress()+"]");

                    if ( dev.getName().contains("智咪") ){

                        /*如果是第二个智咪，主动断开*/
                        if ( isSppConnected()  ){
                            if ( mLastSppAddr != null && !dev.getAddress().contentEquals(mLastSppAddr) ) {

                                Log.i(Constants.TAG, "A2DP : 断开智咪..." + dev.getAddress());
                                A2dpManager a2dpManager = new A2dpManager();
                                a2dpManager.disconnect(dev);
                            }
                        }
                        else {
                            Log.i(Constants.TAG, "SPP : 检测到智咪" + dev.getAddress()+"auto_connect_spp = "+mIsAutoConnectSppEnabled);
                            //Toast.makeText(mContext, "detected iTalkie ...", Toast.LENGTH_SHORT).show();

                            //智咪刚配对，第一次连接
                            mLastDev = dev;
                            if (mIsAutoConnectSppEnabled) {
                                connectSpp(dev.getAddress());
                            } else {
                                //保存地址
                                mLastSppAddr = dev.getAddress();
                            }
                        }
                        mZmNotFoundInConnectedDevices = false;

                    }
                    else{
                        mZmNotFoundInConnectedDevices = true;
                    }
                }
                if (devicelist.isEmpty()){
                    mZmNotFoundInConnectedDevices = true;
                }


            }
        }
        public void onServiceDisconnected(int profile) {
            if (profile == BluetoothProfile.A2DP) {
                Log.d(Constants.TAG,"SPP : proxy disconnected");

                mBluetoothA2dp = null;
            }
        }
    };


    private void onA2dpConnected(){
        checkConnectivity();
    }

    public void checkConnectivity(){
        mBluetoothAdapter.getProfileProxy(mContext.getApplicationContext(), mProfileListener, BluetoothProfile.A2DP);

    }

    //for diagnose
    private boolean mZmNotFoundInConnectedDevices = true;
    private boolean mZmNotFoundInPairedDevices = true;

    public boolean isPaired(){
        return !mZmNotFoundInPairedDevices;
    }
    public boolean isConnected(){
        return !mZmNotFoundInConnectedDevices;
    }

}
