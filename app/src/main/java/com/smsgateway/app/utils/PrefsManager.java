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
        return prefs.getBoolean(KEY_LOGGED_IN, false) && !getAuthToken().isEmpty() && !getServerUrl().isEmpty();
    }
    public void saveLanguage(String langCode) { prefs.edit().putString(KEY_LANGUAGE, langCode).apply(); }
    public String getLanguage() { return prefs.getString(KEY_LANGUAGE, "en"); }
    public void savePollInterval(int seconds) { prefs.edit().putInt(KEY_POLL_INTERVAL, seconds).apply(); }
    public int getPollInterval() { return prefs.getInt(KEY_POLL_INTERVAL, 5); }
    public void saveSimSlot(int slot) { prefs.edit().putInt(KEY_SIM_SLOT, slot).apply(); }
    public int getSimSlot() { return prefs.getInt(KEY_SIM_SLOT, 0); }
    public void saveAutoRetry(boolean enabled) { prefs.edit().putBoolean(KEY_AUTO_RETRY, enabled).apply(); }
    public boolean isAutoRetry() { return prefs.getBoolean(KEY_AUTO_RETRY, true); }
    public void saveNotificationsEnabled(boolean enabled) { prefs.edit().putBoolean(KEY_NOTIFICATIONS, enabled).apply(); }
    public boolean isNotificationsEnabled() { return prefs.getBoolean(KEY_NOTIFICATIONS, true); }
    public void incrementSentCount() { prefs.edit().putInt(KEY_SENT_COUNT, getSentCount() + 1).apply(); }
    public int getSentCount() { return prefs.getInt(KEY_SENT_COUNT, 0); }
    public void incrementFailedCount() { prefs.edit().putInt(KEY_FAILED_COUNT, getFailedCount() + 1).apply(); }
    public int getFailedCount() { return prefs.getInt(KEY_FAILED_COUNT, 0); }
    public void addMessageLog(String entry) {
        List<String> logs = getMessageLog();
        String timestamp = new SimpleDateFormat("MM/dd HH:mm", Locale.getDefault()).format(new Date());
        logs.add(0, timestamp + " — " + entry);
        if (logs.size() > MAX_LOG_ENTRIES) logs = logs.subList(0, MAX_LOG_ENTRIES);
        StringBuilder sb = new StringBuilder();
        for (String log : logs) sb.append(log).append("|||");
        prefs.edit().putString(KEY_MESSAGE_LOG, sb.toString()).apply();
    }
    public List<String> getMessageLog() {
        String raw = prefs.getString(KEY_MESSAGE_LOG, "");
        List<String> list = new ArrayList<>();
        if (!raw.isEmpty()) {
            for (String entry : raw.split("\\|\\|\\|")) {
                if (!entry.trim().isEmpty()) list.add(entry);
            }
        }
        return list;
    }
    public void clearSession() {
        prefs.edit().remove(KEY_AUTH_TOKEN).remove(KEY_LOGGED_IN).apply();
    }
    public void setActivated(boolean activated) {
        prefs.edit().putBoolean("activated", activated).apply();
    }
    public boolean isActivated() {
        return prefs.getBoolean("activated", false);
    }
}
