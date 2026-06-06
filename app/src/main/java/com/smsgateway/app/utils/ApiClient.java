package com.smsgateway.app.utils;

import android.util.Log;

import com.smsgateway.app.models.LoginResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {

    private static final String TAG = "ApiClient";
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final String baseUrl;
    private final String token;
    private final OkHttpClient client;

    public ApiClient(String baseUrl) {
        this(baseUrl, null);
    }

    public ApiClient(String baseUrl, String token) {
        // Normalize URL
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.token = token;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    // ---- Login ----
    public void login(String email, String password, LoginCallback callback) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("email", email);
                body.put("password", password);

                Request request = new Request.Builder()
                        .url(baseUrl + "/api/login")
                        .post(RequestBody.create(body.toString(), JSON))
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "";
                    if (response.isSuccessful()) {
                        JSONObject json = new JSONObject(responseBody);
                        LoginResponse loginResponse = new LoginResponse();
                        loginResponse.token = json.optString("token",
                                json.optString("api_token", ""));
                        loginResponse.userId = json.optString("id", "");
                        callback.onSuccess(loginResponse);
                    } else {
                        JSONObject err = new JSONObject(responseBody);
                        callback.onError(err.optString("message", "Login failed: " + response.code()));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Login error", e);
                callback.onError("Connection failed: " + e.getMessage());
            }
        }).start();
    }

    // ---- Get pending outgoing SMS from server ----
    public void getPendingSms(PendingSmsCallback callback) {
        new Thread(() -> {
            try {
                Request request = new Request.Builder()
                        .url(baseUrl + "/api/sms/pending")
                        .get()
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    String responseBody = response.body() != null ? response.body().string() : "[]";
                    if (response.isSuccessful()) {
                        JSONArray array;
                        // Handle both array and object wrapper
                        if (responseBody.trim().startsWith("[")) {
                            array = new JSONArray(responseBody);
                        } else {
                            JSONObject obj = new JSONObject(responseBody);
                            array = obj.optJSONArray("data");
                            if (array == null) array = new JSONArray();
                        }
                        callback.onSuccess(array);
                    } else {
                        callback.onError("HTTP " + response.code());
                    }
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ---- Report delivered ----
    public void reportDelivered(String messageId, SimpleCallback callback) {
        updateMessageStatus(messageId, "delivered", null, callback);
    }

    // ---- Report failed ----
    public void reportFailed(String messageId, String reason, SimpleCallback callback) {
        updateMessageStatus(messageId, "failed", reason, callback);
    }

    private void updateMessageStatus(String messageId, String status, String reason, SimpleCallback callback) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("status", status);
                if (reason != null) body.put("reason", reason);

                Request request = new Request.Builder()
                        .url(baseUrl + "/api/sms/" + messageId + "/status")
                        .patch(RequestBody.create(body.toString(), JSON))
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) callback.onSuccess();
                    else callback.onError("HTTP " + response.code());
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ---- Forward incoming SMS to server ----
    public void forwardIncomingSms(String from, String message, long timestamp, SimpleCallback callback) {
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("from", from);
                body.put("message", message);
                body.put("received_at", timestamp);

                Request request = new Request.Builder()
                        .url(baseUrl + "/api/sms/incoming")
                        .post(RequestBody.create(body.toString(), JSON))
                        .addHeader("Authorization", "Bearer " + token)
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    if (response.isSuccessful()) callback.onSuccess();
                    else callback.onError("HTTP " + response.code());
                }
            } catch (Exception e) {
                callback.onError(e.getMessage());
            }
        }).start();
    }

    // ---- Callbacks ----
    public interface LoginCallback {
        void onSuccess(LoginResponse response);
        void onError(String error);
    }

    public interface PendingSmsCallback {
        void onSuccess(JSONArray messages);
        void onError(String error);
    }

    public interface SimpleCallback {
        void onSuccess();
        void onError(String error);
    }
}
