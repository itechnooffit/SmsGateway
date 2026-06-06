package com.smsgateway.app.utils;

import android.util.Log;
import com.smsgateway.app.models.LoginResponse;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.concurrent.TimeUnit;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {
    private static final String TAG = "ApiClient";
    private final String baseUrl;
    private final String sessionId;
    private final OkHttpClient client;

    private static final String ANDROID_ID = "smsgate_device";
    private static final String USER_ID = "1";

    public ApiClient(String baseUrl) { this(baseUrl, null); }

    public ApiClient(String baseUrl, String sessionId) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length()-1) : baseUrl;
        this.sessionId = sessionId;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    public void login(String email, String password, LoginCallback callback) {
        new Thread(() -> {
            try {
                final String[] androidId = {ANDROID_ID};
                final String[] userId = {USER_ID};

                if (password.startsWith("QR:")) {
                    String apiKey = password.substring(3);
                    RequestBody body = new FormBody.Builder()
                            .add("key", apiKey)
                            .add("androidId", androidId[0])
                            .add("userId", userId[0])
                            .build();
                    Request req = new Request.Builder()
                            .url(baseUrl + "/services/sign-in.php")
                            .post(body)
                            .build();
                    try (Response r = client.newCall(req).execute()) {
                        String rb = r.body() != null ? r.body().string() : "";
                        Log.d(TAG, "QR SignIn: " + rb);
                        JSONObject j = new JSONObject(rb);
                        if (j.optBoolean("success", false)) {
                            JSONObject data = j.optJSONObject("data");
                            LoginResponse lr = new LoginResponse();
                            lr.token = data != null ? data.optString("sessionId", "") : "";
                            lr.userId = userId[0];
                            callback.onSuccess(lr);
                        } else {
                            JSONObject err = j.optJSONObject("error");
                            callback.onError(err != null ? err.optString("message") : "QR login failed");
                        }
                        return;
                    }
                }

                RequestBody body2 = new FormBody.Builder()
                        .add("androidId", androidId[0])
                        .add("userId", userId[0])
                        .build();
                Request req2 = new Request.Builder()
                        .url(baseUrl + "/services/sign-in.php")
                        .post(body2)
                        .build();
                try (Response r2 = client.newCall(req2).execute()) {
                    String rb2 = r2.body() != null ? r2.body().string() : "";
                    Log.d(TAG, "SignIn: " + rb2);
                    JSONObject j2 = new JSONObject(rb2);
                    if (j2.optBoolean("success", false)) {
                        JSONObject data = j2.optJSONObject("data");
                        LoginResponse lr = new LoginResponse();
                        lr.token = data != null ? data.optString("sessionId", "") : "";
                        lr.userId = userId[0];
                        callback.onSuccess(lr);
                    } else {
                        JSONObject err = j2.optJSONObject("error");
                        callback.onError(err != null ? err.optString("message") : "Sign in failed");
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Login error", e);
                callback.onError("Connection failed: " + e.getMessage());
            }
        }).start();
    }

    public void getPendingSms(PendingSmsCallback callback) {
        new Thread(() -> {
            try {
                RequestBody body = new FormBody.Builder()
                        .add("groupId", "1")
                        .build();
                Request req = buildRequest("/services/get-messages.php", body);
                try (Response r = client.newCall(req).execute()) {
                    String rb = r.body() != null ? r.body().string() : "{}";
                    JSONObject j = new JSONObject(rb);
                    if (j.optBoolean("success", false)) {
                        JSONObject data = j.optJSONObject("data");
                        JSONArray messages = data != null ? data.optJSONArray("messages") : new JSONArray();
                        if (messages == null) messages = new JSONArray();
                        callback.onSuccess(messages);
                    } else {
                        callback.onError("No messages");
                    }
                }
            } catch (Exception e) { callback.onError(e.getMessage()); }
        }).start();
    }

    public void reportDelivered(String messageId, SimpleCallback callback) {
        reportStatus(messageId, "Delivered", callback);
    }

    public void reportFailed(String messageId, String reason, SimpleCallback callback) {
        reportStatus(messageId, "Failed", callback);
    }

    private void reportStatus(String messageId, String status, SimpleCallback callback) {
        new Thread(() -> {
            try {
                JSONArray messages = new JSONArray();
                JSONObject msg = new JSONObject();
                msg.put("ID", messageId);
                msg.put("status", status);
                msg.put("deliveredDate", new java.util.Date().toString());
                messages.put(msg);
                RequestBody body = new FormBody.Builder()
                        .add("messages", messages.toString())
                        .build();
                Request req = buildRequest("/services/report-status.php", body);
                try (Response r = client.newCall(req).execute()) {
                    if (r.isSuccessful()) callback.onSuccess();
                    else callback.onError("HTTP " + r.code());
                }
            } catch (Exception e) { callback.onError(e.getMessage()); }
        }).start();
    }

    public void forwardIncomingSms(String from, String message, long timestamp, SimpleCallback callback) {
        new Thread(() -> {
            try {
                RequestBody body = new FormBody.Builder()
                        .add("from", from)
                        .add("message", message)
                        .add("receivedDate", new java.util.Date(timestamp).toString())
                        .build();
                Request req = buildRequest("/services/receive-message.php", body);
                try (Response r = client.newCall(req).execute()) {
                    if (r.isSuccessful()) callback.onSuccess();
                    else callback.onError("HTTP " + r.code());
                }
            } catch (Exception e) { callback.onError(e.getMessage()); }
        }).start();
    }

    private Request buildRequest(String path, RequestBody body) {
        Request.Builder builder = new Request.Builder()
                .url(baseUrl + path)
                .post(body);
        if (sessionId != null && !sessionId.isEmpty()) {
            builder.addHeader("Cookie", "ZERO_SMS=" + sessionId);
        }
        return builder.build();
    }

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
