package com.itsmartreach.libzm.gaia;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Set;
import java.util.UUID;

import com.itsmartreach.libzm.Constants;
import com.itsmartreach.libzm.gaia.Gaia.Status;


import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

public class GaiaLink
{
    private static final String TAG = "SPP";
    
    private static final UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final UUID GAIA_UUID = UUID.fromString("A5E648B6-374D-422D-A7DF-92259D4E7817");

    private static final String NOT_CONNECTED = "SPP is not connected";
    
    private DataConnectionListener dataConnectionListener;   
    
    /**
     * Allows registering for updates on the GaiaLink connection status.
     * @param dataConnectionListener An object that implements the DataConnectionListener interface.
     */
    public void setDataConnectionListener(DataConnectionListener dataConnectionListener) {
		this.dataConnectionListener = dataConnectionListener;
	}

    /**
     * Called when the data transfer status changes, to update the listener.
     * @param isDataTransferInProgress True if a data transfer is in progress.
     */
    public void updateDataConnectionStatus(boolean isDataTransferInProgress)
    {
    	if(dataConnectionListener != null)
    	{
    		dataConnectionListener.update(isDataTransferInProgress);
    	}    	
    }
    
	//  Gateway address from the emulator will always be 10.0.2.2; the port might change if
    //  we need to support multiple instances.  You need to redirect the host port like so:
    //      redir add udp:7701:7701
    //      udplistener com9 7700 7701

    private static final String GW_ADDRESS = "10.0.2.2";
    private static final int GW_PORT_OUT = 7700;
    private static final int GW_PORT_IN = 7701;
        
    private static final int META_CONNECT = 0x2001;

    // Static fields exposed publicly
    
    // Maximum number of bytes in the payload portion of a GAIA packet.
    public static final int MAX_PACKET_PAYLOAD = 254;
    
    public static final int PACKET_HEADER_SIZE = 8;
    
    public static enum Message
    {
        UNHANDLED,
        CONNECTED,
        ERROR,
        DEBUG,
        DISCONNECTED;
        
        public static Message valueOf(int what)
        {
            if (what < 0 || what >= Message.values().length)
                return null;
            
            return Message.values()[what];
        }
    }
    
    public static enum Transport
    {
        BT_SPP,
        BT_GAIA,
        INET_UDP;
    }
    
   // End of public fields
    
    private final int MAX_BUFFER = 1024;
    
    private boolean mDebug = false;
    private boolean mVerbose = false;
    
    private BluetoothAdapter mBTAdapter = null;
    private BluetoothDevice mBTDevice = null;
    
    private DatagramSocket mDatagramSocket = null;
    private BluetoothSocket mBTSocket = null;
    
    private InputStream mInputStream = null;
    private Reader mReader;
    private Connector mConnector;
    private Handler mReceiveHandler = null;
    private Handler mLogHandler = null;
    private Handler mDeviceSearchHandler = null;
        
    private Transport mTransport = Transport.BT_SPP;
    private boolean mIsListening = false;
    private boolean mIsConnected = false;
    private BluetoothServerSocket mListener;
    
    private BroadcastReceiver mReceiver;
    private Context mContext;
    
	public void setContext(Context context) {
		this.mContext = context;
	}

	/**
     * Class constructor.
     */
    public GaiaLink()
    {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
    }
    
    
    /**
     * Class constructor specifying transport to use.
     * @param t The transport to use
     */
    public GaiaLink(Transport t)
    {
        mBTAdapter = BluetoothAdapter.getDefaultAdapter();
        mTransport = t;
    }
    
    /**
     * Sets the transport to use.
     * @param t The transport to use
     * @throws IOException
     */
    public void setTransport(Transport t) throws IOException
    {
        if (mIsConnected)
            throw new IOException("Incorrect state");
        
        else
            mTransport = t;
    }
    
    /**
     * Returns the transport used
     * @return The transport used
     */
    public Transport getTransport()
    {
        return mTransport;
    }
    
    
    /**
     * Validate a Bluetooth device address
     * @param address The address to check
     * @return true if the passed address is syntactically correct
     */
    public static boolean checkBluetoothAddress(String address)
    {
        return BluetoothAdapter.checkBluetoothAddress(address);
    }
    
    
    /**
     * Establishes an outbound connection to the specified device.
     * @param address Bluetooth or IP address of the remote device depending on the transport.
     * @throws IOException
      */
    public void connect(String address) throws IOException
    {
        if (mIsListening || mIsConnected)
            throw new IOException("Incorrect state");
        
        switch (mTransport)
        {
        case BT_SPP:
        case BT_GAIA:
            connectBluetooth(address);
            break;
            
        case INET_UDP:
            connectUdp(address);
            break;
        }       
    }
    
    
    /**
     * Listens for an an inbound connection.
     * @throws IOException
     */
    public void listen() throws IOException
    {
        if (mIsListening || mIsConnected)
            throw new IOException("Incorrect state");
        
        switch (mTransport)
        {
        case BT_SPP:
        case BT_GAIA:
            listenBluetooth();
            break;
            
        case INET_UDP:
            listenUdp();
            break;
        }               
    }
    
    /**
     * Set the debug level which controls how verbose we are with debugging information sent to logcat.
     * @param level The debug level. Set to zero for no debug logging, 1 for standard debug messages, 2 for verbose debug messages. 
     */
    public void setDebugLevel(int level)
    {
        mDebug = level > 0;
        mVerbose = level > 1;
    }
    
    /**
     * Listen for packets on the current transport.
     * @throws IOException
     */
    private void listenBluetooth() throws IOException
    {
        if (mDebug) Log.i(TAG, "listenBluetooth");
        
        switch (mTransport)
        {
        case BT_GAIA:
            mListener = listenUsingUuid("Gaia", GAIA_UUID);
            break;
            
        case BT_SPP:
            mListener = listenUsingUuid("Gaia SPP", SPP_UUID);
            break;
            
        default:
            throw new IOException("Unsupported Transport " + mTransport.toString());
        }
        
        mReader = new Reader();
        mReader.start();
        
        mIsListening = true;
    }
    
    
    /**
     * Start listening on RFCOMM socket
     * @param name SDP record service name.
     * @param uuid UUID for SDP record.
     * @return Listening socket.
     * @throws IOException
     */
    private BluetoothServerSocket listenUsingUuid(String name, UUID uuid) throws IOException
    {
        if (btIsSecure())
        {	
            return mBTAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
        }
        else
        {	
            return mBTAdapter.listenUsingRfcommWithServiceRecord(name, uuid);
        }    
    }

    /**
     * Check for RFCOMM security.
     * @return True if RFCOMM security is implemented.
     */
    private boolean btIsSecure()
    {
    //  Establish if RFCOMM security is implemented, in which case we'll
    //  use the insecure variants of the RFCOMM functions
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1;
    }


    /**
     * Connectionless connection.
     */
    private void listenUdp()
    {
        if (mDebug) Log.i(TAG, "listenUdp");

        mReader = new Reader();
        mReader.start();
        
        mIsListening = true;
    }
    
    
    /**
     * Create a Bluetooth connection.
     * @param address Remote Bluetooth address to connect to.
     * @throws IOException
     */
    private void connectBluetooth(String address) throws IOException
    {
        
    	String upper_address = address.toUpperCase();
        
        if (!getBluetoothAvailable())
            throw new IOException("Bluetooth is not available");
        
        else if (!BluetoothAdapter.checkBluetoothAddress(upper_address))
            throw new IOException("Illegal Bluetooth address");
        
        else
        {   
            if (mDebug) Log.i(TAG, "connect BT " + address);
            
            mBTDevice = mBTAdapter.getRemoteDevice(upper_address);
            
            switch (mTransport)
            {
            case BT_GAIA:
                mBTSocket = createSocket(GAIA_UUID);
                break;
                
            case BT_SPP:
                mBTSocket = createSocket(SPP_UUID);
                break;
               
            default:
                throw new IOException("Unsupported Transport " + mTransport.toString());
            }
            
            mConnector = new Connector();
            mConnector.start();
       }
    }

    
    /**
     * Create the RFCOMM bluetooth socket.
     * @param uuid UUID to create the socket with.
     * @return BluetoothSocket object.
     * @throws IOException
     */
    private BluetoothSocket createSocket(UUID uuid) throws IOException
    {
        BluetoothSocket socket = null;
        try{
            if (btIsSecure())
            {
                socket = mBTDevice.createRfcommSocketToServiceRecord(uuid);
            }
            else
            {
                socket = mBTDevice.createRfcommSocketToServiceRecord(uuid); 
            }
        }catch(IOException e){

            Log.w(Constants.TAG,"Audio : GAIA createRfcommSocketToServiceRecord failed, try createRfcommSocket");
            
            try {
                // This is a workaround that reportedly helps on some older devices like HTC Desire, where using
                // the standard createRfcommSocketToServiceRecord() method always causes connect() to fail.
                Method method = mBTDevice.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
                socket = (BluetoothSocket) method.invoke(mBTDevice, Integer.valueOf(1));
                return socket;
            } catch (NoSuchMethodException e1) {
                e1.printStackTrace();
                throw new IOException();
            } catch (IllegalArgumentException e1) {
                e1.printStackTrace();
                throw new IOException();
            } catch (IllegalAccessException e1) {
                e1.printStackTrace();
                throw new IOException();
            } catch (InvocationTargetException e1) {
                e1.printStackTrace();
                throw new IOException();
            }
        }
        
        return socket;
    }


    /**
     * Make a UDP connection.
     * @param address IP address to connect to.
     * @throws IOException
     */
    private void connectUdp(String address) throws IOException
    {
        mDatagramSocket = new DatagramSocket();
        mDatagramSocket.connect(InetAddress.getByName(GW_ADDRESS), GW_PORT_OUT);
        
        mReader = new Reader();
        mReader.start();
        int bd[] = new int[6];

        if (mDebug) Log.i(TAG, "connect UDP " + address);

        for (int idx = 0; idx < 6; ++idx)
            bd[idx] = Integer.valueOf(address.toUpperCase().substring(3 * idx, 3 * idx + 2), 16);
        
        sendCommand(Gaia.VENDOR_CSR, META_CONNECT, bd[0], bd[1], bd[2], bd[3], bd[4], bd[5]);
    }
    
    
    /**
     * Set the enabled state of the bluetooth adapter.
     * @param enabled True if Bluetooth should be enabled.
     */
    public void setBluetoothEnabled(boolean enabled)
    {
        if (mBTAdapter != null)
        { 
            if (enabled)
                mBTAdapter.enable();
            
            else
                mBTAdapter.disable();
        }
    }
    
    
    /**
     * Returns the availability of Bluetooth
     * @return true if the local Bluetooth adapter is available
     */
    public boolean getBluetoothAvailable()
    {
    /*  Bluetooth is available only if the adapter exists  */
        return mBTAdapter != null;
    }
    
    /**
     * Returns the enabled state of Bluetooth
     * @return true if the local Bluetooth adapter is enabled
     */
    public boolean getBluetoothEnabled()
    {
        return (mBTAdapter != null) && mBTAdapter.isEnabled();
    }
    
    
    /**
     * Disconnects from the remote device.
     * @throws IOException
     */
    public void disconnect() throws IOException
    {
        switch (mTransport)
        {
        case BT_SPP:
        case BT_GAIA:
            disconnectBluetooth();
            break;
            
        case INET_UDP:
            disconnectUdp();
            break;
        }
        
        mIsConnected = false;
        
		if(mContext != null)
			mContext.unregisterReceiver(mReceiver);
    }
    
    /**
     * Disconnect Bluetooth device.
     * @throws IOException
     */
    private void disconnectBluetooth() throws IOException
    {
        if (mDebug) Log.i(TAG, "Audio : GAIA disconnect BT");
        
        if (mBTSocket != null)
        {
        //  HTC SPP disconnection is buggy; ask the other end to do it for us
        //  sendCommand(VENDOR_CSR, META_DISCONNECT);
            mReader = null;
            
            if (mInputStream != null)
                mInputStream.close();
            
            if (mBTSocket.getOutputStream() != null)
                mBTSocket.getOutputStream().close();
            
            mBTSocket.close();

            mBTSocket = null;
            mBTDevice = null;
            
            updateDataConnectionStatus(false);
        }
    }
    
    /**
     * Close UDP socket.
     * @throws IOException
     */
    private void disconnectUdp() throws IOException
    {
        if (mDebug) Log.i(TAG, "disconnect UDP");
        
        if (mDatagramSocket != null)
        {
            mDatagramSocket.disconnect();
            mDatagramSocket.close();
        }
    }
    
    /**
     * Sends a Gaia command to the remote device.
     * @param vendor_id The vendor identifier qualifying the command.
     * @param command_id    The command identifier.
     * @param payload   Array of command-specific bytes.
     * @param payload_length    The number of payload bytes to send.
     * @throws IOException
     */
    public void sendCommand(int vendor_id, int command_id, byte[] payload, int payload_length) throws IOException
    {
        byte[] data = Gaia.frame(vendor_id, command_id, payload, payload_length);
        
        if (mLogHandler != null)
        {
            String text = "\u2192 " + Gaia.hexw(vendor_id) + " " + Gaia.hexw(command_id);
            
                for (int i = 0; i < payload.length; ++i)
                    text += " " + Gaia.hexb(payload[i]);
            
            Log.d(TAG, text);
            mLogHandler.obtainMessage(Message.DEBUG.ordinal(), text).sendToTarget();
        }
        
        sendCommandData(data);
    }
    
    /**
     * Write data to the Bluetooth or datagram socket.
     * @param data Array of bytes to send.
     * @throws IOException
     */
    private void sendCommandData(byte[] data) throws IOException
    {
        switch (mTransport)
        {
        case BT_SPP:
        case BT_GAIA:
            if (mBTSocket == null)
                throw new IOException(NOT_CONNECTED);
            
            if (mDebug) Log.v(TAG, "send " + data.length);
            mBTSocket.getOutputStream().write(data);
            break;
            
        case INET_UDP:
            if (mDatagramSocket == null)
                throw new IOException(NOT_CONNECTED);
            
            mDatagramSocket.send(new DatagramPacket(data, data.length));
            break;
        }
    }


    /**
     * Sends a Gaia command to the remote device.
     * @param vendor_id The vendor identifier qualifying the command.
     * @param command_id    The command identifier.
     * @param payload   Array of command-specific bytes.
     * @throws IOException
     */
    public void sendCommand(int vendor_id, int command_id, byte[] payload) throws IOException
    {
        if (payload == null)
            sendCommand(vendor_id, command_id);

        else
            sendCommand(vendor_id, command_id, payload, payload.length);
    }

    /**
     * Sends a Gaia command to the remote device.
     * @param vendor_id The vendor identifier qualifying the command.
     * @param command_id    The command identifier.
     * @param param...  Command-specific integers.
     * @throws IOException
     */
    public void sendCommand(int vendor_id, int command_id, int... param) throws IOException
    {
        if (param == null || param.length == 0)
        {
            byte[] data = Gaia.frame(vendor_id, command_id);
            
            if (mLogHandler != null)
            {
                String text = "\u2192 " + Gaia.hexw(vendor_id) + " " + Gaia.hexw(command_id);
                Log.d(TAG, text);
                mLogHandler.obtainMessage(Message.DEBUG.ordinal(), text).sendToTarget();
            }
            
            sendCommandData(data);
        }
        
        
        else
        {
        //  Convenient but involves copying the payload twice.  It's usually short. 
            byte[] payload; 
            payload = new byte[param.length];

            for (int idx = 0; idx < param.length; ++idx)
                payload[idx] = (byte) param[idx];
        
            sendCommand(vendor_id, command_id, payload);
        }
    }
    
    
    /**
     * Sends a Gaia enable-style command to the remote device.
     * @param vendor_id The vendor identifier qualifying the command.
     * @param command_id    The command identifier.
     * @param enable Enable (true) or disable (false).
     * @throws IOException
     */
    public void sendCommand(int vendor_id, int command_id, boolean enable) throws IOException
    {
        sendCommand(vendor_id, command_id, enable ? Gaia.FEATURE_ENABLED : Gaia.FEATURE_DISABLED);
    }
    
    
    /**
     * Sends a Gaia acknowledgement to the remote device.
     * @param vendor_id The vendor identifier qualifying the command.
     * @param command_id    The command identifier.
     * @param status    The status of the command.
     * @param param...  Acknowledgement-specific integers.
     * @throws IOException
     */
    public void sendAcknowledgement(int vendor_id, int command_id, Gaia.Status status, int... param) throws IOException
    {
    //  Convenient but involves copying the payload twice.  It's usually short. 
        byte[] payload; 
        
        if (param == null)
            payload = new byte[1];
        
        else
        {
            payload = new byte[param.length + 1];
            
            for (int idx = 0; idx < param.length; ++idx)
                payload[idx + 1] = (byte) param[idx];
        }
       
        payload[0] = (byte) status.ordinal();
        sendCommand(vendor_id, command_id | Gaia.ACK_MASK, payload);
    }
    
    
    /**
     * Sends a Gaia acknowledgement to the remote device, assuming the vendor id is CSR
     * @param command_id The command identifier.
     * @param status The status of the command.
     * @param param... Acknowledgement-specific integers.
     * @throws IOException
     */
    public void sendAcknowledgement(int command_id, Gaia.Status status, int... param) throws IOException
    {
        sendAcknowledgement(Gaia.VENDOR_CSR, command_id, status, param);
    }

    public void sendAcknowledgement(GaiaCommand command, Status status) throws IOException
    {
        sendAcknowledgement(command.getVendorId(), command.getCommandId(), status);
    }
    
    public void sendAcknowledgement(GaiaCommand command, Status status, int... payload) throws IOException
    {
        sendAcknowledgement(command.getVendorId(), command.getCommandId(), status, payload);
    }

    
    /**
     * Requests the device to perform no operation (other
     * than to send an acknowledgement packet). 
     * @throws IOException 
     */
    public void noOperation() throws IOException
    {
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_NO_OPERATION);       
    }

    
    /**
     * Requests the device's Protocol and API versions
     * @throws IOException 
     */
    public void getAPIVersion() throws IOException
    {
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_API_VERSION);       
    }

    
    /**
     * Requests the current Received Signal Strength Indication
     * at the remote device.
     * @throws IOException 
     */
    public void getCurrentRSSI() throws IOException
    {
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_CURRENT_RSSI);       
    }

    /**
     * Requests the current battery level at the remote device.
     * @throws IOException 
     */
    public void getCurrentBatteryLevel() throws IOException
    {
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_CURRENT_BATTERY_LEVEL);       
    }

    
    /**
     * Requests the Module Id of the remote device.
     * @throws IOException 
     */
    public void getModuleId() throws IOException
    {
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_MODULE_ID);       
    }

    
    /**
     * Requests the Application Version of the remote device.
     * The acknowledgement payload contains an eight-octet
     * application version identifier optionally followed by 
     * null-terminated human-readable text.
     * @throws IOException 
    */
    public void getApplicationVersion() throws IOException
    {
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_APPLICATION_VERSION);       
    }

    
    /**
     * Enables or disables LED indicators on the device
     * @param enable    Sets the enabled state
     * @throws IOException 
     */
    public void setLEDControl(boolean enable) throws IOException
    {
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_SET_LED_CONTROL, enable);          
    }

    
    /**
     * Requests the current state of LED enable
     * @throws IOException 
     */
    public void getLEDControl() throws IOException
    {
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_LED_CONTROL);          
    }

    
    /**
     * Enables or disables voice prompts on the device
     * @param enable    Sets the enabled state
     * @throws IOException 
     */
    public void setVoicePromptControl(boolean enable) throws IOException
    {
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_SET_VOICE_PROMPT_CONTROL, enable);          
    }
   
    
    /**
     * Requests the current state of voice prompt control
     * @throws IOException 
     */
    public void getVoicePromptControl() throws IOException
    {
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_VOICE_PROMPT_CONTROL);          
    }
   
    
    /**
     * Requests notification of the given event
     * @param event   The Event for which notifications are to be raised
     * @throws IOException 
     */
    public void registerNotification(Gaia.EventId event) throws IOException, IllegalArgumentException
    {
        byte[] args;
        
        switch (event)
        {
        case START:
        case DEVICE_STATE_CHANGED:
        case DEBUG_MESSAGE:
        case BATTERY_CHARGED:
        case CHARGER_CONNECTION:
        case CAPSENSE_UPDATE:
        case USER_ACTION:
        case SPEECH_RECOGNITION:
        case AV_COMMAND:
        case REMOTE_BATTERY_LEVEL:
           args = new byte[1];
            break;
            
        default:
            throw new IllegalArgumentException();
        }
               
        args[0] = (byte) event.ordinal();
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_REGISTER_NOTIFICATION, args);
    }
        
    
    /**
     * Requests notification of the given event
     * @param event   The Event for which notifications are to be raised
     * @param level The level at which events are to be raised 
     * @throws IOException 
     */
    public void registerNotification(Gaia.EventId event, int level) throws IOException, IllegalArgumentException
    {
        byte[] args;
        
        switch (event)
        {
        case RSSI_LOW_THRESHOLD:
        case RSSI_HIGH_THRESHOLD:
            args = new byte[2];
            args[1] = (byte) level;
            break;
            
        case BATTERY_LOW_THRESHOLD:
        case BATTERY_HIGH_THRESHOLD:
            args = new byte[3];
            args[1] = (byte) (level >>> 8);
            args[2] = (byte) level;
            break;

        case PIO_CHANGED:
            args = new byte[5];
            args[1] = (byte) (level >>> 24);
            args[2] = (byte) (level >>> 16);
            args[3] = (byte) (level >>> 8);
            args[4] = (byte) level;
            break;

        default:
            throw new IllegalArgumentException();
        }
               
        args[0] = (byte) event.ordinal();
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_REGISTER_NOTIFICATION, args);
    }

    
    /**
     * Requests notification of the given event
     * @param event   The Event for which notifications are to be raised
     * @param level1 The first level at which events are to be raised 
     * @param level2 The second level at which events are to be raised 
     * @throws IOException 
     */
    public void registerNotification(Gaia.EventId event, int level1, int level2) throws IOException, IllegalArgumentException
    {
        byte[] args;
        
        switch (event)
        {
        case RSSI_LOW_THRESHOLD:
        case RSSI_HIGH_THRESHOLD:
            args = new byte[3];
            args[1] = (byte) level1;
            args[2] = (byte) level2;
            break;
            
        case BATTERY_LOW_THRESHOLD:
        case BATTERY_HIGH_THRESHOLD:
            args = new byte[5];
            args[1] = (byte) (level1 >>> 8);
            args[2] = (byte) level1;
            args[3] = (byte) (level2 >>> 8);
            args[4] = (byte) level2;
            break;

        default:
            throw new IllegalArgumentException();
        }
               
        args[0] = (byte) event.ordinal();
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_REGISTER_NOTIFICATION, args);
    }

    
    /**
     * Requests the status of notifications for the given event
     * @param event   The Event for which the status is requested
     * @throws IOException 
     */
    public void getNotification(Gaia.EventId event) throws IOException
    {
        byte[] args = new byte[1];
        args[0] = (byte) event.ordinal();
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_GET_NOTIFICATION, args);
    }
        
    
    /**
     * Cancels notification of the given event
     * @param event   The Event for which notifications are no longer to be raised
     * @throws IOException 
     */
    public void cancelNotification(Gaia.EventId event) throws IOException
    {
        byte[] args = new byte[1];
        args[0] = (byte) event.ordinal();
        sendCommand(Gaia.VENDOR_CSR, Gaia.COMMAND_CANCEL_NOTIFICATION, args);
    }
        
    
    /**
     * Sets the target for Gaia messages received from the remote device.
     * @param handler   The Handler for Gaia messages received from the remote device. 
     */
    public void setReceiveHandler(Handler handler)
    {
        mReceiveHandler = handler;
    }
    
    /**
     * Returns the friendly name of the remote device or null if there is none.
     * @return Friendly name as a string.
     */
    public String getName()
    {
        return mBTDevice.getName();
    }
    
    /**
     * Get list of paired devices.
     * @return A list of paired devices delimited with line feeds.
     */
    public CharSequence[] getClientList()
    {
        CharSequence[] client_list;
        
        if (mBTAdapter == null)
            client_list = new CharSequence[0];           
        else
        {
            Set<BluetoothDevice> pdl = mBTAdapter.getBondedDevices();
            client_list = new CharSequence[pdl.size()];
            int count = 0;
            
            for (BluetoothDevice device : pdl) 
                client_list[count++] = device.getName() + "\n" + device.getAddress();
        }
        
        return client_list;
    }
    
    /**
     * Sets the target for debugging log messages.
     * @param handler The Handler for debugging messages received from the remote device. 
     */
    public void setLogHandler(Handler handler)
    {
        mLogHandler = handler;
    }
    
    
    /**
     * Log a hex string representation of a commands payload bytes to the log handler.
     * @param command The command to log.
     */
    private void logCommand(GaiaCommand command)
    {
       if (mLogHandler != null)
       {
            String text = "\u2190 " + Gaia.hexw(command.getVendorId()) + " " + Gaia.hexw(command.getCommandId());
            
            if (command.getPayload() != null)
                for (int i = 0; i < command.getPayload().length; ++i)
                    text += " " + Gaia.hexb(command.getPayload()[i]);
            
            if (mDebug) Log.d(TAG, text);
            mLogHandler.obtainMessage(Message.DEBUG.ordinal(), text).sendToTarget();
        }
    }

    /**
     * Thread to connect the Bluetooth socket and start the thread that reads from the socket.
     *
     */
    private class Connector extends Thread
    {        
        public void run() 
        {            
            try
            {
                //Log.d(Constants.TAG,"Audio : GAIA cancelDiscovery ...");
                mBTAdapter.cancelDiscovery();
                //Log.d(Constants.TAG,"Audio : GAIA connect ...");
                mBTSocket.connect();
                mInputStream = mBTSocket.getInputStream();
                mReader = new Reader();
                mReader.start();
            }
            
            catch (Exception e)
            {
                if (mDebug) Log.e(TAG, "connector: " + e.toString());
                mReceiveHandler.obtainMessage(Message.ERROR.ordinal(), e).sendToTarget();
            }
        }
    }
    
    /**
     * Thread to read incoming packets from SPP, GAIA or UDP.
     */
    private class Reader extends Thread
    {        
        byte[] packet = new byte[Gaia.MAX_PACKET];
        int flags;
        int packet_length = 0;
        int expected = Gaia.MAX_PAYLOAD;
        
        boolean going;
        DatagramSocket rx_socket = null;


        public void run() 
        {            
            switch (mTransport)
            {
            case BT_SPP:
            case BT_GAIA:
                runSppReader();
                break;
                
            case INET_UDP:
                runUdpReader();
                break;
            }
            
            if (mReceiveHandler == null)
                Log.e(TAG, "reader: no receive handler");
            
            else
                mReceiveHandler.obtainMessage(Message.DISCONNECTED.ordinal()).sendToTarget();
        }
        
        private void runSppReader()
        {
            byte[] buffer = new byte[MAX_BUFFER];            
            int bytes;
            
            going = false;
            
            Log.v(TAG,"Audio : GAIA runSppReader start...");
            
            if (mIsListening)
            {
                try
                {
                	mBTSocket = mListener.accept();
                    mInputStream = mBTSocket.getInputStream();
                    mReceiveHandler.obtainMessage(Message.CONNECTED.ordinal(), mBTDevice.getAddress()).sendToTarget();
                    mIsConnected = true;
                    mIsListening = false;
                    going = true;
                }
                
                catch (IOException e) 
                {        
                    if (mDebug) Log.e(TAG, "runSppReader: accept: " + e.toString());
                    mReceiveHandler.obtainMessage(Message.ERROR.ordinal(), e).sendToTarget();
                    going = false;                
                }  
            }
            
            else
            {
                mReceiveHandler.obtainMessage(Message.CONNECTED.ordinal(), mBTDevice.getAddress()).sendToTarget();
                mIsConnected = true;
                going = true;
            }
            
            while (going) 
            {                
            	try 
                {
                    if ( mInputStream != null ) {
                        bytes = mInputStream.read(buffer);
                    }
                    else {
                        bytes = -2;
                    }

                    if (bytes < 0) {
                        going = false;
                    } else {
                        scanStream(buffer, bytes);
                        updateDataConnectionStatus(true);
                    }

                } 
                catch (IOException e)
                {        
                    if (mDebug) Log.e(TAG, "Audio : GAIA runSppReader: read: " + e.toString());
                    going = false;
                }  
            }
            
        }
                
        private void runUdpReader()
        {
            going = false;
            
            if (mReceiveHandler == null)
            {
                if (mDebug) Log.e(TAG, "No receive_handler");
            }
            
            else
            {
                byte[] buffer = new byte[MAX_BUFFER];            
                DatagramPacket packet = new DatagramPacket(buffer, MAX_BUFFER);
                int bytes;            
                
                try
                {
                    rx_socket = new DatagramSocket(GW_PORT_IN);
                    if (mDebug) Log.i(TAG, "rx skt on " + GW_PORT_IN);
                    going = true;
                }
                
                catch (Exception e)
                {
                    if (mDebug) Log.e(TAG, "runUdpReader: " + e.toString());
                    e.printStackTrace();
                //  going = false;
                }
                
                while (going)
                {
                    try
                    {
                        rx_socket.receive(packet);
                        bytes = packet.getLength();
 
                        if (mVerbose) Log.i(TAG, "rx " + bytes);
                        
                        if (bytes < 0)
                            going = false;
                        
                        else
                            scanStream(buffer, bytes);
                    }
                    
                    catch (IOException e)
                    {
                        if (mDebug) Log.e(TAG, "runUdpReader: " + e.toString());
                        e.printStackTrace();
                        going = false;
                    }
                }
                
                if (mDebug) Log.e(TAG, "going exit");
            }
        }
        
        private void scanStream(byte[] buffer, int length)
        {
            for (int i = 0; i < length; ++i)
            {
                if ((packet_length > 0) && (packet_length < Gaia.MAX_PACKET))
                {
                    packet[packet_length] = buffer[i];
                    
                    if (packet_length == Gaia.OFFS_FLAGS)
                         flags = buffer[i];
                    
                    else if (packet_length == Gaia.OFFS_PAYLOAD_LENGTH)
                    {
                        expected = buffer[i] + Gaia.OFFS_PAYLOAD + (((flags & Gaia.FLAG_CHECK) != 0) ? 1 : 0);
                        if (mVerbose) Log.d(TAG, "expect " + expected);
                    }
                    
                    ++packet_length;
                   
                    if (packet_length == expected)
                    {    
                        if (mVerbose) Log.d(TAG, "got " + expected);
                        
                        if (mReceiveHandler == null)
                        {
                            if (mDebug) Log.e(TAG, "No receiver");
                        }
                        
                        else
                        {
                            GaiaCommand command = new GaiaCommand(packet, packet_length);
                            logCommand(command);
                            
                            if (command.getEventId() == Gaia.EventId.START && !mIsConnected)
                            {
                                if (mDebug) Log.i(TAG, "start");
                                mReceiveHandler.obtainMessage(Message.CONNECTED.ordinal(), mBTDevice.getAddress()).sendToTarget();
                                mIsConnected = true;
                            }
                            
                            else
                            {
                                if (mDebug) Log.v(TAG, "unhandled " + Gaia.hexw(command.getCommand()));
                                mReceiveHandler.obtainMessage(Message.UNHANDLED.ordinal(), command).sendToTarget();
                            }
                        }
                        
                        packet_length = 0;
                        expected = Gaia.MAX_PAYLOAD;
                    }             
                 }
                
                else if (buffer[i] == Gaia.SOF)
                    packet_length = 1;
            }
        }
    }

    /**
     * Obtain the address of the Bluetooth device.
     * @return String representing the Bluetooth device address.
     */
    public String getBluetoothAddress()
    {
        return mBTDevice.getAddress();
    }


    /**
     * Obtain the BluetoothDevice object.
     * @return BluetoothDevice object.
     */
    public BluetoothDevice getBluetoothDevice()
    {
        return mBTDevice;
    }
    
    
    /**
     * Set handler for scan responses.
     * @param handler Handler object to accept scan responses.
     */
    public void setDeviceSearchHandler(Handler handler)
    {
        this.mDeviceSearchHandler = handler;
    }
    
    /**
     * Start device discovery and handle responses by sending to the device
     * search handler which should have been registered via setDeviceSearchHandler() 
     * before calling this method.
     */
    public void scanDevice()
    {
    	mBTAdapter.startDiscovery();
    	mReceiver = new BroadcastReceiver() {
    		
		@Override
		public void onReceive(Context context, Intent intent) {
		    String action = intent.getAction();
    	    if (BluetoothDevice.ACTION_FOUND.equals(action)) 
    	    {
    	        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
    	    	// Get the BluetoothDevice object from the Intent
    	        if(mDeviceSearchHandler != null)
    	        {
    	        	mDeviceSearchHandler.obtainMessage(0, device).sendToTarget();
    	        }	
    	    }
		}
    	};
    	
    	// Register the BroadcastReceiver
    	if(mDeviceSearchHandler != null)
    	{
    		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    		mContext.registerReceiver(mReceiver, filter); 
    	}
    }
}
