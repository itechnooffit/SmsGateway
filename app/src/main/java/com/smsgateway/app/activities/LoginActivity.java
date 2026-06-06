package com.smsgateway.app.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import com.smsgateway.app.R;
import com.smsgateway.app.databinding.ActivityLoginBinding;
import com.smsgateway.app.models.LoginRequest;
import com.smsgateway.app.models.LoginResponse;
import com.smsgateway.app.utils.ApiClient;
import com.smsgateway.app.utils.PrefsManager;

import org.json.JSONObject;

import java.util.Locale;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private ActivityLoginBinding binding;
    private PrefsManager prefsManager;

    private final String[] languages = {"English", "Spanish", "French", "German", "Portuguese", "Russian", "Arabic"};
    private final String[] languageCodes = {"en", "es", "fr", "de", "pt", "ru", "ar"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        prefsManager = new PrefsManager(this);

        // If already logged in, go to main
        if (prefsManager.isLoggedIn()) {
            startMainActivity();
            return;
        }

        setupLanguageSpinner();
        setupClickListeners();
        restoreSavedCredentials();
    }

    private void setupLanguageSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, languages);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        binding.spinnerLanguage.setAdapter(adapter);

        // Select saved language
        String savedLang = prefsManager.getLanguage();
        for (int i = 0; i < languageCodes.length; i++) {
            if (languageCodes[i].equals(savedLang)) {
                binding.spinnerLanguage.setSelection(i);
                break;
            }
        }

        binding.spinnerLanguage.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                prefsManager.saveLanguage(languageCodes[position]);
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void restoreSavedCredentials() {
        String savedServer = prefsManager.getServerUrl();
        String savedEmail = prefsManager.getEmail();
        if (!TextUtils.isEmpty(savedServer)) {
            binding.etServer.setText(savedServer);
        }
        if (!TextUtils.isEmpty(savedEmail)) {
            binding.etEmail.setText(savedEmail);
        }
    }

    private void setupClickListeners() {
        binding.btnSignIn.setOnClickListener(v -> attemptLogin());
        binding.btnQrSignIn.setOnClickListener(v -> startQrScan());
    }

    private void attemptLogin() {
        String server = binding.etServer.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        // Validation
        binding.tilServer.setError(null);
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        if (TextUtils.isEmpty(server)) {
            binding.tilServer.setError(getString(R.string.error_server_required));
            return;
        }
        if (!server.startsWith("http://") && !server.startsWith("https://")) {
            binding.tilServer.setError(getString(R.string.error_server_invalid));
            return;
        }
        if (TextUtils.isEmpty(email)) {
            binding.tilEmail.setError(getString(R.string.error_email_required));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            binding.tilPassword.setError(getString(R.string.error_password_required));
            return;
        }

        // Show loading
        setLoading(true);

        // Save server + email
        prefsManager.saveServerUrl(server);
        prefsManager.saveEmail(email);

        // Perform API login
        ApiClient apiClient = new ApiClient(server);
        apiClient.login(email, password, new ApiClient.LoginCallback() {
            @Override
            public void onSuccess(LoginResponse response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    prefsManager.saveAuthToken(response.token);
                    prefsManager.setLoggedIn(true);
                    requestPermissionsAndProceed();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void startQrScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE);
        integrator.setPrompt(getString(R.string.scan_qr_prompt));
        integrator.setCameraId(0);
        integrator.setBeepEnabled(true);
        integrator.setBarcodeImageEnabled(false);
        integrator.initiateScan();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                parseQrResult(result.getContents());
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void parseQrResult(String qrContent) {
    try {
        String apiKey = qrContent.trim();
        String server = "";
        if (qrContent.startsWith("{")) {
            JSONObject json = new JSONObject(qrContent);
            apiKey = json.optString("key",
                     json.optString("apiKey",
                     json.optString("api_key", qrContent)));
            server = json.optString("server", "");
        }
        if (!server.isEmpty()) {
            binding.etServer.setText(server);
            prefsManager.saveServerUrl(server);
        }
        String finalServer = server.isEmpty() ? prefsManager.getServerUrl() : server;
        prefsManager.saveEmail("qr_login");
        setLoading(true);
        ApiClient apiClient = new ApiClient(finalServer);
        apiClient.login("qr_login", "QR:" + apiKey, new ApiClient.LoginCallback() {
            @Override
            public void onSuccess(LoginResponse response) {
                runOnUiThread(() -> {
                    setLoading(false);
                    prefsManager.saveAuthToken(response.token);
                    prefsManager.setLoggedIn(true);
                    requestPermissionsAndProceed();
                });
            }
            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, error, Toast.LENGTH_LONG).show();
                });
            }
        });
    } catch (Exception e) {
        Toast.makeText(this, R.string.qr_parse_error, Toast.LENGTH_SHORT).show();
    }
}

    private void requestPermissionsAndProceed() {
        String[] permissions = {
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.CAMERA
        };

        boolean allGranted = true;
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            startMainActivity();
        } else {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            startMainActivity();
        }
    }

    private void startMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setLoading(boolean loading) {
        binding.btnSignIn.setEnabled(!loading);
        binding.btnQrSignIn.setEnabled(!loading);
        binding.progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.btnSignIn.setText(loading ? R.string.signing_in : R.string.sign_in);
    }
}
