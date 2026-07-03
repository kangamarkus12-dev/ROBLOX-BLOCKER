package com.robloxblocker.app;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
public class ForegroundService extends Service {
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startService(new Intent(this, AppBlockerService.class));
        return START_STICKY;
    }
    @Override
    public IBinder onBind(Intent intent) { return null; }
    @Override
    public void onDestroy() {
        super.onDestroy();
        startService(new Intent(this, ForegroundService.class));
    }
}
