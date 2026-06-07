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
                // Generate unique device ID from phone model
                String deviceAndroidId = "zikooo_" + android.os.Build.MODEL.replaceAll("\\s+", "_") + "_" + android.os.Build.SERIAL;
                final String[] androidId = {deviceAndroidId};
                final String[] userId = {USER_ID};
                if (password.startsWith("QR:")) {
                    // QR login - register new device first
                    String apiKey = password.substring(3);
                    String uniqueId = "zikooo_" + android.os.Build.MODEL.replaceAll("\\s+", "_") + "_" + System.currentTimeMillis();
                    RequestBody regBody = new FormBody.Builder()
                            .add("key", apiKey)
                            .add("androidId", uniqueId)
                            .add("model", android.os.Build.MODEL)
                            .add("androidVersion", android.os.Build.VERSION.RELEASE)
                            .add("appVersion", "1.0")
                            .build();
                    Request regReq = new Request.Builder()
                            .url(baseUrl + "/services/register-device.php")
                            .post(regBody)
                            .build();
                    try (Response r = client.newCall(regReq).execute()) {
                        String rb = r.body() != null ? r.body().string() : "";
                        Log.d(TAG, "QR Register: " + rb);
                        JSONObject j = new JSONObject(rb);
                        if (j.optBoolean("success", false)) {
                            JSONObject data = j.optJSONObject("data");
                            JSONObject device = data != null ? data.optJSONObject("device") : null;
                            if (device != null) {
                                userId[0] = String.valueOf(device.optInt("userID", 1));
                                androidId[0] = device.optString("androidID", uniqueId);
                            }
                        } else {
                            JSONObject err = j.optJSONObject("error");
                            callback.onError(err != null ? err.optString("message") : "QR failed");
                            return;
                        }
                    }
} else {
                    // Step 1: Verify email/password first
                    RequestBody verifyBody = new FormBody.Builder()
                            .add("email", email)
                            .add("password", password)
                            .build();
                    Request verifyReq = new Request.Builder()
                            .url(baseUrl + "/services/verify-credentials.php")
                            .post(verifyBody)
                            .build();
                    try (Response vr = client.newCall(verifyReq).execute()) {
                        String vrb = vr.body() != null ? vr.body().string() : "";
                        Log.d(TAG, "Verify: " + vrb);
                        JSONObject vj = new JSONObject(vrb);
                        if (!vj.optBoolean("success", false)) {
                            JSONObject err = vj.optJSONObject("error");
                            callback.onError(err != null ? err.optString("message") : "Invalid credentials");
                            return;
                        }
                        JSONObject vdata = vj.optJSONObject("data");
                        if (vdata != null) {
                            userId[0] = String.valueOf(vdata.optInt("userId", 1));
                        }
                    }
                    // Step 2: Register device using API key
                    String apiKey = "4070fcf23eb1ba2a28408447cc1256351241d768";
                    RequestBody regBody = new FormBody.Builder()
                            .add("key", apiKey)
                            .add("androidId", deviceAndroidId)
                            .add("model", android.os.Build.MODEL)
                            .add("androidVersion", android.os.Build.VERSION.RELEASE)
                            .add("appVersion", "1.0")
                            .build();
                    Request regReq = new Request.Builder()
                            .url(baseUrl + "/services/register-device.php")
                            .post(regBody)
                            .build();
                    try (Response r = client.newCall(regReq).execute()) {
                        String rb = r.body() != null ? r.body().string() : "";
                        Log.d(TAG, "Auto Register: " + rb);
                        JSONObject j = new JSONObject(rb);
                        if (j.optBoolean("success", false)) {
                            JSONObject data = j.optJSONObject("data");
                            JSONObject device = data != null ? data.optJSONObject("device") : null;
                            if (device != null) {
                                androidId[0] = device.optString("androidID", deviceAndroidId);
                            }
                        }
                    } catch (Exception ignored) {}
                }
                // Sign in
                RequestBody body = new FormBody.Builder()
                        .add("androidId", androidId[0])
                        .add("userId", userId[0])
                        .build();
                Request req = new Request.Builder()
                        .url(baseUrl + "/services/sign-in.php")
                        .post(body)
                        .build();
                try (Response r = client.newCall(req).execute()) {
                    String rb = r.body() != null ? r.body().string() : "";
                    Log.d(TAG, "SignIn: " + rb);
                    JSONObject j = new JSONObject(rb);
                    if (j.optBoolean("success", false)) {
                        JSONObject data = j.optJSONObject("data");
                        LoginResponse lr = new LoginResponse();
                        lr.token = data != null ? data.optString("sessionId", "") : "";
                        lr.userId = userId[0];
                        callback.onSuccess(lr);
                    } else {
                        JSONObject err = j.optJSONObject("error");
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
                        .add("userId", USER_ID)
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
