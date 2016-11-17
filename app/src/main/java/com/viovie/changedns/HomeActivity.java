package com.viovie.changedns;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.viovie.changedns.MainVpnService.MyLocalBinder;
import com.viovie.changedns.receivers.VpnStatusReceiver;

public class HomeActivity extends Activity {

    private static final String TAG = "HomeActivity";

    MainVpnService mainService;
    boolean isBound = false;

    private SharedPreferences prefs;

    private View connectBtn;
    private View disconnectBtn;
    private EditText editTextDNS1;
    private EditText editTextDNS2;
    private TextView textViewDNS1;
    private TextView textViewDNS2;

    /**
     * Called when the activity is first created.
     */
    private VpnStatusReceiver vpnStatusReceiver;
    private IntentFilter filter;

    @Override
    protected void onPause() {
        super.onPause();
        if (isBound) {
            this.unbindService(mainConnection);
            isBound = false;
        }
        unregisterReceiver(vpnStatusReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (MainVpnService.isRunning && !isBound) {
            this.bindService(new Intent(this, MainVpnService.class), mainConnection, Context.BIND_AUTO_CREATE);
            updateConnectedUI();
        } else {
            updateDisconnectedUI();
        }
        registerReceiver(vpnStatusReceiver, filter);
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        //Initiate shared preferences
        prefs = this.getSharedPreferences("com.viovie.changedns", Context.MODE_PRIVATE);
        connectBtn = (View) findViewById(R.id.button3);
        disconnectBtn = (View) findViewById(R.id.button4);
        editTextDNS1 = (EditText) findViewById(R.id.editTextDNS1);
        editTextDNS2 = (EditText) findViewById(R.id.editTextDNS2);
        textViewDNS1 = (TextView) findViewById(R.id.textViewDNS1);
        textViewDNS2 = (TextView) findViewById(R.id.textViewDNS2);

        vpnStatusReceiver = new VpnStatusReceiver() {
            // this code is call asyncrously from the receiver
            @Override
            public void onVpnStartReceived() {
                updateConnectedUI();
            }
        };

        filter = new IntentFilter("vpn.start");
        this.registerReceiver(vpnStatusReceiver, filter);

        // Load DNS settings
        String strDNS1 = prefs.getString("dns1", "8.8.8.8");
        String strDNS2 = prefs.getString("dns2", "8.8.4.4");
        editTextDNS1.setText(strDNS1);
        editTextDNS2.setText(strDNS2);

        if (MainVpnService.isRunning) {
            this.bindService(new Intent(this, MainVpnService.class), mainConnection, Context.BIND_AUTO_CREATE);
            updateConnectedUI();
        } else {
            updateDisconnectedUI();
        }
    }

    public void updateConnectedUI() {
        connectBtn.setVisibility(View.GONE);
        disconnectBtn.setVisibility(View.VISIBLE);
        editTextDNS1.setVisibility(View.GONE);
        editTextDNS2.setVisibility(View.GONE);
        textViewDNS1.setVisibility(View.GONE);
        textViewDNS2.setVisibility(View.GONE);
    }

    public void updateDisconnectedUI() {
        connectBtn.setVisibility(View.VISIBLE);
        disconnectBtn.setVisibility(View.GONE);
        editTextDNS1.setVisibility(View.VISIBLE);
        editTextDNS2.setVisibility(View.VISIBLE);
        textViewDNS1.setVisibility(View.VISIBLE);
        textViewDNS2.setVisibility(View.VISIBLE);
    }

    public void connect(View view) {
        prefs.edit().putString("dns1", editTextDNS1.getText().toString()).apply();
        prefs.edit().putString("dns2", editTextDNS2.getText().toString()).apply();

        MainVpnService.dns1 = editTextDNS1.getText().toString();
        MainVpnService.dns2 = editTextDNS2.getText().toString();

        Intent intent = VpnService.prepare(this);
        if (intent != null) {
            startActivityForResult(intent, 0);
        } else {
            onActivityResult(0, RESULT_OK, null);
        }
    }

    public void disconnect(View view) {
        mainService.kill();
        updateDisconnectedUI();
    }

    @Override
    protected void onActivityResult(int request, int result, Intent data) {
        if (result == RESULT_OK) {
            Intent i = new Intent(this, MainVpnService.class);
            startService(i);
            this.bindService(new Intent(this, MainVpnService.class), mainConnection, Context.BIND_AUTO_CREATE);
        } else {
            super.onActivityResult(request, result, data);
        }
    }

    private ServiceConnection mainConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MyLocalBinder binder = (MyLocalBinder) service;
            mainService = binder.getService();
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public void onDestroy() {
        if (isBound) {
            this.unbindService(mainConnection);
        }
        super.onDestroy();
    }
}
