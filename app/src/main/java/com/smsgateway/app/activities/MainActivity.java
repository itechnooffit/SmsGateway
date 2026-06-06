package com.smsgateway.app.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.smsgateway.app.R;
import com.smsgateway.app.databinding.ActivityMainBinding;
import com.smsgateway.app.services.SmsGatewayService;
import com.smsgateway.app.utils.PrefsManager;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private PrefsManager prefsManager;
    private BroadcastReceiver batteryReceiver;
    private BroadcastReceiver statsReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.app_name);
        }

        prefsManager = new PrefsManager(this);
        setupUI();
        startGatewayService();
        registerBatteryReceiver();
        registerStatsReceiver();
    }

    private void setupUI() {
        binding.tvServerUrl.setText(prefsManager.getServerUrl());
        binding.tvEmail.setText(prefsManager.getEmail());
        binding.tvStatus.setText(R.string.status_running);

        // Restore SIM selection
        int savedSim = prefsManager.getSimSlot();
        if (savedSim == 0) binding.rbSimDefault.setChecked(true);
        else if (savedSim == 1) binding.rbSim1.setChecked(true);
        else if (savedSim == 2) binding.rbSim2.setChecked(true);

        binding.rgSimSlot.setOnCheckedChangeListener((group, checkedId) -> {
            int simSlot = 0;
            if (checkedId == R.id.rbSim1) simSlot = 1;
            else if (checkedId == R.id.rbSim2) simSlot = 2;
            prefsManager.saveSimSlot(simSlot);
            restartService();
        });

        // Auto retry switch
        binding.switchAutoRetry.setChecked(prefsManager.isAutoRetry());
        binding.switchAutoRetry.setOnCheckedChangeListener((btn, checked) -> {
            prefsManager.saveAutoRetry(checked);
        });

        // Notifications switch
        binding.switchNotifications.setChecked(prefsManager.isNotificationsEnabled());
        binding.switchNotifications.setOnCheckedChangeListener((btn, checked) -> {
            prefsManager.saveNotificationsEnabled(checked);
        });

        // Restore stats
        binding.tvSentCount.setText(String.valueOf(prefsManager.getSentCount()));
        binding.tvFailedCount.setText(String.valueOf(prefsManager.getFailedCount()));
        binding.tvPendingCount.setText("0");

        binding.btnToggleService.setOnClickListener(v -> toggleService());
        binding.btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));
        binding.btnViewLog.setOnClickListener(v ->
                startActivity(new Intent(this, LogActivity.class)));
    }

    private void startGatewayService() {
        Intent serviceIntent = new Intent(this, SmsGatewayService.class);
        serviceIntent.putExtra("server", prefsManager.getServerUrl());
        serviceIntent.putExtra("token", prefsManager.getAuthToken());
        serviceIntent.putExtra("simSlot", prefsManager.getSimSlot());
        startForegroundService(serviceIntent);
        binding.tvStatus.setText(R.string.status_running);
        binding.tvStatus.setTextColor(getColor(R.color.green_running));
        binding.btnToggleService.setText(R.string.stop_service);
    }

    private void restartService() {
        stopService(new Intent(this, SmsGatewayService.class));
        startGatewayService();
    }

    private void toggleService() {
        if (SmsGatewayService.isRunning) {
            stopService(new Intent(this, SmsGatewayService.class));
            binding.tvStatus.setText(R.string.status_stopped);
            binding.tvStatus.setTextColor(getColor(R.color.red_stopped));
            binding.btnToggleService.setText(R.string.start_service);
        } else {
            startGatewayService();
        }
    }

    private void registerBatteryReceiver() {
        batteryReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
                int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
                if (level >= 0 && scale > 0) {
                    int pct = (int) ((level / (float) scale) * 100);
                    binding.tvBattery.setText(pct + "%");
                    if (pct > 50) binding.tvBattery.setTextColor(getColor(R.color.green_running));
                    else if (pct > 20) binding.tvBattery.setTextColor(getColor(R.color.primary_blue));
                    else binding.tvBattery.setTextColor(getColor(R.color.red_stopped));
                }
            }
        };
        registerReceiver(batteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
    }

    private void registerStatsReceiver() {
        statsReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                binding.tvSentCount.setText(String.valueOf(prefsManager.getSentCount()));
                binding.tvFailedCount.setText(String.valueOf(prefsManager.getFailedCount()));
            }
        };
        registerReceiver(statsReceiver, new IntentFilter("com.smsgateway.STATS_UPDATED"));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (batteryReceiver != null) unregisterReceiver(batteryReceiver);
        if (statsReceiver != null) unregisterReceiver(statsReceiver);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            confirmLogout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void confirmLogout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.logout)
                .setMessage(R.string.logout_confirm)
                .setPositiveButton(R.string.yes, (d, w) -> logout())
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void logout() {
        stopService(new Intent(this, SmsGatewayService.class));
        prefsManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
