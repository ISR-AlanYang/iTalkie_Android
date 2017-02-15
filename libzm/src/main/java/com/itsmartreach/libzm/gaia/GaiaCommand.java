package com.itsmartreach.libzm.gaia;

import com.itsmartreach.libzm.gaia.Gaia.AsrResult;

/**
 * Encapsulates a GAIA command/
 *
 */
public class GaiaCommand
{
    private int mVendorId = Gaia.VENDOR_NONE;
    private int mCommandId = 0;
    private byte[] mCommandPayload = null;
    
    /**
     * Constructor that builds a command from a byte sequence.
     * @param source Array of bytes to build the command from.
     */
    GaiaCommand(byte[] source)
    {
        buildCommand(source, source.length);
    }
    
    /**
     * Constructor that builds a command from a specified number of bytes in a byte sequence. 
     * @param source Array of bytes to build the command from.
     * @param source_length Number of bytes from the array to use.
     */
    GaiaCommand(byte[] source, int source_length)
    {
        buildCommand(source, source_length);
    }

    /**
     * Combine two bytes at a particular offset in an array of bytes to make a 16-bit value.
     * @param array Array of bytes to retrieve the value from.
     * @param offset Offset within the array to get the value from.
     * @return 16-bit value.
     */
    private int shortFromByteArray(byte[] array, int offset)
    {
        int value = 0;
        
        try
        {
            value = ((array[offset] & 0xFF) << 8) | (array[offset + 1] & 0xFF);
        }        
        catch (ArrayIndexOutOfBoundsException e)
        {
            value = 0;    
        }
        
        return value;
    }
    
    /**
     * Build a GAIA command payload from a byte sequence.
     * @param source Array of bytes to build the command from.
     * @param source_length Number of bytes from the array to use.
     */
    private void buildCommand(byte[] source, int source_length)
    {
        int flags = source[Gaia.OFFS_FLAGS];
        int payload_length = source_length - Gaia.OFFS_PAYLOAD;
        
        if ((flags & Gaia.FLAG_CHECK) != 0)
            --payload_length;
        
        mVendorId = shortFromByteArray(source, Gaia.OFFS_VENDOR_ID);
        mCommandId = shortFromByteArray(source, Gaia.OFFS_COMMAND_ID);
        
        if (payload_length > 0)
        {
            mCommandPayload = new byte[payload_length];
            for (int i = 0; i < payload_length; ++i)
                mCommandPayload[i] = source[i + Gaia.OFFS_PAYLOAD];
        }
    }
    
    /**
     * Check if this command has the ACK bit set.
     * @return True if the command is an acknowledgement.
     */
    public boolean isAcknowledgement()
    {
        return (mCommandId & Gaia.ACK_MASK) != 0;
    }
    
    /**
     * Check if this command is a known CSR command.
     * @return True if command has vendor set to CSR.
     */
    public boolean isKnownCommand()
    {
        return (mVendorId == Gaia.VENDOR_CSR);
    }
    
    /**
     * Check if this command is a known CSR command and also matches the specified value.
     * @param test_id The value to match the command against.
     * @return
     */
    public boolean isKnownCommand(int test_id)
    {
        return isKnownCommand() && (mCommandId == test_id);
    }
    
    /**
     * Get the event ID found in byte zero of the payload.
     * @return The event ID.
     */
    public Gaia.EventId getEventId()
    {
        if (mCommandPayload == null || mCommandPayload.length == 0 || !isKnownCommand(Gaia.COMMAND_EVENT_NOTIFICATION))
            return null;
            
        return Gaia.EventId.valueOf(mCommandPayload[0]);    
    }

    /**
     * Get the status byte from the payload of an acknowledgement packet.
     * @return The status code as defined in Gaia.EventId.
     */
    public Gaia.Status getStatus()
    {
        if (mCommandPayload == null || mCommandPayload.length == 0)
            return null;
            
        return Gaia.Status.valueOf(mCommandPayload[0]);
    }
    
    /**
     * Get the entire payload.
     * @return Array of bytes containing the payload.
     */
    public byte[] getPayload()
    {
        return mCommandPayload;
    }
    
    /**
     * Get a single byte from the payload at the specified offset.
     * @param offset Offset within the payload.
     * @return Value at the specified offset.
     */
    public byte getByte(int offset)
    {
        byte value;
        
        try
        {
            value = mCommandPayload[offset];
        }
        
        catch (ArrayIndexOutOfBoundsException e)
        {
            value = 0;
        }
        
        return value;
    }
    
    /**
     * Get the byte at payload offset 1.
     * @return Value at offset 1.
     */
    public byte getByte()
    {
        return getByte(1);
    }
    
    /**
     * Get the byte at the specified offset in the payload interpreted as a boolean value.
     * @param offset Offset within the payload to get the boolean value from.
     * @return True if the value at the specified offset is non zero.
     */
    public boolean getBoolean(int offset)
    {
        return getByte(offset) != 0;
    }
    
    /**
     * Get the byte at the offset 1 in the payload interpreted as a boolean value.
     * @return True if the value is non zero.
     */
    public boolean getBoolean()
    {
        return getBoolean(1);
    }
    
    /**
     * 
     * @return
     */
    public AsrResult getAsrResult()
    {
        return AsrResult.valueOf(getByte());
    }
    
    
    /**
     * Combine two bytes at a specified offset in the payload to make a 16-bit value.
     * @param offset Offset within the payload to get the value.
     * @return 16-bit value.
     */
    public int getShort(int offset)
    {
        return shortFromByteArray(mCommandPayload, offset);
    }

    /**
     * Get the 16-bit value at offset 1 in the payload.
     * @return 16-bit value at offset 1.
     */
    public int getShort()
    {
        return getShort(1);
    }

    /**
     * Combine two bytes at a specified offset in the payload to make a 32-bit value.
     * @param offset Offset within the payload to get the value.
     * @return 32-bit value.
     */
    public int getInt(int offset)
    {
        int value;
        
        try
        {
            value = ((mCommandPayload[offset] & 0xFF) << 24) 
                | ((mCommandPayload[offset + 1] & 0xFF) << 16)
                | ((mCommandPayload[offset + 2] & 0xFF) << 8)
                | (mCommandPayload[offset + 3] & 0xFF);
        }
        
        catch (ArrayIndexOutOfBoundsException e)
        {
            value = 0;
        }
        
        return value;
    }

    /**
     * Get the vendor identifier for this command.
     * @return The vendor identifier.
     */
    public int getVendorId()
    {
        return mVendorId;
    }
    
    /**
     * Get the raw command ID for this command with the ACK bit stripped out.
     * @return The command ID.
     */
    public int getCommand()
    {
        return mCommandId & Gaia.COMMAND_MASK;
    }

    /**
     * Get the command ID including the ACK bit.
     * @return The command ID.
     */
    public int getCommandId()
    {
        return mCommandId;
    }
}
