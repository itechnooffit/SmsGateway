package com.smsgateway.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.smsgateway.app.utils.ApiClient;
import com.smsgateway.app.utils.PrefsManager;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";
    private static final String SMS_RECEIVED = "android.provider.Telephony.SMS_RECEIVED";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (!SMS_RECEIVED.equals(intent.getAction())) return;

        Bundle bundle = intent.getExtras();
        if (bundle == null) return;

        Object[] pdus = (Object[]) bundle.get("pdus");
        if (pdus == null) return;

        String format = bundle.getString("format");

        for (Object pdu : pdus) {
            SmsMessage smsMessage;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                smsMessage = SmsMessage.createFromPdu((byte[]) pdu, format);
            } else {
                smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
            }

            if (smsMessage == null) continue;

            String sender = smsMessage.getDisplayOriginatingAddress();
            String messageBody = smsMessage.getMessageBody();
            long timestamp = smsMessage.getTimestampMillis();

            Log.d(TAG, "SMS received from: " + sender);

            forwardToServer(context, sender, messageBody, timestamp);
        }
    }

    private void forwardToServer(Context context, String from, String message, long timestamp) {
        PrefsManager prefs = new PrefsManager(context);
        if (!prefs.isLoggedIn()) return;

        ApiClient apiClient = new ApiClient(prefs.getServerUrl(), prefs.getAuthToken());
        apiClient.forwardIncomingSms(from, message, timestamp, new ApiClient.SimpleCallback() {
            @Override
            public void onSuccess() {
                Log.d(TAG, "Forwarded SMS from " + from + " to server");
            }

            @Override
            public void onError(String error) {
                Log.w(TAG, "Failed to forward SMS: " + error);
            }
        });
    }
}
