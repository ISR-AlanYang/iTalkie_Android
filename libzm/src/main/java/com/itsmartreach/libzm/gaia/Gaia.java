package com.itsmartreach.libzm.gaia;

import java.io.IOException;

public class Gaia
{
    static final byte SOF = (byte) 0xFF;

    //    Packet format.
    //    0 bytes  1        2        3        4        5        6        7        8      len+8
    //    +--------+--------+--------+--------+--------+--------+--------+--------+ +--------+--------+ +--------+
    //    |   SOF  |VERSION | FLAGS  | LENGTH |    VENDOR ID    |   COMMAND ID    | | PAYLOAD   ...   | | CHECK  |
    //    +--------+--------+--------+--------+--------+--------+--------+--------+ +--------+--------+ +--------+

    static final int OFFS_SOF = 0;
    static final int OFFS_VERSION = 1;
    static final int OFFS_FLAGS = 2;
    static final int OFFS_PAYLOAD_LENGTH = 3;
    static final int OFFS_VENDOR_ID = 4;
    static final int OFFS_VENDOR_ID_H = OFFS_VENDOR_ID;
    static final int OFFS_VENDOR_ID_L = OFFS_VENDOR_ID + 1;
    static final int OFFS_COMMAND_ID = 6;
    static final int OFFS_COMMAND_ID_H = OFFS_COMMAND_ID;
    static final int OFFS_COMMAND_ID_L = OFFS_COMMAND_ID + 1;
    static final int OFFS_PAYLOAD = 8;
      
    static final int FLAG_CHECK = 0x01;
    
    static final int COMMAND_MASK = 0x7FFF;
    static final int ACK_MASK = 0x8000;
    
    public static final int VENDOR_NONE = 0x7FFE;
    public static final int VENDOR_CSR = 0x000A;
    
    public static final int COMMAND_INTENT_GET = 0x0080;
    
    public static final int COMMAND_TYPE_MASK = 0x7F00;
    public static final int COMMAND_TYPE_CONFIGURATION = 0x0100;
    public static final int COMMAND_TYPE_CONTROL = 0x0200;
    public static final int COMMAND_TYPE_STATUS = 0x0300;
    public static final int COMMAND_TYPE_FEATURE = 0x0500;
    public static final int COMMAND_TYPE_DEBUG = 0x0700;
    public static final int COMMAND_TYPE_NOTIFICATION = 0x4000;

//  Configuration commands 0x01nn
    public static final int COMMAND_SET_RAW_CONFIGURATION = 0x0100;
    public static final int COMMAND_GET_RAW_CONFIGURATION = 0x0180;
    public static final int COMMAND_SET_LED_CONFIGURATION = 0x0101;
    public static final int COMMAND_GET_LED_CONFIGURATION = 0x0181;
    public static final int COMMAND_SET_TONE_CONFIGURATION = 0x0102;
    public static final int COMMAND_GET_TONE_CONFIGURATION = 0x0182;
    public static final int COMMAND_SET_DEFAULT_VOLUME = 0x0103;
    public static final int COMMAND_GET_DEFAULT_VOLUME = 0x0183;
    public static final int COMMAND_FACTORY_DEFAULT_RESET = 0x0104;
    public static final int COMMAND_GET_CONFIGURATION_ID = 0x0184;
    public static final int COMMAND_SET_VIBRATOR_CONFIGURATION = 0x0105;
    public static final int COMMAND_GET_VIBRATOR_CONFIGURATION = 0x0185;
    public static final int COMMAND_SET_VOICE_PROMPT_CONFIGURATION = 0x0106;
    public static final int COMMAND_GET_VOICE_PROMPT_CONFIGURATION = 0x0186;
    public static final int COMMAND_SET_FEATURE_CONFIGURATION = 0x0107;
    public static final int COMMAND_GET_FEATURE_CONFIGURATION = 0x0187;
    public static final int COMMAND_SET_USER_EVENT_CONFIGURATION = 0x0108;
    public static final int COMMAND_GET_USER_EVENT_CONFIGURATION = 0x0188;
    public static final int COMMAND_SET_TIMER_CONFIGURATION = 0x0109;
    public static final int COMMAND_GET_TIMER_CONFIGURATION = 0x0189;
    public static final int COMMAND_SET_AUDIO_GAIN_CONFIGURATION = 0x010A;
    public static final int COMMAND_GET_AUDIO_GAIN_CONFIGURATION = 0x018A;
    public static final int COMMAND_SET_HFP_CONFIGURATION = 0x010B;
    public static final int COMMAND_GET_HFP_CONFIGURATION = 0x018B;
    public static final int COMMAND_SET_POWER_CONFIGURATION = 0x010C;
    public static final int COMMAND_GET_POWER_CONFIGURATION = 0x018C;
    public static final int COMMAND_SET_USER_TONE_CONFIGURATION = 0x010E;
    public static final int COMMAND_GET_USER_TONE_CONFIGURATION = 0x018E;
    public static final int COMMAND_SET_DEVICE_NAME = 0x010F;
    public static final int COMMAND_GET_DEVICE_NAME = 0x018F;
    public static final int COMMAND_SET_RSSI_CONFIGURATION = 0x0110;
    public static final int COMMAND_GET_RSSI_CONFIGURATION = 0x0190;

    
//  Control commands 0x02nn
    public static final int COMMAND_CHANGE_VOLUME = 0x0201;
    public static final int COMMAND_DEVICE_RESET = 0x0202;
    public static final int COMMAND_GET_BOOT_MODE = 0x0282;
    public static final int COMMAND_POWER_OFF = 0x0204;
    public static final int COMMAND_SET_VOLUME_ORIENTATION = 0x0205;
    public static final int COMMAND_GET_VOLUME_ORIENTATION = 0x0285;
    public static final int COMMAND_SET_VIBRATOR_CONTROL = 0x0206;
    public static final int COMMAND_GET_VIBRATOR_CONTROL = 0x0286;
    public static final int COMMAND_SET_LED_CONTROL = 0x0207;
    public static final int COMMAND_GET_LED_CONTROL = 0x0287;
    public static final int COMMAND_FM_CONTROL = 0x0208;
    public static final int COMMAND_PLAY_TONE = 0x0209;
    public static final int COMMAND_SET_VOICE_PROMPT_CONTROL = 0x020A;
    public static final int COMMAND_GET_VOICE_PROMPT_CONTROL = 0x028A;
    public static final int COMMAND_CHANGE_TTS_LANGUAGE = 0x020B;
    public static final int COMMAND_SET_SPEECH_RECOGNITION_CONTROL = 0x020C;
    public static final int COMMAND_GET_SPEECH_RECOGNITION_CONTROL = 0x028C;
    public static final int COMMAND_ALERT_LEDS = 0x020D;
    public static final int COMMAND_ALERT_TONE = 0x020E;
    public static final int COMMAND_ALERT_EVENT = 0x0210;
    public static final int COMMAND_ALERT_VOICE = 0x0211;
    public static final int COMMAND_SET_TTS_LANGUAGE = 0x0212;
    public static final int COMMAND_GET_TTS_LANGUAGE = 0x0292;
    public static final int COMMAND_START_SPEECH_RECOGNITION = 0x0213;
    public static final int COMMAND_SET_EQ_CONTROL = 0x0214;
    public static final int COMMAND_GET_EQ_CONTROL = 0x0294;
    public static final int COMMAND_SET_BASS_BOOST_CONTROL = 0x0215;
    public static final int COMMAND_GET_BASS_BOOST_CONTROL = 0x0295;
    public static final int COMMAND_SET_3D_ENHANCEMENT_CONTROL = 0x0216;
    public static final int COMMAND_GET_3D_ENHANCEMENT_CONTROL = 0x0296;
    public static final int COMMAND_SWITCH_EQ_CONTROL = 0x0217;
    public static final int COMMAND_TOGGLE_BASS_BOOST_CONTROL = 0x0218;
    public static final int COMMAND_TOGGLE_3D_ENHANCEMENT_CONTROL = 0x0219;
    public static final int COMMAND_DISPLAY_CONTROL = 0x021C;

    public static final int COMMAND_SET_CODEC = 0x0240;
    public static final int COMMAND_GET_CODEC = 0x02C0;

    
//  Polled status commands 0x03nn
    public static final int COMMAND_GET_API_VERSION = 0x0300;
    public static final int COMMAND_GET_CURRENT_RSSI = 0x0301;
    public static final int COMMAND_GET_CURRENT_BATTERY_LEVEL = 0x0302;
    public static final int COMMAND_GET_MODULE_ID = 0x0303;
    public static final int COMMAND_GET_APPLICATION_VERSION = 0x0304;
    public static final int COMMAND_GET_PIO_STATE = 0x0306;
    
    
//  Feature Control commands 0x05nn
    public static final int COMMAND_GET_AUTH_BITMAPS = 0x0580;
    public static final int COMMAND_AUTHENTICATE_REQUEST = 0x0501;
    public static final int COMMAND_AUTHENTICATE_RESPONSE = 0x0502;
    public static final int COMMAND_SET_FEATURE = 0x0503;
    public static final int COMMAND_GET_FEATURE = 0x0583;
    
    
//  Data Transfer commands 0x06nn
    public static final int COMMAND_GET_STORAGE_PARTITION_STATUS = 0x0610;
    public static final int COMMAND_OPEN_STORAGE_PARTITION = 0x0611;
    public static final int COMMAND_WRITE_STORAGE_PARTITION = 0x0615;
    public static final int COMMAND_CLOSE_STORAGE_PARTITION = 0x0618;
    public static final int COMMAND_MOUNT_STORAGE_PARTITION = 0x061A;
    public static final int COMMAND_GET_FILE_STATUS = 0x0620;
    public static final int COMMAND_OPEN_FILE = 0x0621;
    public static final int COMMAND_READ_FILE = 0x0624;
    public static final int COMMAND_CLOSE_FILE = 0x0628;
    
//
    public static final int COMMAND_GET_MOUNTED_PARTITIONS = 0x01a0;    
    
    
//  Debugging commands 0x07nn
    public static final int COMMAND_NO_OPERATION = 0x0700;
    public static final int COMMAND_GET_DEBUG_FLAGS = 0x0701;
    public static final int COMMAND_SET_DEBUG_FLAGS = 0x0702;
    public static final int COMMAND_RETRIEVE_PS_KEY = 0x0710;
    public static final int COMMAND_RETRIEVE_FULL_PS_KEY = 0x0711;
    public static final int COMMAND_STORE_PS_KEY = 0x0712;
    public static final int COMMAND_FLOOD_PS = 0x0713;
    public static final int COMMAND_SEND_DEBUG_MESSAGE = 0x0720;
    public static final int COMMAND_SEND_APPLICATION_MESSAGE = 0x0721;
    public static final int COMMAND_GET_MEMORY_SLOTS = 0x0730;
    public static final int COMMAND_GET_DEBUG_VARIABLE = 0x0740;
    public static final int COMMAND_SET_DEBUG_VARIABLE = 0x0741;
    public static final int COMMAND_DELETE_PDL = 0x0750;
    
    
//  Notification commands 0x40nn
    public static final int COMMAND_REGISTER_NOTIFICATION = 0x4001;
    public static final int COMMAND_GET_NOTIFICATION = 0x4081;
    public static final int COMMAND_CANCEL_NOTIFICATION = 0x4002;
    public static final int COMMAND_EVENT_NOTIFICATION  = 0x4003;

    private static final int PROTOCOL_VERSION = 1;
    private static final byte DEFAULT_FLAGS = 0x00;

    
    public static final int MAX_PAYLOAD = 254;
    public static final int MAX_PACKET = 270;

    
    public static final int FEATURE_DISABLED = 0x00;
    public static final int FEATURE_ENABLED = 0x01;
    
    public static enum Status
    {
        SUCCESS,
        NOT_SUPPORTED,
        NOT_AUTHENTICATED,
        INSUFFICIENT_RESOURCES,
        AUTHENTICATING,
        INVALID_PARAMETER,
        INCORRECT_STATE;

        public static Status valueOf(int status)
        {
            if (status < 0 || status >= Status.values().length)
                return null;
            
            return Status.values()[status];
        }
    }
    
    public static enum EventId
    {
        START,
        RSSI_LOW_THRESHOLD,
        RSSI_HIGH_THRESHOLD,
        BATTERY_LOW_THRESHOLD,
        BATTERY_HIGH_THRESHOLD,
        DEVICE_STATE_CHANGED,
        PIO_CHANGED,
        DEBUG_MESSAGE,
        BATTERY_CHARGED,
        CHARGER_CONNECTION,
        CAPSENSE_UPDATE,
        USER_ACTION,
        SPEECH_RECOGNITION,
        AV_COMMAND,
        REMOTE_BATTERY_LEVEL,
        KEY;

        public static EventId valueOf(int id)
        {
            if (id < 0 || id >= EventId.values().length)
                return null;
            
            return EventId.values()[id];
        }
    }
    
    public static enum AsrResult
    {
        UNRECOGNISED,
        NO,
        YES,
        WAIT,
        CANCEL;
        
        public static AsrResult valueOf(int id)
        {
            if (id < 0 || id >= AsrResult.values().length)
                return null;
            
            return AsrResult.values()[id];
        }
    }

    /**
     * Returns descriptive string representing a status code.
     * @param status    The status code to be translated
     */
    public static String statusText(Status status)
    {
        switch (status)
        {
        case SUCCESS:
            return "Success";
            
        case NOT_SUPPORTED:
            return "Command not supported";
            
        case NOT_AUTHENTICATED:
            return "Not authenticated";
            
        case INSUFFICIENT_RESOURCES:
            return "Insufficient resources";
            
        case AUTHENTICATING:
            return "Authentication in progress";
            
        case INVALID_PARAMETER:
            return "Invalid parameter";
            
        default:
            return "Unknown status code " + status;
        }
    }
    

    /**
     * Build a GAIA frame.
     * @param vendor_id Vendor identifier.
     * @param command_id Command identifier.
     * @param payload Array of payload bytes
     * @param payload_length Length of payload.
     * @param flags Flags byte.
     * @return Correctly formatted GAIA frame as an array of bytes.
     * @throws IOException
     */
    public static byte[] frame(int vendor_id, int command_id, byte[] payload, int payload_length, byte flags) throws IOException
    {
        if (payload_length > MAX_PAYLOAD)
        {
            throw new IOException("GAIA frame full");
        }

        boolean use_check = (flags & FLAG_CHECK) != 0;
        int packet_length = payload_length + OFFS_PAYLOAD + (use_check ? 1 : 0);        
        byte[] data = new byte[packet_length];
        
        data[OFFS_SOF] = SOF;
        data[OFFS_VERSION] = PROTOCOL_VERSION;
        data[OFFS_FLAGS] = flags;
        data[OFFS_PAYLOAD_LENGTH] = (byte) payload_length;
        data[OFFS_VENDOR_ID_H] = (byte) (vendor_id >> 8);
        data[OFFS_VENDOR_ID_L] = (byte) vendor_id;
        data[OFFS_COMMAND_ID_H] = (byte) (command_id >> 8);
        data[OFFS_COMMAND_ID_L] = (byte) command_id;
        
        for (int idx = 0; idx < payload_length; ++idx)
            data[idx + OFFS_PAYLOAD] = payload[idx];
        
        if (use_check)
        {
            byte check = 0;
            
            for (int idx = 0; idx < packet_length - 1; ++idx)
                check ^= data[idx];
            
            data[packet_length - 1] = check;
        }
        
        return data;
    }
    
    /**
     * Build a GAIA frame with default flags set.
     * @param vendor_id Vendor identifier.
     * @param command_id Command identifier.
     * @param payload Array of payload bytes
     * @param payload_length Length of payload.
     * @return Correctly formatted GAIA frame as an array of bytes.
     * @throws IOException
     */
    public static byte[] frame(int vendor_id, int command_id, byte[] payload, int payload_length) throws IOException
    {
        return frame(vendor_id, command_id, payload, payload_length, DEFAULT_FLAGS);
    }

    /**
     * Build a GAIA frame.
     * @param vendor_id Vendor identifier.
     * @param command_id Command identifier.
     * @param payload Array of payload bytes (payload length will be set to the size of this array).
     * @param flags Flags byte.
     * @return Correctly formatted GAIA frame as an array of bytes.
     * @throws IOException
     */
    public static byte[] frame(int vendor_id, int command_id, byte[] payload, byte flags) throws IOException
    {
        int payload_length;
        
        if (payload == null)
            payload_length = 0;
        else
            payload_length = payload.length;
        
        return frame(vendor_id, command_id, payload, payload_length, flags); 
    }
    
    /**
     * Build a GAIA frame with default flags set.
     * @param vendor_id Vendor identifier.
     * @param command_id Command identifier.
     * @param payload Array of payload bytes (payload length will be set to the size of this array).
     * @return Correctly formatted GAIA frame as an array of bytes.
     * @throws IOException
     */
    public static byte[] frame(int vendor_id, int command_id, byte[] payload) throws IOException
    {
        return frame(vendor_id, command_id, payload, DEFAULT_FLAGS); 
    }
    
    /**
     * Build a GAIA frame with no payload.
     * @param vendor_id Vendor identifier.
     * @param command_id Command identifier.
     * @return Correctly formatted GAIA frame as an array of bytes.
     * @throws IOException
     */
    public static byte[] frame(int vendor_id, int command_id) throws IOException
    {
        return frame(vendor_id, command_id, null);
    }
    
    /**
     * Get 8-bit hex string representation of byte.  
     * @param b The value.
     * @return Hex value as a string.
     */
    public static String hexb(byte b)
    {
        return String.format("%02X", b & 0xFF);
    }

    /**
     * Get 16-bit hex string representation of byte.  
     * @param b The value.
     * @return Hex value as a string.
     */
    public static String hexw(int i)
    {
        return String.format("%04X", i & 0xFFFF);
    }
 }
