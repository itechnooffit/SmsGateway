package com.smsgateway.app.activities;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.smsgateway.app.R;
import com.smsgateway.app.utils.PrefsManager;

import org.json.JSONObject;

public class QrActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.qr_code);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        PrefsManager prefs = new PrefsManager(this);
        ImageView ivQr = findViewById(R.id.ivQrCode);
        TextView tvInstructions = findViewById(R.id.tvInstructions);
        TextView tvServer = findViewById(R.id.tvServer);

        tvInstructions.setText(R.string.qr_scan_instructions);
        tvServer.setText(prefs.getServerUrl());

        try {
            JSONObject qrContent = new JSONObject();
            qrContent.put("server", prefs.getServerUrl());
            qrContent.put("key", prefs.getAuthToken());
            Bitmap qrBitmap = generateQrCode(qrContent.toString(), 600);
            ivQr.setImageBitmap(qrBitmap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Bitmap generateQrCode(String content, int size) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size);
        Bitmap bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565);
        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                bitmap.setPixel(x, y, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bitmap;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
