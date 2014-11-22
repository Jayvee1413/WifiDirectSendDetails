package com.example.android.wifidirect;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class HybridMANETDTN extends Activity implements WifiP2pManager.PeerListListener, DeviceActionListener, WifiP2pManager.ConnectionInfoListener{

    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver receiver = null;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private final IntentFilter intentFilter = new IntentFilter();
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();

    private boolean isWifiP2pEnabled = false;
    public static final String TAG = "HYBRIDMANET";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.turnOnRadios();
        this.makeDiscoverable();
        this.turnOnGPS();
        this.discoverWiFiPeers();
        setContentView(R.layout.activity_hybrid_manetdtn);
        Intent i = new Intent(this, SendSavedData.class);

        this.findViewById(R.id.btn_send_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("TEST", "testing lang");
                discoverWiFiPeers();
            }
        });





        //this.startService(i);

    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(this.manager, this.channel, this);
        registerReceiver(receiver, intentFilter);

    }

    private void turnOnGPS(){
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        this.sendBroadcast(intent);
    }

    private void turnOnRadios(){
        this.wifiManager.setWifiEnabled(true);

        // add necessary intent values to be matched.

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);



        this.manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = manager.initialize(this, getMainLooper(), null);

        receiver = new WiFiDirectBroadcastReceiver(this.manager, this.channel, this);
        registerReceiver(receiver, intentFilter);
        this.bluetoothAdapter.enable();
    }

    private void discoverWiFiPeers(){
        manager.discoverPeers(channel, null);
    }
    private void makeDiscoverable() {
        if (bluetoothAdapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(discoverableIntent);
         }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_hybrid_manetdtn, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onPeersAvailable(WifiP2pDeviceList peerList) {
        Log.d("PEER LIST SIZE", Integer.toString(peerList.getDeviceList().size()));
        Log.d("PEERS", "UPDATING PEERS");
        peers.clear();
        peers.addAll(peerList.getDeviceList());

        for(WifiP2pDevice peer: peers){
            Log.d("WIFI PEERS", peer.deviceName);
        }
    }

    @Override
    public void showDetails(WifiP2pDevice device) {

    }

    @Override
    public void cancelDisconnect() {

    }

    @Override
    public void connect(WifiP2pConfig config) {

    }

    @Override
    public void disconnect() {

    }

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {

    }
}
