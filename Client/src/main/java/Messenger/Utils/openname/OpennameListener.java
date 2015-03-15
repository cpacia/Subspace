package Messenger.Utils.openname;

import Messenger.Address;

/**
 * Listener for downloading the openname avatar
 */
public interface OpennameListener {
    //Download finished sucessfully
    void onDownloadComplete(Address addr, String formattedName);
    //Download Failed
    void onDownloadFailed();
}
