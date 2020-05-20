package com.example.oneviewapisample.hotspot

/** This must be replaced by android's OnStartTetheringCallBack  */
abstract class OnStartTetheringCallBack {
    /**
     * Called when tethering has been successfully started.
     */
    abstract fun onTetheringStarted()

    /**
     * Called when starting tethering failed.
     */
    abstract fun onTetheringFailed()
}