package com.joyboy.forcehz;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/** Reapplies the saved refresh-rate vote after the device finishes booting. */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "forcehz";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            return;
        }

        if (!Rms.restoreOnBoot(context)) {
            Log.i(TAG, "Boot restore disabled by user");
            return;
        }

        int fps = Rms.selectedRate(context);
        if (fps == 60 || fps == 90 || fps == 120 || fps == 144) {
            boolean submitted = Rms.vote(context, fps);
            Log.i(TAG, "Boot restore " + fps + " Hz submitted=" + submitted);
        }
    }
}
