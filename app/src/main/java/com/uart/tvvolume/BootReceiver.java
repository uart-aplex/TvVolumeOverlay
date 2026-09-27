package com.uart.tvvolume;

import android.content.*;
import android.util.Log;

public class BootReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        SharedPreferences prefs = context.getSharedPreferences("settings", 0);
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) && prefs.getBoolean("boot", false) && prefs.getInt("mode", 0) != 0) {
            try { context.startForegroundService(new Intent(context, VolumeService.class)); }
            catch (RuntimeException e) { Log.w("TvVolume", "Boot start denied", e); }
        }
    }
}
