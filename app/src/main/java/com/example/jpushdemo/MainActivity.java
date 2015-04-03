package com.example.jpushdemo;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.oracle.app.owifi.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import cn.jpush.android.api.InstrumentedActivity;
import cn.jpush.android.api.JPushInterface;


public class MainActivity extends InstrumentedActivity implements OnClickListener {

    private static final String TAG = "owifi";

    private Button mInit;
    private Button mSetting;
    private Button mStopPush;
    private Button mResumePush;
    private Button mConnectWifi;
    private EditText msgText;
    private TextView serverTimeText;
    private WifiManager mWifiManager;
    private List<ScanResult> mScanResults;
    private boolean connected = false;

    public static boolean isForeground = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        initView();
        registerMessageReceiver();  // used for receive msg
        mWifiManager = (WifiManager) getSystemService(WIFI_SERVICE);

        SharedPreferences sharedPref = getSharedPreferences("owifi", MODE_PRIVATE);
        String loginUrl = sharedPref.getString("loginUrl", "");
        String serverTime = sharedPref.getString("serverTime", "");
        if (loginUrl != null && !"".equals(loginUrl)) {
            setCostomMsg(loginUrl, serverTime);
        }
    }


    private BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (connected) {
                return;
            }
            if (action.equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                mScanResults = mWifiManager.getScanResults();


                // mWifiManager.startScan();
                boolean clearGuestVisable = false;
                ScanResult mScanResult = null;
                for (ScanResult sr : mScanResults) {
                    if ("clear-guest".equals(sr.SSID)) {
                        clearGuestVisable = true;
                        mScanResult = sr;
                        Log.d(TAG, "Can find 'clear-guest' wifi");
                        break;
                    }
                }
                //boolean  connResult = Wifi.connectToNewNetwork(getApplicationContext(), mWifiManager, mScanResult, null, 10);
                if (mScanResult != null) {
                    final String security = Wifi.ConfigSec.getScanResultSecurity(mScanResult);
                    final WifiConfiguration config = Wifi.getWifiConfiguration(mWifiManager, mScanResult, security);
                    //boolean result = Wifi.connectToConfiguredNetwork(getApplicationContext(), mWifiManager, config, false);
                    boolean result = Wifi.connectToNewNetwork(getApplicationContext(), mWifiManager, mScanResult, null, 10);

                    if (result) {
                        connected = true;


                    }
                }
            }


        }
    };


    private void initView() {
        TextView mImei = (TextView) findViewById(R.id.tv_imei);
        String udid = ExampleUtil.getImei(getApplicationContext(), "");
        if (null != udid) mImei.setText("IMEI: " + udid);

        TextView mAppKey = (TextView) findViewById(R.id.tv_appkey);
        String appKey = ExampleUtil.getAppKey(getApplicationContext());
        if (null == appKey) appKey = "AppKey异常";
        mAppKey.setText("AppKey: " + appKey);

        String packageName = getPackageName();
        TextView mPackage = (TextView) findViewById(R.id.tv_package);
        mPackage.setText("PackageName: " + packageName);

        String versionName = ExampleUtil.GetVersion(getApplicationContext());
        TextView mVersion = (TextView) findViewById(R.id.tv_version);
        mVersion.setText("Version: " + versionName);

        mInit = (Button) findViewById(R.id.init);
        mInit.setOnClickListener(this);

        mStopPush = (Button) findViewById(R.id.stopPush);
        mStopPush.setOnClickListener(this);

        mResumePush = (Button) findViewById(R.id.resumePush);
        mResumePush.setOnClickListener(this);

        mConnectWifi = (Button) findViewById(R.id.connectWIFI);
        mConnectWifi.setOnClickListener(this);

        mSetting = (Button) findViewById(R.id.setting);
        mSetting.setOnClickListener(this);

        msgText = (EditText) findViewById(R.id.msg_rec);

        serverTimeText = (TextView) findViewById(R.id.serverTime);
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.init:
                init();
                break;
            case R.id.setting:
                Intent intent = new Intent(MainActivity.this, PushSetActivity.class);
                startActivity(intent);
                break;
            case R.id.stopPush:
                JPushInterface.stopPush(getApplicationContext());
                break;
            case R.id.resumePush:
                JPushInterface.resumePush(getApplicationContext());
                break;
            case R.id.connectWIFI:
                connectWifi();
                break;
        }
    }

    // 初始化 JPush。如果已经初始化，但没有登录成功，则执行重新登录。
    private void init() {
        JPushInterface.init(getApplicationContext());
    }

    private void connectWifi() {
        boolean clearGuestVisable = false;
        mWifiManager.startScan();

    }

    @Override
    protected void onResume() {
        isForeground = true;
        super.onResume();
        final IntentFilter filter = new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(mReceiver, filter);

    }


    @Override
    protected void onPause() {
        isForeground = false;
        super.onPause();
    }


    @Override
    protected void onDestroy() {
        unregisterReceiver(mMessageReceiver);
        unregisterReceiver(mReceiver);
        super.onDestroy();
    }


    //for receive customer msg from jpush server
    private MessageReceiver mMessageReceiver;
    public static final String MESSAGE_RECEIVED_ACTION = "com.example.jpushdemo.MESSAGE_RECEIVED_ACTION";
    public static final String KEY_TITLE = "title";
    public static final String KEY_MESSAGE = "message";
    public static final String KEY_EXTRAS = "extras";

    public void registerMessageReceiver() {
        mMessageReceiver = new MessageReceiver();
        IntentFilter filter = new IntentFilter();
        filter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        filter.addAction(MESSAGE_RECEIVED_ACTION);
        registerReceiver(mMessageReceiver, filter);
    }

    public class MessageReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (MESSAGE_RECEIVED_ACTION.equals(intent.getAction())) {
                String message = intent.getStringExtra(KEY_MESSAGE);
                String extras = intent.getStringExtra(KEY_EXTRAS);
                String serverTime = null;
                try {
                    JSONObject jsonObject = new JSONObject(extras);
                    serverTime = (String) jsonObject.get("serverTime");
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                StringBuilder showMsg = new StringBuilder();
                showMsg.append(message + "\n");
                if (!ExampleUtil.isEmpty(extras)) {
                    //showMsg.append(KEY_EXTRAS + " : " + extras + "\n");
                }

                setCostomMsg(showMsg.toString(), serverTime);

                //connectWifi();


            }
        }
    }

    private void setCostomMsg(String msg, String time) {
        if (null != msgText) {
            msgText.setText(msg);
            msgText.setVisibility(View.VISIBLE);
        }
        if (null != serverTimeText) {
            serverTimeText.setVisibility(View.VISIBLE);
            serverTimeText.setText("" + time);
        }
    }

}