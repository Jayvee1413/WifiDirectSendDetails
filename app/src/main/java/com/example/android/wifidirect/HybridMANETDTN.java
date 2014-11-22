package com.example.android.wifidirect;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.wifi.WpsInfo;
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
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
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
    private boolean isSender = false;

    private boolean wifiConnectFlag = false;
    private WifiP2pInfo info;
    private static final int WIFIPORT = 8003;
    private static final int SOCKET_TIMEOUT = 5000;
    private GPSTracker gpsTracker;

    private String data_message;
    private int peer_counter = 0;


    // FOR BLUETOOTH
    // Local Bluetooth adapter
    private ArrayList bluetoothDevices;
    // Message types sent from the BluetoothChatService Handler
    public static final int BT_MESSAGE_STATE_CHANGE = 1;
    public static final int BT_MESSAGE_READ = 2;
    public static final int BT_MESSAGE_WRITE = 3;
    public static final int BT_MESSAGE_TOAST = 4;
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
                isSender = true;
                data_message = getDataOut();
                Log.d(TAG, "MESSAGE: " + data_message);
                discoverWiFiPeers();
                //doBluetoothDiscovery();
            }
        });

    }



    private String getDataOut(){
        TextView statusText = (TextView) this.findViewById(R.id.status_text);
        EditText mName = (EditText) this.findViewById(R.id.nameField);
        EditText mAddress = (EditText) this.findViewById(R.id.addressField);
        EditText mAge = (EditText) this.findViewById(R.id.ageField);
        EditText mMessage = (EditText) this.findViewById(R.id.messageField);
        String name = mName.getText().toString();
        String address = mAddress.getText().toString();
        String age = mAge.getText().toString();
        String message = mMessage.getText().toString();
        Double latitude = this.gpsTracker.getLatitude();
        Double longitude = this.gpsTracker.getLongitude();
        JSONObject data_object = new JSONObject();
        try {
            data_object.put("name", name);
            data_object.put("address", address);
            data_object.put("age", age);
            data_object.put("message", message);
            data_object.put("latitude", latitude);
            data_object.put("longitude", longitude);
            return data_object.toString();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";

    }

    /** register the BroadcastReceiver with the intent values to be matched */
    @Override
    public void onResume() {
        super.onResume();
        receiver = new WiFiDirectBroadcastReceiver(this.manager, this.channel, this);
        registerReceiver(receiver, intentFilter);

        // Register for broadcasts when a device is discovered
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReceiver, filter);

        // Register for broadcasts when discovery has finished
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReceiver, filter);

    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        unregisterReceiver(mReceiver);
    }

    private void turnOnGPS(){
        Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
        intent.putExtra("enabled", true);
        this.sendBroadcast(intent);
        this.gpsTracker = new GPSTracker(this);
        if(this.gpsTracker.canGetLocation()){
            this.gpsTracker.getLocation();
        }
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
        Log.d(this.TAG, "DISCOVERING PEERS");
        manager.discoverPeers(channel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(TAG,"SUUCESSFULLY DISCOVERED PEERS");
            }

            @Override
            public void onFailure(int reason) {

                Log.d(TAG,"FAILED DISCOVERED PEERS: " + reason);
            }
        });

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
        Log.d("PEERS AVAILABLE CONNECT FLAG: ", (this.wifiConnectFlag ? "YES" : "NO"));
        Log.d("PEERS AVAILABLE IS SENDER: ", (this.isSender ? "YES" : "NO"));
        peers.clear();
        peers.addAll(peerList.getDeviceList());
        if(isSender){
            Log.d(this.TAG, "IS SENDER");
            connectWifiPeers();
        }
    }

    private void connectWifiPeers(){

        peer_counter = peers.size();
        Log.d(this.TAG, "CONNECTING TO PEERS");
        if(!wifiConnectFlag) {
            for (WifiP2pDevice peer : peers) {
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = peer.deviceAddress;
                config.wps.setup = WpsInfo.PBC;
                config.groupOwnerIntent = 0;
                connect(config);
            }

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
        Log.d(this.TAG, "Trying to connect");

        manager.connect(channel, config, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                Log.d(HybridMANETDTN.TAG, "Connected");
                wifiConnectFlag = true;
                Log.d(HybridMANETDTN.TAG, "INFO IN CONNECT: " + (info != null ? "YES" : "NO"));

            }

            @Override
            public void onFailure(int reason) {
                Log.d(HybridMANETDTN.TAG, "Failed to Connect");
                wifiConnectFlag = false;
                peer_counter--;
                if(peer_counter == 0){
                    isSender = false;
                }
            }
        });
    }

    @Override
    public void disconnect() {
        Log.d(this.TAG, "INSIDE DISCONNECT");
        manager.removeGroup(channel, null);
        wifiConnectFlag = false;
        this.isSender = false;

    }

    /**
     * @param isWifiP2pEnabled the isWifiP2pEnabled to set
     */
    public void setIsWifiP2pEnabled(boolean isWifiP2pEnabled) {
        this.isWifiP2pEnabled = isWifiP2pEnabled;
    }

    @Override
    public void onConnectionInfoAvailable(WifiP2pInfo info) {
        this.info = info;
        Log.d(this.TAG, "CONNECTION INFO AVAILABLE");

        Log.d(this.TAG, "CONNECTION INFO WIFICONNECT FLAG: " + (this.wifiConnectFlag ? "TRUE" : "FALSE"));
        Log.d(this.TAG, "CONNECTION INFO IS SENDER FLAG: " + (this.isSender ? "TRUE" : "FALSE"));
        Log.d(this.TAG, "CONNECTION INFO INFO FLAG: " + (this.info != null ? "TRUE" : "FALSE"));

        if(this.wifiConnectFlag && this.isSender && this.info != null){
            sendWifiMessage(data_message);
        }

    }

    public void doBluetoothDiscovery() {
        bluetoothDevices = new ArrayList();

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
                bluetoothDevices.add(device.getAddress());
            }
            // When discovery is finished
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Log.d(HybridMANETDTN.TAG, "Finished bluetooth discovery");
                if (bluetoothDevices.size() == 0) {
                    Toast.makeText(HybridMANETDTN.this, "No devices found via bluetooth", Toast.LENGTH_LONG).show();
                    Log.d(HybridMANETDTN.TAG, "No BT devices found");
                } else {
                    java.util.HashSet hs = new java.util.HashSet();
                    hs.addAll(bluetoothDevices);
                    bluetoothDevices.clear();
                    bluetoothDevices.addAll(hs);
                    java.util.Iterator iterator = (java.util.Iterator) bluetoothDevices.iterator();
                    BluetoothSocket mmSocket = null;
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice("28:CC:01:20:52:5B");
                    mConnService.connect(device);

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

                            if (!BluetoothAdapter.getDefaultAdapter().getAddress().equals("28:CC:01:20:52:5B"))
                            {
                                Log.d(TAG,"Connected!!!! Sending...");
                                sendBTMessage("TEST");
                            }

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
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    break;
                case BT_MESSAGE_TOAST:
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                            Toast.LENGTH_SHORT).show();
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
            //mConnService.stop();
            //mConnService.start();
            Log.d(TAG, "Sent Message: "+message);
            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }

    private void sendWifiMessage(String message){
        Log.d(this.TAG, "INSIDE SEND WIFI MESSAGE");
        Log.d(this.TAG, "WIFICONNECTFLAG: " + (wifiConnectFlag ? "TRUE" : "FALSE"));
        Log.d(this.TAG, "IS SENDER: " + (isSender ? "TRUE" : "FALSE"));
        if(wifiConnectFlag && message.length() > 0) {
            String host = this.info.groupOwnerAddress.toString();
            int port = this.WIFIPORT;
            Intent intent = new Intent(this, SendDataService.class);
            intent.putExtra(SendDataService.EXTRAS_ADDRESS, host);
            intent.putExtra(SendDataService.EXTRAS_MESSAGE, message);

            this.startService(intent);


            /*
            try {
                Log.d(this.TAG, "MAKING SOCKET CONNECTION");
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                if (socket != null) {
                    OutputStream stream = socket.getOutputStream();
                    OutputStreamWriter output_writer = new OutputStreamWriter(stream);
                    Log.d(this.TAG, "SENDING MESSAGE: " + message);
                    output_writer.write(message);
                    output_writer.close();

                }
            } catch (Exception e) {
                Log.d(this.TAG, "FAILED TO CREATE SOCKET CONNECTION: ", e);
                socket = null;
            } finally {
                if(socket != null){
                    if (socket.isConnected()) {
                        try {
                            socket.close();
                            this.disconnect();
                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
            }
            */
        }
    }



}
