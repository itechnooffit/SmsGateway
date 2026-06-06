package com.smsgateway.app.activities;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.smsgateway.app.R;
import com.smsgateway.app.utils.PrefsManager;

import java.util.List;

public class LogActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.message_log);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        PrefsManager prefs = new PrefsManager(this);
        ListView listView = findViewById(R.id.listLog);
        TextView tvEmpty = findViewById(R.id.tvEmpty);

        List<String> logs = prefs.getMessageLog();
        if (logs.isEmpty()) {
            tvEmpty.setVisibility(android.view.View.VISIBLE);
            listView.setVisibility(android.view.View.GONE);
        } else {
            tvEmpty.setVisibility(android.view.View.GONE);
            listView.setVisibility(android.view.View.VISIBLE);
            ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                    android.R.layout.simple_list_item_1, logs);
            listView.setAdapter(adapter);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
