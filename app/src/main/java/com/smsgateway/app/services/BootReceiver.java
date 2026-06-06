package com.smsgateway.app.services;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.smsgateway.app.utils.PrefsManager;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) ||
                "android.intent.action.QUICKBOOT_POWERON".equals(action)) {

            PrefsManager prefs = new PrefsManager(context);
            if (prefs.isLoggedIn()) {
                Intent serviceIntent = new Intent(context, SmsGatewayService.class);
                serviceIntent.putExtra("server", prefs.getServerUrl());
                serviceIntent.putExtra("token", prefs.getAuthToken());
                context.startForegroundService(serviceIntent);
            }
        }
    }
}
