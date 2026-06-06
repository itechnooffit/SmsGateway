package com.smsgateway.app.activities;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smsgateway.app.R;
import com.smsgateway.app.databinding.ActivitySettingsBinding;
import com.smsgateway.app.utils.PrefsManager;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private PrefsManager prefsManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.settings);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefsManager = new PrefsManager(this);
        loadSettings();

        binding.btnSaveSettings.setOnClickListener(v -> saveSettings());
    }

    private void loadSettings() {
        binding.etPollInterval.setText(String.valueOf(prefsManager.getPollInterval()));
    }

    private void saveSettings() {
        try {
            int interval = Integer.parseInt(binding.etPollInterval.getText().toString());
            if (interval < 1) interval = 1;
            if (interval > 3600) interval = 3600;
            prefsManager.savePollInterval(interval);
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show();
            finish();
        } catch (NumberFormatException e) {
            binding.tilPollInterval.setError(getString(R.string.error_invalid_number));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
