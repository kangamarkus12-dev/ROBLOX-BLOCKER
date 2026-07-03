package com.robloxblocker.app;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class SetupActivity extends AppCompatActivity {

    private static final String PREF_NAME = "RobloxBlockerPrefs";
    private static final String KEY_PASSWORD = "admin_password";
    private static final String KEY_IS_SETUP = "is_setup";

    private DevicePolicyManager devicePolicyManager;
    private ComponentName deviceAdminComponent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        deviceAdminComponent = new ComponentName(this, DeviceAdminReceiver.class);

        Button btnEnableAdmin = findViewById(R.id.btn_enable_admin);
        Button btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility);
        Button btnStartService = findViewById(R.id.btn_start_service);
        EditText passwordInput = findViewById(R.id.password_input);

        // Sauvegarder le mot de passe
        findViewById(R.id.btn_save_password).setOnClickListener(v -> {
            String pwd = passwordInput.getText().toString().trim();
            if (pwd.length() < 6) {
                Toast.makeText(this, "Le mot de passe doit faire au moins 6 chiffres", Toast.LENGTH_LONG).show();
                return;
            }
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putString(KEY_PASSWORD, pwd)
                    .apply();
            Toast.makeText(this, "✅ Mot de passe sauvegardé !", Toast.LENGTH_SHORT).show();
        });

        btnEnableAdmin.setOnClickListener(v -> {
            if (!devicePolicyManager.isAdminActive(deviceAdminComponent)) {
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, deviceAdminComponent);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Cette permission empêche la désinstallation de l'application.");
                startActivity(intent);
            } else {
                Toast.makeText(this, "✅ Admin déjà activé", Toast.LENGTH_SHORT).show();
            }
        });

        btnEnableAccessibility.setOnClickListener(v -> {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        });

        btnStartService.setOnClickListener(v -> {
            getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_IS_SETUP, true)
                    .apply();
            startService(new Intent(this, ForegroundService.class));
            Toast.makeText(this, "🔒 Blocage actif ! Roblox bloqué de 23h à 8h30.", Toast.LENGTH_LONG).show();
            finish();
        });

        // Charger le mot de passe existant
        String savedPassword = getSharedPreferences(PREF_NAME, MODE_PRIVATE)
                .getString(KEY_PASSWORD, "");
        if (!savedPassword.isEmpty()) {
            passwordInput.setText(savedPassword);
        }
    }

    // Pour rouvrir l'activité de configuration (via un code secret)
    public static void openSetup(Context context) {
        Intent intent = new Intent(context, SetupActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
}