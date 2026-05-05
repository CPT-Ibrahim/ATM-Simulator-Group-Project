package com.atm.nfcreader;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    // -----------------------------------------------------------------------
    // CONFIG — change this port to match your Java ATM server port
    // -----------------------------------------------------------------------
    private static final String ATM_SERVER_URL = "http://localhost:8080/nfc";
    private static final int CONNECT_TIMEOUT_MS  = 3000;
    private static final int READ_TIMEOUT_MS     = 3000;
    // -----------------------------------------------------------------------

    private NfcAdapter nfcAdapter;
    private PendingIntent pendingIntent;
    private IntentFilter[] intentFiltersArray;
    private String[][] techListsArray;

    private TextView tvStatus;
    private TextView tvUID;
    private TextView tvInstruction;
    private View statusDot;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    // UI state constants
    private static final int STATE_WAITING  = 0;
    private static final int STATE_SENDING  = 1;
    private static final int STATE_SUCCESS  = 2;
    private static final int STATE_ERROR    = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus      = findViewById(R.id.tvStatus);
        tvUID         = findViewById(R.id.tvUID);
        tvInstruction = findViewById(R.id.tvInstruction);
        statusDot     = findViewById(R.id.statusDot);

        setupNFC();
        setUIState(STATE_WAITING, null, null);

        // Handle the case where app was launched by tapping an NFC tag
        handleIntent(getIntent());
    }

    // -----------------------------------------------------------------------
    // NFC Setup
    // -----------------------------------------------------------------------

    private void setupNFC() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this);

        if (nfcAdapter == null) {
            setUIState(STATE_ERROR, "NFC not available", "This device does not support NFC.");
            return;
        }

        if (!nfcAdapter.isEnabled()) {
            setUIState(STATE_ERROR, "NFC is OFF", "Please enable NFC in your device settings.");
            return;
        }

        // Foreground dispatch setup — this app gets priority when in foreground
        pendingIntent = PendingIntent.getActivity(
            this, 0,
            new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_MUTABLE
        );

        IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);
        IntentFilter techDetected = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED);
        intentFiltersArray = new IntentFilter[]{tagDetected, techDetected};

        techListsArray = new String[][]{
            new String[]{NfcA.class.getName()},
            new String[]{NfcB.class.getName()},
            new String[]{IsoDep.class.getName()},
            new String[]{NfcF.class.getName()},
            new String[]{NfcV.class.getName()},
            new String[]{MifareClassic.class.getName()},
            new String[]{MifareUltralight.class.getName()}
        };
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (nfcAdapter != null && nfcAdapter.isEnabled()) {
            nfcAdapter.enableForegroundDispatch(
                this, pendingIntent, intentFiltersArray, techListsArray
            );
            setUIState(STATE_WAITING, null, null);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (nfcAdapter != null) {
            nfcAdapter.disableForegroundDispatch(this);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleIntent(intent);
    }

    // -----------------------------------------------------------------------
    // NFC Tag Handling
    // -----------------------------------------------------------------------

    private void handleIntent(Intent intent) {
        if (intent == null) return;
        String action = intent.getAction();

        if (NfcAdapter.ACTION_TAG_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_TECH_DISCOVERED.equals(action)
            || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {

            Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            if (tag != null) {
                String uid = bytesToHex(tag.getId());
                onCardDetected(uid);
            }
        }
    }

    private void onCardDetected(String uid) {
        setUIState(STATE_SENDING, uid, "Authenticating...");
        sendUIDToATM(uid);
    }

    // -----------------------------------------------------------------------
    // Network — Send UID to ATM Java server via ADB tunnel
    // -----------------------------------------------------------------------

    private void sendUIDToATM(String uid) {
        executor.execute(() -> {
            try {
                URL url = new URL(ATM_SERVER_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
                conn.setReadTimeout(READ_TIMEOUT_MS);
                conn.setRequestProperty("Content-Type", "text/plain");
                conn.setRequestProperty("X-NFC-UID", uid);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(uid.getBytes("UTF-8"));
                    os.flush();
                }

                int code = conn.getResponseCode();
                conn.disconnect();

                if (code == 200) {
                    mainHandler.post(() ->
                        setUIState(STATE_SUCCESS, uid, "Card accepted — check ATM screen")
                    );
                } else {
                    mainHandler.post(() ->
                        setUIState(STATE_ERROR, uid, "Server responded with error " + code)
                    );
                }

            } catch (Exception e) {
                String hint = e.getMessage() != null && e.getMessage().contains("refused")
                    ? "Connection refused.\n\nMake sure:\n• Java ATM is running\n• Run: adb reverse tcp:8080 tcp:8080"
                    : "Failed: " + e.getMessage()
                        + "\n\nRun: adb reverse tcp:8080 tcp:8080";

                mainHandler.post(() -> setUIState(STATE_ERROR, uid, hint));
            }
        });
    }

    // -----------------------------------------------------------------------
    // UI State Machine
    // -----------------------------------------------------------------------

    private void setUIState(int state, String uid, String message) {
        switch (state) {
            case STATE_WAITING:
                statusDot.setBackgroundResource(R.drawable.dot_blue);
                tvInstruction.setText("Tap your NFC card to authenticate");
                tvStatus.setText("Waiting for card...");
                tvUID.setVisibility(View.GONE);
                break;

            case STATE_SENDING:
                statusDot.setBackgroundResource(R.drawable.dot_yellow);
                tvInstruction.setText("Hold still...");
                tvStatus.setText("Sending to ATM server");
                tvUID.setVisibility(View.VISIBLE);
                tvUID.setText("UID: " + uid);
                break;

            case STATE_SUCCESS:
                statusDot.setBackgroundResource(R.drawable.dot_green);
                tvInstruction.setText("Tap another card to authenticate again");
                tvStatus.setText(message);
                tvUID.setVisibility(View.VISIBLE);
                tvUID.setText("UID: " + uid);
                // Auto-reset to waiting after 4 seconds
                mainHandler.postDelayed(() -> setUIState(STATE_WAITING, null, null), 4000);
                break;

            case STATE_ERROR:
                statusDot.setBackgroundResource(R.drawable.dot_red);
                tvInstruction.setText("Tap to retry");
                tvStatus.setText(message);
                if (uid != null) {
                    tvUID.setVisibility(View.VISIBLE);
                    tvUID.setText("UID: " + uid);
                }
                // Auto-reset to waiting after 6 seconds
                mainHandler.postDelayed(() -> setUIState(STATE_WAITING, null, null), 6000);
                break;
        }
    }

    // -----------------------------------------------------------------------
    // Utility
    // -----------------------------------------------------------------------

    /**
     * Converts NFC tag byte array ID to uppercase hex string.
     * e.g., [0xA1, 0xB2, 0xC3, 0xD4] → "A1B2C3D4"
     */
    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b));
        }
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
