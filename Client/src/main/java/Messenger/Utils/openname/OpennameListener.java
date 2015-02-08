package Messenger.Utils.openname;

import Messenger.Address;

/**
 * Created by chris on 2/8/15.
 */
public interface OpennameListener {
    void onDownloadComplete(Address addr);
    void onDownloadFailed();
}
