package com.viovie.changedns;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.VpnService;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;

public class MainVpnService extends VpnService {


    private SharedPreferences prefs;
    private final IBinder mainBinder = new MyLocalBinder();
    private final String TAG = "MainVpnService";
    private Thread vpnThread;
    private ParcelFileDescriptor vpnInterface;
    Builder builder = new Builder();
    public static String dns1;
    public static String dns2;
    public static boolean isRunning;

    @Override
    public void onCreate() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // Start a new session by creating a new thread.
        vpnThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //a. Configure the TUN and get the interface.
                    vpnInterface = builder.setSession("ChangeDnsLocalVpn")
                            .setMtu(1500)
                            .addAddress("10.0.2.15", 24)
                            .addAddress("10.0.2.16", 24)
                            .addAddress("10.0.2.17", 24)
                            .addAddress("10.0.2.18", 24)
                            .addDnsServer(dns1)
                            .addDnsServer(dns2).establish();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }, "ChangeDnsVpnRunnable");

        //start the service in a separate thread
        vpnThread.start();
        isRunning = true;

        Intent in = new Intent();
        in.setAction("vpn.start");
        sendBroadcast(in);

        return START_STICKY;
    }


    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    public void kill() {
        try {
            if (vpnInterface != null) {
                vpnInterface.close();
                vpnInterface = null;
            }
            isRunning = false;
        } catch (Exception e) {

        }
        stopSelf();
    }

    @Override
    public void onDestroy() {
        if (vpnThread != null) {
            vpnThread.interrupt();
        }
        isRunning = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return mainBinder;
    }

    public class MyLocalBinder extends Binder {
        MainVpnService getService() {
            return MainVpnService.this;
        }
    }

}
