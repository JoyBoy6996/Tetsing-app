package com.joyboy.forcehz;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Parcel;
import android.util.Log;

/**
 * Sends refresh-rate votes to Vivo's proprietary "rms" Binder service.
 *
 * This is the direct Binder implementation: Shizuku, root, WRITE_SETTINGS,
 * and an AccessibilityService are not used for the global refresh-rate vote.
 */
final class Rms {
    private static final String TAG = "forcehz";

    private static final int TX_RELEASE = 24;
    private static final int TX_GET = 26;
    private static final int TX_ACQUIRE_V2 = 31;

    private static final long HANDLE_A = 900001L;
    private static final String KEY_HANDLE = "handle";

    // USE_PRIVATE_FPS | USE_VSYNC_DFPS | IGNORE_BRIGHTNESS
    private static final int FLAGS = 14;
    private static final int PRIORITY = -1;

    private Rms() {}

    static IBinder binder() {
        try {
            Class<?> serviceManager = Class.forName("android.os.ServiceManager");
            return (IBinder) serviceManager
                    .getMethod("getService", String.class)
                    .invoke(null, "rms");
        } catch (Throwable t) {
            Log.w(TAG, "Unable to obtain rms Binder service", t);
            return null;
        }
    }

    static boolean isAvailable() {
        return binder() != null;
    }

    /**
     * Submits a vote using alternating handles. The new vote is submitted
     * before the old handle is released because the Binder calls are one-way.
     */
    static boolean vote(Context context, int fps) {
        IBinder service = binder();
        if (service == null) return false;

        SharedPreferences prefs = preferences(context);
        long oldHandle = prefs.getLong(KEY_HANDLE, HANDLE_A);
        long newHandle = oldHandle == HANDLE_A ? HANDLE_A + 1 : HANDLE_A;

        Parcel data = Parcel.obtain();
        try {
            data.writeLong(newHandle);
            data.writeString("FixRate");
            data.writeString("VivoHz");
            data.writeInt(-1);       // reqDisplay: all displays
            data.writeInt(0);        // reqResolution
            data.writeInt(fps);      // reqFps
            data.writeInt(0);        // reqDFps
            data.writeInt(0);        // reqConfigBits
            data.writeInt(0);        // reqStates
            data.writeInt(PRIORITY);
            data.writeInt(0);        // duration
            data.writeInt(0);        // caller
            data.writeInt(0);        // client
            data.writeInt(FLAGS);
            data.writeInt(0);        // arg1
            data.writeInt(0);        // arg2

            boolean accepted = service.transact(
                    TX_ACQUIRE_V2, data, null, IBinder.FLAG_ONEWAY);

            if (!accepted) return false;
        } catch (Throwable t) {
            Log.e(TAG, "Refresh-rate vote failed", t);
            return false;
        } finally {
            data.recycle();
        }

        prefs.edit().putLong(KEY_HANDLE, newHandle).apply();
        release(service, oldHandle);
        return true;
    }

    /** Releases both handles used by this app, returning control to system policy. */
    static boolean reset() {
        IBinder service = binder();
        if (service == null) return false;

        boolean first = release(service, HANDLE_A);
        boolean second = release(service, HANDLE_A + 1);
        return first && second;
    }

    private static boolean release(IBinder service, long handle) {
        Parcel data = Parcel.obtain();
        try {
            data.writeInt(0);
            data.writeLong(handle);
            return service.transact(
                    TX_RELEASE, data, null, IBinder.FLAG_ONEWAY);
        } catch (Throwable t) {
            Log.e(TAG, "Vote release failed for handle " + handle, t);
            return false;
        } finally {
            data.recycle();
        }
    }

    /** Returns the rate reported by Vivo's RMS service, or 0 on failure. */
    static float currentRate() {
        IBinder service = binder();
        if (service == null) return 0f;

        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        try {
            boolean ok = service.transact(TX_GET, data, reply, 0);
            if (!ok) return 0f;
            reply.readException();
            return reply.readFloat();
        } catch (Throwable t) {
            Log.w(TAG, "Unable to read active refresh rate", t);
            return 0f;
        } finally {
            data.recycle();
            reply.recycle();
        }
    }

    static void saveSelectedRate(Context context, int fps) {
        preferences(context).edit().putInt("fps", fps).apply();
    }

    static int selectedRate(Context context) {
        return preferences(context).getInt("fps", 0);
    }

    static boolean restoreOnBoot(Context context) {
        return preferences(context).getBoolean("on_boot", true);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences("forcehz_prefs", Context.MODE_PRIVATE);
    }
}
