package com.smsgateway.app.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.smsgateway.app.R;
import com.smsgateway.app.utils.PrefsManager;

import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PurchaseActivity extends AppCompatActivity {

    private EditText etServer;
    private EditText etPurchaseCode;
    private Button btnActivate;
    private ProgressBar progressBar;
    private PrefsManager prefsManager;

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchase);

        prefsManager = new PrefsManager(this);

        if (prefsManager.isActivated()) {
            goToLogin();
            return;
        }

        etServer = findViewById(R.id.etPurchaseServer);
        etPurchaseCode = findViewById(R.id.etPurchaseCode);
        btnActivate = findViewById(R.id.btnActivate);
        progressBar = findViewById(R.id.purchaseProgressBar);

        btnActivate.setOnClickListener(v -> validateCode());

        findViewById(R.id.btnTelegram).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/zikooo2002")));
            } catch (Exception e) {
                Toast.makeText(this, "Telegram: @zikooo2002", Toast.LENGTH_LONG).show();
            }
        });

        findViewById(R.id.btnWhatsapp).setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/40760100024")));
            } catch (Exception e) {
                Toast.makeText(this, "WhatsApp: +40760100024", Toast.LENGTH_LONG).show();
            }
        });
    }

    private void validateCode() {
        String server = etServer.getText().toString().trim();
        String code = etPurchaseCode.getText().toString().trim();

        if (TextUtils.isEmpty(server)) { etServer.setError("Server URL is required"); return; }
        if (TextUtils.isEmpty(code)) { etPurchaseCode.setError("Purchase code is required"); return; }

        setLoading(true);
        String deviceId = android.os.Build.MODEL.replaceAll("\\s+", "_") + "_" + android.os.Build.SERIAL;

        new Thread(() -> {
            try {
                RequestBody body = new FormBody.Builder()
                        .add("code", code)
                        .add("deviceId", deviceId)
                        .build();
                Request req = new Request.Builder()
                        .url(server + "/services/validate-license.php")
                        .post(body)
                        .build();
                try (Response r = client.newCall(req).execute()) {
                    String rb = r.body() != null ? r.body().string() : "";
                    JSONObject j = new JSONObject(rb);
                    runOnUiThread(() -> {
                        setLoading(false);
                        if (j.optBoolean("success", false)) {
                            prefsManager.setActivated(true);
                            prefsManager.saveServerUrl(server);
                            Toast.makeText(this, "✅ Activated!", Toast.LENGTH_SHORT).show();
                            goToLogin();
                        } else {
                            JSONObject err = j.optJSONObject("error");
                            String msg = err != null ? err.optString("message", "Invalid code") : "Invalid code";
                            Toast.makeText(this, "❌ " + msg, Toast.LENGTH_LONG).show();
                        }
                    });
                }
            } catch (Exception e) {
                runOnUiThread(() -> { setLoading(false); Toast.makeText(this, "Connection failed: " + e.getMessage(), Toast.LENGTH_LONG).show(); });
            }
        }).start();
    }

    private void goToLogin() { startActivity(new Intent(this, LoginActivity.class)); finish(); }

    private void setLoading(boolean loading) {
        btnActivate.setEnabled(!loading);
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnActivate.setText(loading ? "Activating..." : "ACTIVATE");
    }
}
