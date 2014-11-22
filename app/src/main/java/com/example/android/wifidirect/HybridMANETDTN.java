package com.example.android.wifidirect;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.location.LocationManager;
import android.os.Handler;
import android.os.Message;
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

import java.io.IOException;
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


    // FOR BLUETOOTH
    // Local Bluetooth adapter
    private ArrayList bluetoothDevices;
    // Message types sent from the BluetoothChatService Handler
    public static final int BT_MESSAGE_STATE_CHANGE = 1;
    public static final int BT_MESSAGE_READ = 2;
    public static final int BT_MESSAGE_WRITE = 3;
    public static final int BT_MESSAGE_TOAST = 4;
    public static final int BT_NEXT_PEER = 5;
    // Key names received from the BluetoothChatService Handler
    public static final String TOAST = "toast";
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Member object for the connection services
    private BluetoothConnService mConnService = null;
    // ENDFOR BLUETOOTH


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
                doBluetoothDiscovery();
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
        if (!((LocationManager) this.getSystemService(LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
            intent.putExtra("enabled", true);
            this.sendBroadcast(intent);
        }
    }

    private void turnOnRadios(){
        if (!this.wifiManager.isWifiEnabled()) {
            this.wifiManager.setWifiEnabled(true);
        }
        // add necessary intent values to be matched.

        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);



        this.manager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        this.channel = manager.initialize(this, getMainLooper(), null);

        receiver = new WiFiDirectBroadcastReceiver(this.manager, this.channel, this);
        registerReceiver(receiver, intentFilter);

        if (!this.bluetoothAdapter.isEnabled()){
            this.bluetoothAdapter.enable();

        }
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
        // Initialize the BluetoothChatService to perform bluetooth connections
        mConnService = new BluetoothConnService(this, mHandler);

        // FOR BLUETOOTH
        if (mConnService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mConnService.getState() == BluetoothConnService.STATE_NONE) {
                // Start the Bluetooth chat services
                mConnService.start();
            }
        }
        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");

        bluetoothDevices = new ArrayList();

        // END FORBLUETOOTH
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

    public void doBluetoothDiscovery() {
        // If we're already discovering, stop it
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Request discover from BluetoothAdapter
        bluetoothAdapter.startDiscovery();

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                // Add the name and address to an array adapter to show in a ListView
                Log.d(HybridMANETDTN.TAG, "Discovered " + device.getName() + " " + device.getAddress());
                if (!bluetoothDevices.contains(device.getAddress())){
                    bluetoothDevices.add(device.getAddress());
                }
            }
            // When discovery is finished
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(HybridMANETDTN.TAG, "Finished bluetooth discovery");
                if (bluetoothDevices.size() == 0) {
                    Toast.makeText(HybridMANETDTN.this, "No devices found via bluetooth", Toast.LENGTH_LONG).show();
                    Log.d(HybridMANETDTN.TAG, "No BT devices found");
                } else {
                    sendToBTPeers();
                }
            }
        }
    };

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BT_MESSAGE_STATE_CHANGE:
                    Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    switch (msg.arg1) {
                        case BluetoothConnService.STATE_CONNECTED:
                            Log.d(TAG,"Connected!!!! Sending...");

                            sendBTMessage("TEST");


                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothConnService.STATE_CONNECTING:
                            //setStatus(R.string.title_connecting);
                            break;
                        case BluetoothConnService.STATE_LISTEN:
                        case BluetoothConnService.STATE_NONE:
                            //setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case BT_MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    //mConversationArrayAdapter.add("Me:  " + writeMessage);
                    break;
                case BT_MESSAGE_READ:
                    //byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    break;
                case BT_MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
                case BT_NEXT_PEER:
                    if(bluetoothDevices.size() > 0) {
                        bluetoothDevices.remove(0);
                    }
                    sendToBTPeers();
                    break;
            }
        }
    };

    private void sendBTMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mConnService.getState() != BluetoothConnService.STATE_CONNECTED) {
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mConnService.write(send);
            Log.d(TAG, "Sent Message: "+message);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    private void sendToBTPeers() {
        if (bluetoothDevices.size()>0){
            String peerAddress = (String) bluetoothDevices.get(0);
            BluetoothSocket mmSocket = null;
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(peerAddress);
            Log.d(TAG, "Connecting to: "+peerAddress);
            mConnService.connect(device);
        }


    }
}
