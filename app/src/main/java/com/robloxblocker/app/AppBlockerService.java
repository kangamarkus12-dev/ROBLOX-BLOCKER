package com.robloxblocker.app;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.ActivityManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import java.util.List;

public class AppBlockerService extends AccessibilityService {
    private static final String BLOCKED_PACKAGE = "com.roblox.client";
    private static final int BLOCK_START_HOUR = 23;
    private static final int BLOCK_END_HOUR = 8;
    private static final int BLOCK_END_MINUTE = 30;

    private Handler handler = new Handler(Looper.getMainLooper());
    private long lastBlockTime = 0;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            if (packageName.equals(BLOCKED_PACKAGE) && isWithinBlockingHours()) {
                blockApp();
            }
        }
    }

    @Override
    public void onInterrupt() {}

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
        startForegroundService();
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("roblox_blocker_channel", "Securite Systeme", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "roblox_blocker_channel")
                .setContentTitle("Protection active")
                .setContentText("Roblox bloque de 23h a 8h30")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        startForeground(1, notification.build());
    }

    private boolean isWithinBlockingHours() {
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int startMinutes = BLOCK_START_HOUR * 60;
        int endMinutes = BLOCK_END_HOUR * 60 + BLOCK_END_MINUTE;
        return currentMinutes >= startMinutes || currentMinutes < endMinutes;
    }

    private void blockApp() {
        long now = System.currentTimeMillis();
        if (now - lastBlockTime < 2000) return;
        lastBlockTime = now;

        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);

        showBlockNotification();
    }

    private void showBlockNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("block_channel", "Blocage Roblox", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "block_channel")
                .setContentTitle("Roblox bloque")
                .setContentText("IL EST TARD ! Va dormir maintenant.")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setSound(null);

        manager.notify(2, notification.build());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.postDelayed(() -> {
            Intent intent = new Intent(this, ForegroundService.class);
            startService(intent);
        }, 5000);
    }
}
