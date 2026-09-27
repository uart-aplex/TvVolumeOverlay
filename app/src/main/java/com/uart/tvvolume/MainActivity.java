package com.uart.tvvolume;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.net.Uri;
import android.widget.*;

public class MainActivity extends Activity {
    private TextView status;
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(48, 24, 48, 24);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(body);
        setContentView(scroll);
        TextView title = new TextView(this);
        title.setText("TV 音量提示"); title.setTextSize(26); body.addView(title);
        status = new TextView(this); status.setTextSize(16); body.addView(status);
        RadioGroup modes = new RadioGroup(this);
        String[] labels = {"關閉", "原生音量條（實驗）", "全域音量條"};
        int current = getSharedPreferences("settings", 0).getInt("mode", 0);
        for (int i = 0; i < labels.length; i++) {
            RadioButton item = new RadioButton(this);
            item.setId(100 + i); item.setText(labels[i]); modes.addView(item);
        }
        modes.check(100 + current);
        modes.setOnCheckedChangeListener((group, id) -> {
            int mode = id - 100;
            getSharedPreferences("settings", 0).edit().putInt("mode", mode).apply();
            if (mode == 0) stopService(new Intent(this, VolumeService.class));
            else startForegroundService(new Intent(this, VolumeService.class));
            updateStatus();
        });
        body.addView(modes);
        Switch boot = new Switch(this);
        boot.setText("開機自動啟動");
        boot.setChecked(getSharedPreferences("settings", 0).getBoolean("boot", false));
        boot.setOnCheckedChangeListener((v, checked) -> getSharedPreferences("settings", 0).edit().putBoolean("boot", checked).apply());
        body.addView(boot);
        Button permission = new Button(this); permission.setText("懸浮顯示權限");
        permission.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()))); }
            catch (android.content.ActivityNotFoundException e) { Toast.makeText(this, "此 TV 請使用 ADB 授予懸浮權限", Toast.LENGTH_LONG).show(); }
        }); body.addView(permission);
        Button test = new Button(this); test.setText("測試顯示");
        test.setOnClickListener(v -> {
            if (getSharedPreferences("settings", 0).getInt("mode", 0) == 0) {
                Toast.makeText(this, "請先選擇模式", Toast.LENGTH_SHORT).show(); return;
            }
            startForegroundService(new Intent(this, VolumeService.class).setAction("preview"));
        }); body.addView(test);
    }
    @Override public void onResume() {
        super.onResume(); updateStatus();
        if (getSharedPreferences("settings", 0).getInt("mode", 0) != 0)
            startForegroundService(new Intent(this, VolumeService.class));
    }
    private void updateStatus() {
        status.setText(Settings.canDrawOverlays(this) ? "懸浮權限：已允許" : "懸浮權限：未允許");
    }
}
