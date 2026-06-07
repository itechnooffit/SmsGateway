package com.smsgateway.app.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.smsgateway.app.R;
import com.smsgateway.app.activities.MainActivity;
import com.smsgateway.app.utils.ApiClient;
import com.smsgateway.app.utils.PrefsManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;

public class SmsGatewayService extends Service {

    private static final String TAG = "SmsGatewayService";
    private static final String CHANNEL_ID = "sms_gateway_channel";
    private static final int NOTIFICATION_ID = 1;

    public static boolean isRunning = false;

    private Handler handler;
    private Runnable pollRunnable;
    private ApiClient apiClient;
    private PrefsManager prefsManager;
    private int simSlot = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        prefsManager = new PrefsManager(this);
        handler = new Handler();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String server = intent != null ? intent.getStringExtra("server") : prefsManager.getServerUrl();
        String token = intent != null ? intent.getStringExtra("token") : prefsManager.getAuthToken();
        simSlot = intent != null ? intent.getIntExtra("simSlot", 0) : prefsManager.getSimSlot();

        apiClient = new ApiClient(server, token);
        isRunning = true;

        startForeground(NOTIFICATION_ID, buildNotification());
        startPolling();
        return START_STICKY;
    }

    private void startPolling() {
        long intervalMs = prefsManager.getPollInterval() * 1000L;
        pollRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    pollPendingMessages();
                    handler.postDelayed(this, intervalMs);
                }
            }
        };
        handler.post(pollRunnable);
    }

    private void pollPendingMessages() {
        apiClient.getPendingSms(new ApiClient.PendingSmsCallback() {
            @Override
            public void onSuccess(JSONArray messages) {
                for (int i = 0; i < messages.length(); i++) {
                    try {
                        JSONObject msg = messages.getJSONObject(i);
                        String id = msg.getString("ID");
                        String to = msg.getString("number");
                        String text = msg.getString("message");
                        int msgSimSlot = msg.isNull("simSlot") ? simSlot : msg.getInt("simSlot");
                        sendSmsMessage(id, to, text, msgSimSlot);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing message", e);
                    }
                }
            }
            @Override
            public void onError(String error) {
                Log.w(TAG, "Poll error: " + error);
            }
        });
    }

    private void sendSmsMessage(String messageId, String to, String text, int msgSimSlot) {
        try {
            SmsManager smsManager;
            if (msgSimSlot == 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                smsManager = SmsManager.getSmsManagerForSubscriptionId(getSubscriptionId(0));
            } else if (msgSimSlot == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                smsManager = SmsManager.getSmsManagerForSubscriptionId(getSubscriptionId(1));
            } else {
                smsManager = SmsManager.getDefault();
            }

            if (text.length() > 160) {
                ArrayList<String> parts = smsManager.divideMessage(text);
                smsManager.sendMultipartTextMessage(to, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(to, null, text, null, null);
            }

            prefsManager.incrementSentCount();
            prefsManager.addMessageLog("✓ Sent to " + to + " — " + text.substring(0, Math.min(30, text.length())));
            broadcastStatsUpdate();

            if (prefsManager.isNotificationsEnabled()) {
                showSmsNotification("SMS Sent", "To: " + to);
            }

            apiClient.reportDelivered(messageId, new ApiClient.SimpleCallback() {
                @Override public void onSuccess() {}
                @Override public void onError(String error) {}
            });

        } catch (Exception e) {
            Log.e(TAG, "Failed to send SMS to " + to, e);
            prefsManager.incrementFailedCount();
            prefsManager.addMessageLog("✗ Failed to " + to + " — " + e.getMessage());
            broadcastStatsUpdate();

            if (prefsManager.isAutoRetry()) {
                handler.postDelayed(() -> sendSmsMessage(messageId, to, text, msgSimSlot), 30000);
            } else {
                apiClient.reportFailed(messageId, e.getMessage(), new ApiClient.SimpleCallback() {
                    @Override public void onSuccess() {}
                    @Override public void onError(String error) {}
                });
            }
        }
    }

    private int getSubscriptionId(int slotIndex) {
        try {
            android.telephony.SubscriptionManager sm = (android.telephony.SubscriptionManager)
                    getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE);
            if (sm != null) {
                java.util.List<android.telephony.SubscriptionInfo> list = sm.getActiveSubscriptionInfoList();
                if (list != null && list.size() > slotIndex) {
                    return list.get(slotIndex).getSubscriptionId();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting subscription ID", e);
        }
        return SmsManager.getDefaultSmsSubscriptionId();
    }

    private void broadcastStatsUpdate() {
        sendBroadcast(new Intent("com.smsgateway.STATS_UPDATED"));
    }

    private void showSmsNotification(String title, String message) {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(message)
                .setSmallIcon(R.drawable.ic_sms)
                .setAutoCancel(true)
                .build();
        if (manager != null) manager.notify((int) System.currentTimeMillis(), notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "SMS Gateway Service", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Running SMS gateway in background");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.service_running))
                .setSmallIcon(R.drawable.ic_sms)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        isRunning = false;
        if (handler != null && pollRunnable != null) handler.removeCallbacks(pollRunnable);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }
}
