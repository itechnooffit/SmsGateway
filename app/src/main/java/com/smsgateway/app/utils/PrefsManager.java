package com.smsgateway.app.utils;
import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
public class PrefsManager {
    private static final String PREFS_NAME = "sms_gateway_prefs";
    private static final String KEY_SERVER_URL = "server_url";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_LOGGED_IN = "logged_in";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_POLL_INTERVAL = "poll_interval";
    private static final String KEY_SIM_SLOT = "sim_slot";
    private static final String KEY_AUTO_RETRY = "auto_retry";
    private static final String KEY_NOTIFICATIONS = "notifications";
    private static final String KEY_SENT_COUNT = "sent_count";
    private static final String KEY_FAILED_COUNT = "failed_count";
    private static final String KEY_MESSAGE_LOG = "message_log";
    private static final int MAX_LOG_ENTRIES = 100;
    private final SharedPreferences prefs;
    public PrefsManager(Context context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
    public void saveServerUrl(String url) { prefs.edit().putString(KEY_SERVER_URL, url).apply(); }
    public String getServerUrl() { return prefs.getString(KEY_SERVER_URL, ""); }
    public void saveEmail(String email) { prefs.edit().putString(KEY_EMAIL, email).apply(); }
    public String getEmail() { return prefs.getString(KEY_EMAIL, ""); }
    public void saveAuthToken(String token) { prefs.edit().putString(KEY_AUTH_TOKEN, token).apply(); }
    public String getAuthToken() { return prefs.getString(KEY_AUTH_TOKEN, ""); }
    public void setLoggedIn(boolean loggedIn) { prefs.edit().putBoolean(KEY_LOGGED_IN, loggedIn).apply(); }
    public boolean isLoggedIn() {
        return prefs.getBoolean(KEY_LOGGED_IN, false) &&
