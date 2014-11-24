package com.example.android.wifidirect;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by vincentsantos on 11/22/14.
 */
public class SendWifiDataService extends IntentService {
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String EXTRAS_ADDRESS = "go_host";
    public static final String EXTRAS_MESSAGE = "message";
    public static final String EXTRAS_MESSAGE_LIST = "message_list";
    public static final int EXTRAS_RUN_SERVER = 1;
    public static final int EXTRAS_CANCEL_SERVER = 0;
    public static final int RECEIVER_PORT = 8003;

    public SendWifiDataService(String name) {
        super(name);
    }
    public SendWifiDataService() {
        super("SendDataService");
    }
    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(HybridMANETDTN.TAG, "INSIDE SEND DATA SERVCIE");
        Context context = getApplicationContext();
        String host = intent.getExtras().getString(EXTRAS_ADDRESS);
        String message = intent.getExtras().getString(EXTRAS_MESSAGE);
        Messenger messenger = (Messenger)intent.getExtras().get("MESSENGER");
        ArrayList<String> message_list = (ArrayList<String>)intent.getExtras().get(EXTRAS_MESSAGE_LIST);
        Boolean send_data = intent.getExtras().getBoolean("send_data");
        int port = this.RECEIVER_PORT;
        Socket socket = new Socket();
        Log.d(HybridMANETDTN.TAG, "SEND_DATA: " + (send_data ? "TRUE" : "FALSE"));
        if(send_data) {


            try {
                Log.d(HybridMANETDTN.TAG, "MAKING SOCKET CONNECTION: " + host + ": " + port);
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                if (socket != null) {
                    OutputStream stream = socket.getOutputStream();
                    OutputStreamWriter output_writer = new OutputStreamWriter(stream);
                    for(String message_data: message_list) {
                        Log.d(HybridMANETDTN.TAG, "SENDING MESSAGE: " + message_data);
                        output_writer.write(message_data);
                    }
                    output_writer.close();

                }
            } catch (Exception e) {
                Log.d(HybridMANETDTN.TAG, "FAILED TO CREATE SOCKET CONNECTION: ", e);
                socket = null;
            } finally {
                if (socket != null) {
                    if (socket.isConnected()) {
                        try {
                            socket.close();

                        } catch (IOException e) {
                            // Give up
                            e.printStackTrace();
                        }
                    }
                }
                Message handler_message = Message.obtain();
                try {
                    messenger.send(handler_message);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                Intent broadcastIntent = new Intent();
                broadcastIntent.setAction(HybridMANETDTN.MySendWifiDataServiceReceiver.PROCESS_RESPONSE);
                broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
                sendBroadcast(broadcastIntent);
            }

        }

    }





}
