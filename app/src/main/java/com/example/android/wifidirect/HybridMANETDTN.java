package com.example.android.wifidirect;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.net.wifi.WpsInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Messenger;
import android.util.Log;
import android.util.Base64;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.widget.ImageView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.io.InputStream;


import combined2.FileExplore;


public class HybridMANETDTN extends Activity implements WifiP2pManager.PeerListListener, DeviceActionListener, WifiP2pManager.ConnectionInfoListener{

    private WifiManager wifiManager;
    private BluetoothAdapter bluetoothAdapter;
    private BroadcastReceiver receiver = null;
    private WifiP2pManager manager;
    private WifiP2pManager.Channel channel;
    private final IntentFilter intentFilter = new IntentFilter();
    private List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    private StartReceiverService serverReceiverTask;
    private MySendWifiDataServiceReceiver mySendWifiDataServiceReceiver;

    private static final int ACTIVITY_SELECT_IMAGE = 1003;

    private boolean isWifiP2pEnabled = false;
    public static final String TAG = "HYBRIDMANET";

    public boolean isSender() {
        return isSender;
    }

    public void setSender(boolean isSender) {
        this.isSender = isSender;
    }

    private boolean isSender = false;
    public ArrayList<String> data_message_list;

    public void setWifiConnectFlag(boolean wifiConnectFlag) {
        this.wifiConnectFlag = wifiConnectFlag;
    }

    public boolean wifiConnectFlag = false;
    private WifiP2pInfo info;
    private static final int WIFIPORT = 8003;
    private static final int SOCKET_TIMEOUT = 5000;
    private GPSTracker gpsTracker;
    private String data_message;
    private String encoded_image;
    private boolean isBTSender = false;

    public int getPeer_counter() {
        return peer_counter;
    }

    public void setPeer_counter(int peer_counter) {
        this.peer_counter = peer_counter;

    }

    private int peer_counter = 0;


    // FOR BLUETOOTH
    // Local Bluetooth adapter
    private ArrayList bluetoothDevices;
    // Message types sent from the BluetoothChatService Handler
    public static final int BT_MESSAGE_STATE_CHANGE = 1;
    public static final int BT_MESSAGE_READ = 2;
    public static final int BT_MESSAGE_WRITE = 3;
    public static final int BT_MESSAGE_TOAST = 4;
    public static final int BT_NEXT_PEER = 5;
    //Intent
    public static final int REQUEST_ENABLE_BT = 1;

    // Key names received from the BluetoothChatService Handler
    public static final String TOAST = "toast";
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Member object for the connection services
    private BluetoothConnService mConnService = null;
    // ENDFOR BLUETOOTH


    private final Handler sendDataServiceHandler = new Handler(){

        @Override
        public void handleMessage(Message message){
            Log.d(TAG, "GOT MESSAGE FROM SERVICE");
            setPeer_counter(getPeer_counter() - 1);
            disconnect();
            if(getPeer_counter() < 1){
                setWifiConnectFlag(false);
                setSender(false);

            }
        }
    };

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

        ImageView thumb = (ImageView) findViewById(R.id.image_thumb);

        if (thumb.getDrawable() == null)
        {
            findViewById(R.id.image_thumb).setVisibility(View.GONE);
        }

        data_message_list = new ArrayList<String>();

                this.findViewById(R.id.btn_send_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSender = true;
                data_message = getDataOut();

                data_message_list.add(data_message);
                Log.d(TAG, "MESSAGE: " + data_message);
                discoverWiFiPeers();
                doBluetoothDiscovery();
            }
        });

        this.findViewById(R.id.btn_send_saved_data).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                isSender = true;
                DataDAO dataDAO = new DataDAO(getApplicationContext());
                List<Data> data_list = dataDAO.getAllData();
                for(Data data: data_list){
                    data_message = data.getData().toString();
                    data_message_list.add(data_message);
                }
                Log.d(TAG, "MESSAGE: " + data_message);
                discoverWiFiPeers();
                doBluetoothDiscovery();
            }
        });

    }



    private String getDataOut(){
        EditText mName = (EditText) this.findViewById(R.id.nameField);
        EditText mAddress = (EditText) this.findViewById(R.id.addressField);
        EditText mAge = (EditText) this.findViewById(R.id.ageField);
        EditText mMessage = (EditText) this.findViewById(R.id.messageField);
        String name = mName.getText().toString();
        String address = mAddress.getText().toString();
        String age = mAge.getText().toString();
        String message = mMessage.getText().toString();

        Double latitude;
        try {
            latitude = this.gpsTracker.getLatitude();
        } catch(Exception e){
            latitude = 0.0;
        }
        Double longitude;

        try {
            longitude = this.gpsTracker.getLongitude();
        } catch(Exception e){
            longitude = 0.0;
        }
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                startBTConnService();
            case ACTIVITY_SELECT_IMAGE:
                InputStream imageStream = null;

                if(resultCode == RESULT_OK){
                    Uri selectedImage = data.getData();
                    try{
                        imageStream = getContentResolver().openInputStream(selectedImage);
                    }
                    catch (IOException e){
                        Log.e(TAG,"IMAGE SELECT", e);
                    }
                    Bitmap yourSelectedImage = BitmapFactory.decodeStream(imageStream);

                    encoded_image = base64Encode(yourSelectedImage);
                    Log.d(TAG, "IMAGE ENCODING: "+encoded_image.length());
                    Log.d(TAG, "IMAGE ENCODING: "+encoded_image);


                    try{
                        ((ImageView)findViewById(R.id.image_thumb)).setImageBitmap(decodeUri(selectedImage));
                    }
                    catch (FileNotFoundException e){
                        Log.e(TAG, "Image not found",e);
                    }
                    findViewById(R.id.image_thumb).setVisibility(View.VISIBLE);
                }
                else{
                    findViewById(R.id.image_thumb).setVisibility(View.GONE);
                }
        }
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


        IntentFilter mySendWifiDataServiceReceiverfilter = new IntentFilter(mySendWifiDataServiceReceiver.PROCESS_RESPONSE);
        filter.addCategory(Intent.CATEGORY_DEFAULT);
        mySendWifiDataServiceReceiver = new MySendWifiDataServiceReceiver();
        registerReceiver(mySendWifiDataServiceReceiver, mySendWifiDataServiceReceiverfilter);

        this.serverReceiverTask = new StartReceiverService(getApplicationContext());
        serverReceiverTask.execute();


    }

    @Override
    public void onPause() {
        super.onPause();
        unregisterReceiver(receiver);
        unregisterReceiver(mReceiver);
        unregisterReceiver(mySendWifiDataServiceReceiver);
        Intent intent = new Intent(this, StartReceiverService.class);
        this.stopService(intent);
    }

    private void turnOnGPS(){
        if (!((LocationManager) this.getSystemService(LOCATION_SERVICE)).isProviderEnabled(LocationManager.GPS_PROVIDER)){
            Intent intent = new Intent("android.location.GPS_ENABLED_CHANGE");
            intent.putExtra("enabled", true);
            this.sendBroadcast(intent);
            this.gpsTracker = new GPSTracker(this);
            if(this.gpsTracker.canGetLocation()){
                this.gpsTracker.getLocation();
            }
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

        //receiver = new WiFiDirectBroadcastReceiver(this.manager, this.channel, this);
        //registerReceiver(receiver, intentFilter);

        if (!this.bluetoothAdapter.isEnabled()){
            this.bluetoothAdapter.enable();
        }
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
            startActivityForResult(discoverableIntent,REQUEST_ENABLE_BT);
        }
        else{
            startBTConnService();
        }

    }

    private void startBTConnService(){
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
        Log.d(this.TAG,  "PEER LIST SIZE: " + Integer.toString(peerList.getDeviceList().size()));
        Log.d(this.TAG, "UPDATING PEERS");
        Log.d(this.TAG,  "PEERS AVAILABLE CONNECT FLAG: "+ (this.wifiConnectFlag ? "YES" : "NO"));
        Log.d(this.TAG, "PEERS AVAILABLE IS SENDER: "+ (this.isSender ? "YES" : "NO"));
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
                if(peer.deviceName.contains("Lenovo") || peer.deviceName.contains("jeby")) {
                    WifiP2pConfig config = new WifiP2pConfig();
                    config.deviceAddress = peer.deviceAddress;
                    config.wps.setup = WpsInfo.PBC;
                    config.groupOwnerIntent = 0;
                    connect(config);
                }
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
                if (peer_counter == 0) {
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
        Log.d(this.TAG, "CONNECTION INFO IS GROUP OWNER: " + (info.isGroupOwner  ? "TRUE" : "FALSE"));

        if(!this.isSender && info.isGroupOwner){

        }

        if(!info.groupFormed){

        }

        if(this.wifiConnectFlag && this.isSender && this.info != null){

            sendWifiMessage(data_message, data_message_list);
        }

    }

    public void doBluetoothDiscovery() {
        // If we're already discovering, stop it
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }

        // Add paired devices to the device list
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        // If there are paired devices
        if (pairedDevices.size() > 0) {
            // Loop through paired devices
            for (BluetoothDevice device : pairedDevices) {
                // Add the name and address to an array adapter to show in a ListView
                Log.d(HybridMANETDTN.TAG,"Adding paired device " +device.getName() + " " + device.getAddress());
                bluetoothDevices.add(device.getAddress());
            }
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
                    if(!isBTSender){
                        sendToBTPeers();
                    }
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
                            Log.d(TAG,"isBTSender " + (isBTSender ? "YES" : "NO"));
                            if (isBTSender){
                                sendBTMessage(data_message);
                            }
                            //mConversationArrayAdapter.clear();
                            break;
                        case BluetoothConnService.STATE_CONNECTING:
                            isBTSender = true;
                            //setStatus(R.string.title_connecting);
                            break;
                        case BluetoothConnService.STATE_LISTEN:
                            isBTSender = false;
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
                    //String readMessage = new String(readBuf, 0, msg.arg1);
                    //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
                    String readMessage = new String(readBuf,0,msg.arg1);
                    Log.d(HybridMANETDTN.TAG,"Saving Message to DB "+readMessage);
                    if(readMessage.length() > 0 && !readMessage.equals("ACK")){
                        saveDataDAO(readMessage);
                    }
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
        Log.d(TAG, "ENTER "+Integer.toString(message.length()));
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
        isBTSender = true;
        if (bluetoothDevices.size()>0){
            String peerAddress = (String) bluetoothDevices.get(0);
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(peerAddress);
            Log.d(TAG, "Connecting to: "+peerAddress);
            mConnService.connect(device);
        }


    }

    private void saveDataDAO(String result){
        JSONObject json_data = null;
        try {
            json_data = new JSONObject(result);
            String name = json_data.getString("name");
            String age = json_data.getString("age");
            String address = json_data.getString("address");
            String message = json_data.getString("message");
            Double latitude = json_data.getDouble("latitude");
            Double longitude = json_data.getDouble("longitude");
            Log.i(HybridMANETDTN.TAG, "NAME: " + name);
            Log.i(HybridMANETDTN.TAG, "AGE: " + age);
            Log.i(HybridMANETDTN.TAG, "ADDRESS: " + address);
            Log.i(HybridMANETDTN.TAG, "MESSAGE: " + message);
            Log.i(HybridMANETDTN.TAG, "LAT: " + latitude.toString());
            Log.i(HybridMANETDTN.TAG, "LONG: " + longitude.toString());

            DataDAO data_dao = new DataDAO(getApplicationContext());
            Data data = new Data(name, age, address, message, latitude, longitude);
            data_dao.addData(data);

        } catch (JSONException e) {
            e.printStackTrace();
        }

    }

    private void sendWifiMessage(String message, ArrayList<String> message_list){
        Log.d(this.TAG, "INSIDE SEND WIFI MESSAGE");
        Log.d(this.TAG, "WIFICONNECTFLAG: " + (wifiConnectFlag ? "TRUE" : "FALSE"));
        Log.d(this.TAG, "IS SENDER: " + (isSender ? "TRUE" : "FALSE"));
        if(wifiConnectFlag && message.length() > 0) {
            String host = this.info.groupOwnerAddress.getHostAddress();
            int port = this.WIFIPORT;
            Intent intent = new Intent(this, SendWifiDataService.class);
            intent.putExtra(SendWifiDataService.EXTRAS_ADDRESS, host);
            intent.putExtra(SendWifiDataService.EXTRAS_MESSAGE, message);
            intent.putExtra(SendWifiDataService.EXTRAS_MESSAGE_LIST, message_list);
            intent.putExtra("MESSENGER", new Messenger(sendDataServiceHandler));
            intent.putExtra("send_data", true);
            Log.d(this.TAG, "STARTING SERVICE TO SEND MESSAGE");
            this.startService(intent);
        }
    }

    public class MySendWifiDataServiceReceiver extends BroadcastReceiver{

        public static final String PROCESS_RESPONSE = "com.example.android.wifidirect.MySendWifiDataServiceReceiver.PROCESS_RESPONSE";

        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(HybridMANETDTN.TAG, "DISCONNECTING");
            disconnect();
        }


    }

    public void exploreFiles(View view) {
        Log.i("INSIDE EXPLORE FILES", "CLICKED FILE");
        Intent i = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, ACTIVITY_SELECT_IMAGE);
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException {

        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 100;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

    }

    public static String base64Encode(Bitmap image)
    {
        Bitmap immagex=image;
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        immagex.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] b = baos.toByteArray();
        String imageEncoded = Base64.encodeToString(b,Base64.DEFAULT);

        Log.e("LOOK", imageEncoded);
        return imageEncoded;
    }
    public static Bitmap base64Decode(String input)
    {
        byte[] decodedByte = Base64.decode(input, 0);
        return BitmapFactory.decodeByteArray(decodedByte, 0, decodedByte.length);
    }

}
