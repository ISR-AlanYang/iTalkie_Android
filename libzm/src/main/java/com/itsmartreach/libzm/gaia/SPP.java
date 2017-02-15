package com.itsmartreach.libzm.gaia;

import android.os.Handler;
import android.util.Log;

import com.itsmartreach.libzm.ZmCmdLink;
import com.itsmartreach.libzm.Constants;

import java.io.IOException;

/**
 * Created by Alan on 15/1/1.
 */
public class SPP {
    public static final int SPP_STATE_UNKNOWN = 0;
    public static final int SPP_STATE_CONNECTED = 1;
    public static final int SPP_STATE_DISCONNECTED = 2;


    public interface SppListener {
        void onSppStateChanged(int state);
        void onBatteryLevelChanged(int level);
        void onUserEvent(int event);
    }
    private SppListener mListener;


    public SPP(SppListener listener) {
        mListener = (SppListener)listener;

        initBtLink();
    }

    public void initBtLink(){
        //Log.i(Constants.TAG,"Audio : initBtLink create, mGaiaLink = "+mGaiaLink);
        if (mGaiaLink == null ) {
            mGaiaLink = new GaiaLink();
            if (mGaiaLink != null) {
                mGaiaLink.setReceiveHandler(mGaiaReceiveHandler);
            }
        }
    }

    public void connect(String btAddress) {

        mBtAddress = btAddress;

        if ( mBtAddress == null || mIsGaiaConnectionPending ){
            //Log.w(Constants.TAG,"Audio : GAIA connect abort due to connection pending ...");
            return;
        }

        if (mGaiaLink != null && !mIsGaiaConnected) {
            try {

                Log.d(Constants.TAG, "Audio | SPP : connecting to " + mBtAddress);

                mIsGaiaConnectionPending = true;
                mGaiaLink.connect(mBtAddress);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void disconnect() {

        //mListener.onSppStateChanged(SPP.SPP_STATE_DISCONNECTED);
        if (mGaiaLink != null && mIsGaiaConnected) {
            try {
                Log.v(Constants.TAG,"Audio | SPP : disconnect "+mBtAddress);

                mGaiaLink.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public boolean isConnected(){
        return mIsGaiaConnected;
    }

    public void requestBtDeviceBatteryLevel(){
        if (mGaiaLink != null && mIsGaiaConnected ){
            try {
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL);
                //mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_CURRENT_RSSI);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public void rebootBtDevice(){
        if (mGaiaLink != null && mIsGaiaConnected ){
            try {
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_DEVICE_RESET);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //-----------------------------------GAIA PART---------------------------------------------

    ////////////////////GIGA CONTROL PART////////////////////////////////////
    private String mBtAddress = "";
    private GaiaLink mGaiaLink;
    private GaiaCommand mCommand;
    private  boolean mIsGaiaConnected = false;
    private  boolean isLedOn = false;
    private  int mBattery;

    private Handler mReconnectionHandler = new Handler();
    private boolean mIsGaiaConnectionPending = false;

    Handler mGaiaReceiveHandler = new Handler() {
            /**
             * Handle all incoming messages received through GAIA from the rembooleanote device.
             * Basic messages for connect, disconnect and error are handled here.
             * Other messages are passed off to handleTheUnhandled() for processing.
             */
            @Override
            public void handleMessage(android.os.Message msg) {

                switch (GaiaLink.Message.valueOf(msg.what)) {
                    case DEBUG:
                        break;

                    case UNHANDLED:
                        handleTheUnhandled(msg);
                        break;

                    case CONNECTED:
                        mIsGaiaConnected = true;
                        mIsGaiaConnectionPending = false;
                        if ( mListener != null ){
                            mListener.onSppStateChanged(SPP_STATE_CONNECTED);
                        }

                        handleGaiaConnected();
                        Log.v(Constants.TAG, "SPP : Connected");

                        break;

                    case DISCONNECTED:
                        // Update the UI to show we are disconnected.
                        Log.v(Constants.TAG, "SPP : Disconnected");
                        mIsGaiaConnectionPending = false;
                        mIsGaiaConnected = false;
                        if ( mListener != null ){
                            mListener.onSppStateChanged(SPP_STATE_DISCONNECTED);
                        }

                        // Disconnect the link locally.
                        try {
                            mGaiaLink.disconnect();
                        } catch (IOException e) {
                            Log.d(Constants.TAG, "Disconnect failed: " + e.getMessage());
                        }
                        break;

                    case ERROR:
                        // Connection error received from GAIA library.
                        Log.e(Constants.TAG, "SPP : ERROR = " + ((Exception) msg.obj).toString());
                        mIsGaiaConnectionPending = false;
                        mIsGaiaConnected = false;
                        if ( mListener != null ){
                            mListener.onSppStateChanged(SPP_STATE_DISCONNECTED);
                        }

                        break;
                }
            }

            /**
             * Event handler triggered when the GAIA link connects.
             */
            private void handleGaiaConnected() {


                try {

                    mGaiaLink.registerNotification(Gaia.EventId.USER_ACTION);

                    //mGaiaLink.registerNotification(Gaia.EventId.CHARGER_CONNECTION);

                    //低压告警
                    //mGaiaLink.registerNotification(Gaia.EventId.BATTERY_LOW_THRESHOLD,10);

                    //信号差告警
                    //mGaiaLink.registerNotification(Gaia.EventId.RSSI_LOW_THRESHOLD,0xd6);
                    //mGaiaLink.registerNotification(Gaia.EventId.RSSI_LOW_THRESHOLD,0xf6);

                } catch (IOException e) {
                    Log.d(Constants.TAG, "..." + e.getMessage());
                }
                //
            }

            /**
             * Get information about the remote device.
             */
            private Runnable updateDeviceInfo = new Runnable() {
                public void run() {
                    //getDeveiceInfo();
                }
            };

            /**
             * Thread to reconnect the link.
             */
            private Runnable reconnectDevice = new Runnable() {
                public void run() {
                    if (!"".equalsIgnoreCase(mBtAddress)) {
                        Log.d(Constants.TAG, "SPP : reconnecting device: " + mBtAddress);

                        try {
                            mGaiaLink.connect(mBtAddress);
                        } catch (IOException e) {
                            Log.d(Constants.TAG, "SPP : Error when reconnecting device: " + e.getMessage());
                        }
                    }
                }
            };

            /**
             * Handle a command not handled by GaiaLink.
             *
             * @param msg The Message object containing the command.
             */
            private void handleTheUnhandled(android.os.Message msg) {
                mCommand = (GaiaCommand) msg.obj;

                Gaia.Status status = mCommand.getStatus();
                int command_id = mCommand.getCommand();

                // Handle acks for commands we have sent.
                if (mCommand.isAcknowledgement()) {
                    if (status == Gaia.Status.SUCCESS) {
                        //Log.d(Constants.TAG, "Audio : GAIA" + Integer.toHexString(mCommand.getVendorId()) + ":" + Integer.toHexString(mCommand.getCommandId()) + " = " + mCommand.getPayload().length);

                        // Act on an acknowledgement
                        switch (command_id) {
                            case Gaia.COMMAND_DEVICE_RESET:
                                try {
                                    if (mIsGaiaConnected) {
                                        mGaiaLink.disconnect();
                                    }
                                } catch (IOException e) {
                                    Log.d(Constants.TAG, "SPP : Disconnect failed: " + e.getMessage());
                                }
                                break;
                            //
                            case Gaia.COMMAND_POWER_OFF:
                                try {
                                    if (mIsGaiaConnected) {
                                        Log.v(Constants.TAG, "SPP : Power off");

                                        mGaiaLink.disconnect();
                                        //toast("Device reboot started, will reconnect it in 6 seconds");
                                        mIsGaiaConnected = false;
                                    }
                                } catch (IOException e) {
                                }
                                break;


                            case Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL:
                                if (mCommand.getByte(0) == 0x00) {
                                    mBattery = ((mCommand.getByte(1) & 0xff) << 8) | (mCommand.getByte(2) & 0xff);
                                } else {
                                    mBattery = 0;
                                }
                                Log.v(Constants.TAG, "SPP : battery = " + mBattery);
                                if ( mListener != null ){
                                    mListener.onBatteryLevelChanged(mBattery);
                                }
                                break;
                            case Gaia.COMMAND_GET_API_VERSION:
                                break;
                            case Gaia.COMMAND_GET_CURRENT_RSSI:
                                if (mCommand.getByte(0) == 0x00) {
                                    //mRssi = mCommand.getByte(1);
                                    Log.v(Constants.TAG, "SPP : RSSI = " + mCommand.getByte(1));

                                } else {
                                    //mRssi = 0;
                                    Log.v(Constants.TAG, "SPP : RSSI = 0" );

                                }
                                break;
                            case Gaia.COMMAND_GET_APPLICATION_VERSION: {
                                if (mCommand.getByte(0) == 0x00) {
                                    String ApplicationVersionValue = "";
                                    for (Byte data : mCommand.getPayload()) {
                                        String temp = Gaia.hexb(data);
                                        ApplicationVersionValue += "" + Integer.valueOf(temp, 16).intValue();
                                    }
                                }
                            }
                            break;
                            case Gaia.COMMAND_SET_LED_CONTROL:
                                //Log.d(Constants.TAG, "GAIA: Set LED control result");
                                if (mCommand.getByte(0) == 0x00) {
                                    try {
                                        mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_LED_CONTROL);
                                    } catch (IOException e) {
                                        //toast("Failed to get LED status");
                                    }
                                } else if (mCommand.getByte(0) == 0x05) {
                                }
                                break;
                            case Gaia.COMMAND_GET_LED_CONTROL:
                                if (mCommand.getByte(0) == 0x00) {
                                    //Log.d(Constants.TAG, "GAIA: Get LED successfully");
                                    if (mCommand.getByte(1) == 0x00)
                                        isLedOn = false;
                                    else if (mCommand.getByte(1) == 0x01)
                                        isLedOn = true;

                                }
                                break;

                            case Gaia.COMMAND_PLAY_TONE:
                                if (mCommand.getByte(0) == 0x00) {
                                } else if (mCommand.getByte(0) == 0x05) {
                                    Log.d(Constants.TAG, "SPP : Invalid Parameter");
                                }
                                break;

                            case Gaia.COMMAND_SET_DEFAULT_VOLUME:
                                Log.d(Constants.TAG, "SPP : Set default vol control result");
                                if (mCommand.getByte(0) == 0x00) {
                                    try {
                                        mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_DEFAULT_VOLUME);
                                    } catch (IOException e) {
                                        Log.d(Constants.TAG, "Failed when getting default vol control: " + e.getMessage());
                                    }
                                } else if (mCommand.getByte(0) == 0x05) {
                                    Log.d(Constants.TAG, "Invalid Parameter");
                                }
                                break;

                            case Gaia.COMMAND_REGISTER_NOTIFICATION: {
                                if (mCommand.getByte(0) == 0x00) {
                                    Log.d(Constants.TAG, "Register notification successfully");
                                } else if (mCommand.getByte(0) == 0x05) {
                                    Log.d(Constants.TAG, "Register notification not successfully");
                                }
                            }
                            break;
                        }
                    } else {
                        // Acknowledgement received with non-success result code,
                        // so display the friendly message as a toast and dismiss any dialogs.
                        Log.w(Constants.TAG, "Error from remote device: command_id=" + command_id + ",status=" + Gaia.statusText(status));

                    }
                } else if (command_id == Gaia.COMMAND_EVENT_NOTIFICATION) {
                    Gaia.EventId event_id = mCommand.getEventId();
                    //Log.v(Constants.TAG, "Audio | GAIA  : Event " + event_id.toString());

                    switch (event_id) {
                        case BATTERY_LOW_THRESHOLD:
                            //Todo
                            break;
                        case CHARGER_CONNECTION:
                            break;

                        case USER_ACTION:
                            int user_action = mCommand.getShort(1);
                            //Log.v(Constants.TAG, String.format("Audio | GAIA : HS Event 0x%04X", user_action) );
                            if ( mListener != null ){
                                mListener.onUserEvent(user_action);
                            }
                            break;
                        case AV_COMMAND:
                            Log.d(Constants.TAG, "Audio : AV command : " + new String(mCommand.getPayload()));
                            break;
                        case DEVICE_STATE_CHANGED:
                            Log.d(Constants.TAG, "Audio : current state : " + new String(mCommand.getPayload()));
                            break;
                        case DEBUG_MESSAGE:
                            Log.d(Constants.TAG, "Audio : DEBUG_MESSAGE : " + new String(mCommand.getPayload()));
                            break;
                        case KEY:
                            Log.d(Constants.TAG, "Audio : Key Event " + event_id.toString());
                            break;
                        default:
                            Log.w(Constants.TAG, "Audio : unknown Event " + event_id.toString());
                            break;
                    }

                    sendAcknowledgement(mCommand, Gaia.Status.SUCCESS, event_id);
                }
            }
        };


    private void sendAcknowledgement(GaiaCommand command, Gaia.Status success, Gaia.EventId event_id) {
        sendAcknowledgement(command, success, event_id.ordinal());
    }

    private void sendAcknowledgement(GaiaCommand command, Gaia.Status status, int... payload) {
        try {
            mGaiaLink.sendAcknowledgement(command, status, payload);
        }
        catch (IOException e) {
            Log.w(Constants.TAG, e.toString());
        }
    }
/*
    public void turnOnAmplifier( boolean isTurnOn ){
        try {
            if ( mGaiaLink != null && mIsGaiaConnected){
                Log.v(Constants.TAG,"Audio : "+(isTurnOn?"------Amplifier ON------":"======Amplifier OFF======"));
                mGaiaLink.sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_SET_LED_CONTROL, isTurnOn ? 0 : 1);
            }
        }
        catch (IOException e) {
            Log.e(Constants.TAG, "Audio : GAIA Failure when sending LED set command: " + e.getMessage());
        }
    }
    */

}
