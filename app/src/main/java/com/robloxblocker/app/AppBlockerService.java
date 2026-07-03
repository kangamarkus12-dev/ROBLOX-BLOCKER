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
import android.util.Log;
import android.view.WindowManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager.LayoutParams;
import android.widget.TextView;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.app.NotificationCompat;
import java.util.Calendar;
import java.util.List;

public class AppBlockerService extends AccessibilityService {

    private static final String TAG = "AppBlockerService";
    private static final String BLOCKED_PACKAGE = "com.roblox.client"; // Roblox
    private static final int BLOCK_START_HOUR = 23;
    private static final int BLOCK_START_MINUTE = 0;
    private static final int BLOCK_END_HOUR = 8;
    private static final int BLOCK_END_MINUTE = 30;

    private Handler handler = new Handler(Looper.getMainLooper());
    private long lastBlockTime = 0;
    private WindowManager windowManager;
    private View overlayView;

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
    public void onInterrupt() {
        Log.d(TAG, "Service interrompu");
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "✅ Service d'accessibilité connecté");
        configureAccessibilityService();
        startForegroundService();
        startPeriodicCheck();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
    }

    private void configureAccessibilityService() {
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        setServiceInfo(info);
    }

    private void startForegroundService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "roblox_blocker_channel",
                    "Sécurité Système",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "roblox_blocker_channel")
                .setContentTitle("🔒 Protection active")
                .setContentText("Roblox bloqué de 23h à 8h30")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setOngoing(true);

        startForeground(1, notification.build());
    }

    private void startPeriodicCheck() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkAndBlockIfNeeded();
                handler.postDelayed(this, 3000);
            }
        }, 3000);
    }

    private void checkAndBlockIfNeeded() {
        if (isWithinBlockingHours()) {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
            if (processes != null) {
                for (ActivityManager.RunningAppProcessInfo process : processes) {
                    if (process.processName != null && process.processName.equals(BLOCKED_PACKAGE)) {
                        blockApp();
                        break;
                    }
                }
            }
        }
    }

    private boolean isWithinBlockingHours() {
        Calendar now = Calendar.getInstance();
        int currentMinutes = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        int startMinutes = BLOCK_START_HOUR * 60 + BLOCK_START_MINUTE;
        int endMinutes = BLOCK_END_HOUR * 60 + BLOCK_END_MINUTE;

        if (startMinutes < endMinutes) {
            return currentMinutes >= startMinutes && currentMinutes < endMinutes;
        } else {
            return currentMinutes >= startMinutes || currentMinutes < endMinutes;
        }
    }

    private void blockApp() {
        long now = System.currentTimeMillis();
        if (now - lastBlockTime < 2000) return;
        lastBlockTime = now;

        Log.d(TAG, "🚫 Roblox bloqué à " + java.time.LocalTime.now());

        // 1. Fermer Roblox
        Intent homeIntent = new Intent(Intent.ACTION_MAIN);
        homeIntent.addCategory(Intent.CATEGORY_HOME);
        homeIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(homeIntent);

        // 2. Afficher l'overlay PERSONNALISÉ (message rouge géant)
        showBlockOverlay();

        // 3. Notification
        showBlockNotification();
    }

    private void showBlockOverlay() {
        if (overlayView != null) {
            windowManager.removeView(overlayView);
        }

        // Créer l'overlay
        overlayView = LayoutInflater.from(this).inflate(R.layout.overlay_block, null);
        TextView message = overlayView.findViewById(R.id.block_message);
        TextView subMessage = overlayView.findViewById(R.id.block_submessage);

        // Configurer le message
        message.setText("IL EST TARD");
        message.setTextColor(0xFFFF0000); // Rouge
        message.setTextSize(48);

        subMessage.setText("TOI AUSSI TU ABUSE !\nVA DORMIR\nSINON TU VAS TE DÉTRUIRE.");
        subMessage.setTextColor(0xFFFF4444);
        subMessage.setTextSize(28);

        // Paramètres de l'overlay
        int flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                        ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                        : WindowManager.LayoutParams.TYPE_PHONE,
                flags,
                android.graphics.PixelFormat.TRANSLUCENT
        );

        windowManager.addView(overlayView, params);

        // Disparaît automatiquement après 5 secondes
        handler.postDelayed(() -> {
            if (overlayView != null && windowManager != null) {
                try {
                    windowManager.removeView(overlayView);
                    overlayView = null;
                } catch (Exception e) {
                    Log.e(TAG, "Erreur suppression overlay", e);
                }
            }
        }, 5000);
    }

    private void showBlockNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "block_channel",
                    "Blocage Roblox",
                    NotificationManager.IMPORTANCE_HIGH
            );
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(this, "block_channel")
                .setContentTitle("🚫 Roblox bloqué")
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
        if (overlayView != null && windowManager != null) {
            try {
                windowManager.removeView(overlayView);
            } catch (Exception ignored) {}
        }
        Log.d(TAG, "⚠️ Service détruit, relance automatique");
        handler.postDelayed(() -> {
            Intent intent = new Intent(this, ForegroundService.class);
            startService(intent);
        }, 5000);
    }
}