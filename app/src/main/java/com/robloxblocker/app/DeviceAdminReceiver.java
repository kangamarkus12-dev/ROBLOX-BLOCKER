package com.robloxblocker.app;
import android.app.admin.DeviceAdminReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
public class DeviceAdminReceiver extends DeviceAdminReceiver {
    @Override
    public void onEnabled(Context context, Intent intent) {
        Toast.makeText(context, "?? Protection activée", Toast.LENGTH_LONG).show();
    }
    @Override
    public void onDisabled(Context context, Intent intent) {
        Toast.makeText(context, "?? Désactivation détectée", Toast.LENGTH_LONG).show();
    }
}
