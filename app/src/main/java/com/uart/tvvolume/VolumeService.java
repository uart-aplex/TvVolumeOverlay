package com.uart.tvvolume;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.os.*;
import android.provider.Settings;
import android.util.Log;
import android.view.*;
import android.widget.*;

public class VolumeService extends Service {
    private final Handler handler = new Handler(Looper.getMainLooper());
    private AudioManager audio;
    private WindowManager windows;
    private PowerManager power;
    private LinearLayout overlay;
    private TextView label;
    private ProgressBar bar;
    private int last = -1;
    private boolean lastMute;
    private boolean registered;
    private boolean awake;
    private final Runnable hide = () -> removeOverlay();
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            boolean interactive = power.isInteractive();
            if (interactive) sample(awake);
            else removeOverlay();
            awake = interactive;
            handler.postDelayed(this, interactive ? 500 : 3000);
        }
    };
    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) { awake = false; removeOverlay(); }
            else if (Intent.ACTION_SCREEN_ON.equals(i.getAction())) { sample(false); awake = true; }
            else if (power.isInteractive()) sample(awake);
        }
    };
    @Override public void onCreate() {
        super.onCreate();
        audio = getSystemService(AudioManager.class);
        windows = getSystemService(WindowManager.class);
        power = getSystemService(PowerManager.class);
        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("monitor", "音量提示服務", NotificationManager.IMPORTANCE_LOW));
        Intent open = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        startForeground(1, new Notification.Builder(this, "monitor")
                .setSmallIcon(android.R.drawable.ic_lock_silent_mode_off)
                .setContentTitle("TV 音量提示").setContentText("音量監測執行中")
                .setContentIntent(pi).setOngoing(true).build());
        IntentFilter filter = new IntentFilter("android.media.VOLUME_CHANGED_ACTION");
        filter.addAction("android.media.STREAM_MUTE_CHANGED_ACTION");
        filter.addAction(Intent.ACTION_SCREEN_OFF); filter.addAction(Intent.ACTION_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED);
        else registerReceiver(receiver, filter);
        registered = true;
        awake = power.isInteractive();
        sample(false);
        handler.post(poll);
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (mode() == 0) { stopSelf(); return START_NOT_STICKY; }
        removeOverlay();
        Log.i("TvVolume", "Monitor mode=" + mode());
        if (intent != null && "preview".equals(intent.getAction())) show();
        return START_STICKY;
    }
    private int mode() { return getSharedPreferences("settings", 0).getInt("mode", 0); }
    private void sample(boolean notify) {
        int now = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
        boolean mute = audio.isStreamMute(AudioManager.STREAM_MUSIC);
        boolean changed = last >= 0 && (now != last || mute != lastMute);
        last = now; lastMute = mute;
        if (notify && changed) {
            Log.i("TvVolume", "Volume=" + now + " mute=" + mute + " mode=" + mode());
            show();
        }
    }
    @android.annotation.SuppressLint("WrongConstant") // Explicit opt-in test of AOSP internal flag.
    private void show() {
        if (!power.isInteractive()) return;
        if (mode() == 1) {
            // AOSP internal FLAG_ABSOLUTE_VOLUME bypasses CEC redispatch. Experimental:
            // request UI with ADJUST_SAME only, never set or change the stream index.
            try {
                audio.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_SAME,
                        AudioManager.FLAG_SHOW_UI | 0x2000);
                Log.i("TvVolume", "Native UI requested (same volume)");
            } catch (RuntimeException e) { Log.w("TvVolume", "Native UI request denied", e); }
            return;
        }
        if (mode() != 2 || !Settings.canDrawOverlays(this)) return;
        try {
            if (overlay == null) {
                overlay = new LinearLayout(this);
                overlay.setOrientation(LinearLayout.VERTICAL);
                overlay.setPadding(dp(20), dp(12), dp(20), dp(16));
                overlay.setBackgroundColor(Color.rgb(20, 24, 24));
                label = new TextView(this); label.setTextColor(Color.WHITE); label.setTextSize(25);
                overlay.addView(label);
                bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
                bar.setProgressTintList(android.content.res.ColorStateList.valueOf(Color.rgb(65, 210, 165)));
                overlay.addView(bar, new LinearLayout.LayoutParams(-1, dp(12)));
                WindowManager.LayoutParams p = new WindowManager.LayoutParams(dp(330), -2,
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
                        PixelFormat.TRANSLUCENT);
                p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
                p.y = dp(65); p.setTitle("TvVolumeOverlay");
                windows.addView(overlay, p);
            }
            int max = audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int value = audio.getStreamVolume(AudioManager.STREAM_MUSIC);
            label.setText(audio.isStreamMute(AudioManager.STREAM_MUSIC) ? "靜音" : "音量  " + value + " / " + max);
            bar.setMax(max); bar.setProgress(value);
            handler.removeCallbacks(hide); handler.postDelayed(hide, 3000);
        } catch (RuntimeException e) {
            Log.w("TvVolume", "Overlay unavailable", e); removeOverlay();
        }
    }
    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private void removeOverlay() {
        handler.removeCallbacks(hide);
        if (overlay != null) {
            try { windows.removeView(overlay); } catch (IllegalArgumentException ignored) { }
            overlay = null;
        }
    }
    @Override public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (registered) unregisterReceiver(receiver);
        removeOverlay(); super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
}
