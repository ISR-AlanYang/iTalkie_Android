package com.itsmartreach.libzm.gaia;

/**
 * Interface that should be implemented by a class that would like to receive updates on the GaiaLink connection status.
 *
 */
public interface DataConnectionListener {
    /**
     * Callback for when data starts or stops being sent or received on the GaiaLink.
     * @param isDataTransferInProgress True if data is being sent or received on the link.
     */
	public void update(boolean isDataTransferInProgress);
}

