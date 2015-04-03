package com.example.jpushdemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

/**
 * Created by yukang on 2015/4/2.
 */
public class WifiReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
        if(info != null) {
           if(info.isConnected()) {
                // Do your work.

                // e.g. To check the Network Name or other info:
                WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                String ssid = wifiInfo.getSSID();
                if(ssid.contains("clear-guest")) {
                    Uri uri = Uri.parse("https://webauth-redirect.oracle.com/login.html?username=guest&password=WrDX6YKa&buttonClicked=4");
                    Intent it = new Intent(Intent.ACTION_VIEW, uri);
                    it.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(it);
                }
            }
        }
    }
}