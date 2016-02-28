package com.me.slacy.sl.bonjour_test;

import android.content.Context;
import android.graphics.Color;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.net.nsd.NsdServiceInfo;
import android.net.nsd.NsdManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.pes.androidmaterialcolorpickerdialog.ColorPicker;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

public class MainActivity extends AppCompatActivity {


    private TextView textView;
    private Button scanButton;
    private Button chooseColor;
    private Switch tSwitch;
    NsdManager manager;
    NsdManager.ResolveListener resolveListener;
    NsdManager.DiscoveryListener discoveryListener;
    private Boolean isDiscoveryStarted = false;

    public static final String SERVICE = "_arduino._tcp";
    public static final String TAG = "YUN-search";
    public String serviceName = "YUN-search";
    public static String deviceHost = "";

    private static int defaultColorR = 0;
    private static int defaultColorG = 200;
    private static int defaultColorB = 200;

    private static String initStatus = "Searching";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final RelativeLayout mainLayout = (RelativeLayout) findViewById(R.id.main_layout);
        mainLayout.setBackgroundColor(Color.argb(255, defaultColorR, defaultColorG, defaultColorB));

        final ColorPicker cp = new ColorPicker(MainActivity.this, defaultColorR, defaultColorG, defaultColorB);

        textView = (TextView) findViewById(R.id.editText);
//        scanButton = (Button) findViewById(R.id.scanButton);
        chooseColor = (Button) findViewById(R.id.chooseColor);
        tSwitch = (Switch) findViewById(R.id.switch1);


        tSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    defaultColorR = 0;
                    defaultColorG = 200;
                    defaultColorB = 200;
                    mainLayout.setBackgroundColor(Color.argb(255, defaultColorR, defaultColorG, defaultColorB));
                    String deviceUrl = "http://" + deviceHost + "/arduino/led/" + defaultColorR + "/" + defaultColorG + "/" + defaultColorB;
                    httpRequest(deviceUrl);
                } else {
                    defaultColorR = 0;
                    defaultColorG = 0;
                    defaultColorB = 0;
                    mainLayout.setBackgroundColor(Color.argb(255, defaultColorR, defaultColorG, defaultColorB));
                    String deviceUrl = "http://" + deviceHost + "/arduino/led/" + defaultColorR + "/" + defaultColorG + "/" + defaultColorB;
                    httpRequest(deviceUrl);
                }
            }

        });

        manager = (NsdManager) this.getSystemService(Context.NSD_SERVICE);

        initializeDiscoveryListener();
        initializeResolveListener();

        discoverServices();

//        scanButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                discoverServices();
//            }
//        });

        chooseColor.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            cp.show();
            Button okColor = (Button) cp.findViewById(R.id.okColorButton);
            okColor.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                defaultColorR = cp.getRed();
                defaultColorG = cp.getGreen();
                defaultColorB = cp.getBlue();
                mainLayout.setBackgroundColor(Color.argb(255, defaultColorR, defaultColorG, defaultColorB));

                String deviceUrl = "http://" + deviceHost + "/arduino/led/" + defaultColorR + "/" + defaultColorG + "/" + defaultColorB;
                httpRequest(deviceUrl);
                cp.dismiss();
                }
            });

            }
        });


    }


    public void initializeDiscoveryListener() {

        // Instantiate a new DiscoveryListener
        discoveryListener = new NsdManager.DiscoveryListener() {

            //  Called as soon as service discovery begins.
            @Override
            public void onDiscoveryStarted(String regType) {
                Log.d(TAG, "Service discovery started");
            }

            @Override
            public void onServiceFound(NsdServiceInfo service) {
                // A service was found!  Do something with it.
                Log.d(TAG, "Service discovery success " + service);

                manager.resolveService(service, resolveListener);

                Log.d(TAG, service.getServiceName());
                Log.d(TAG, service.getServiceType());

            }

            @Override
            public void onServiceLost(NsdServiceInfo service) {
                // When the network service is no longer available.
                // Internal bookkeeping code goes here.
                Log.e(TAG, "service lost" + service);
            }

            @Override
            public void onDiscoveryStopped(String serviceType) {
                Log.i(TAG, "Discovery stopped: " + serviceType);
            }

            @Override
            public void onStartDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                manager.stopServiceDiscovery(this);
            }

            @Override
            public void onStopDiscoveryFailed(String serviceType, int errorCode) {
                Log.e(TAG, "Discovery failed: Error code:" + errorCode);
                manager.stopServiceDiscovery(this);
            }
        };
    }
    public void initializeResolveListener() {
        resolveListener = new NsdManager.ResolveListener() {

            @Override
            public void onResolveFailed(NsdServiceInfo serviceInfo, int errorCode) {
                // Called when the resolve fails.  Use the error code to debug.
                Log.e(TAG, "Resolve failed" + errorCode);
            }

            @Override
            public void onServiceResolved(NsdServiceInfo serviceInfo) {
                Log.e(TAG, "Resolve Succeeded. " + serviceInfo);

                if (serviceInfo.getServiceName().equals(serviceName)) {
                    Log.d(TAG, "Same IP.");
                    return;
                }

                final InetAddress host = serviceInfo.getHost();
                deviceHost = host.toString().replaceAll("/","");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        textView.setText(deviceHost);
                    }
                });
            }
        };
    }

    public void discoverServices() {
        if (isDiscoveryStarted) {
            return;
        }
        isDiscoveryStarted = true;
        manager.discoverServices(SERVICE, NsdManager.PROTOCOL_DNS_SD, discoveryListener);
    }

    public void httpRequest(final String deviceUrl) {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                Log.d(TAG, deviceUrl);
                URL url;
                HttpURLConnection urlConnection = null;
                try {
                    url = new URL(deviceUrl);

                    urlConnection = (HttpURLConnection) url.openConnection();
                    InputStream in = urlConnection.getInputStream();
                    InputStreamReader isw = new InputStreamReader(in);
                    int data = isw.read();
                    Log.d(TAG, "" + data + "");
                    while (data != -1) {
                        char current = (char) data;
                        data = isw.read();
                        Log.d(TAG, "" + current + "");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        urlConnection.disconnect();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
                return deviceUrl;
            }

            @Override
            protected void onPostExecute(String id) {
                //
            }
        }.execute(null, null, null);
    }
}
