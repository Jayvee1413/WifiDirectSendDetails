package com.example.android.wifidirect;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Created by vincentsantos on 11/22/14.
 */
public class SendWifiDataService extends IntentService {
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String EXTRAS_ADDRESS = "go_host";
    public static final String EXTRAS_MESSAGE = "message";
    public static final int EXTRAS_RUN_SERVER = 1;
    public static final int EXTRAS_CANCEL_SERVER = 0;
    public static final int  PORT = 8003;
    private WifiAcceptThread wifiAcceptThread;

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
        Boolean send_data = intent.getExtras().getBoolean("send_data");
        int port = this.PORT;
        Socket socket = new Socket();
        if(send_data) {


            try {
                Log.d(HybridMANETDTN.TAG, "MAKING SOCKET CONNECTION: " + host + ": " + port);
                socket.bind(null);
                socket.connect((new InetSocketAddress(host, port)), SOCKET_TIMEOUT);
                if (socket != null) {
                    OutputStream stream = socket.getOutputStream();
                    OutputStreamWriter output_writer = new OutputStreamWriter(stream);
                    Log.d(HybridMANETDTN.TAG, "SENDING MESSAGE: " + message);
                    output_writer.write(message);
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
                            ((HybridMANETDTN) context).disconnect();
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
            }

        } else {

            switch(intent.getExtras().getInt("server_mode")){

                case EXTRAS_CANCEL_SERVER:
                    wifiAcceptThread.cancel();
                    wifiAcceptThread = null;
                    break;
                case EXTRAS_RUN_SERVER:
                    wifiAcceptThread = new WifiAcceptThread();
                    wifiAcceptThread.run();
                    break;

            }
        }

    }

    private class WifiAcceptThread extends Thread{

        private ServerSocket serverSocket;
        private String host;
        private int port = 8003;

        public WifiAcceptThread(){
            super();
        }

        public void run(){
            if(serverSocket == null || (serverSocket!= null && serverSocket.isClosed())){
                try {
                    serverSocket = new ServerSocket(this.port);
                    serverSocket.accept();
                    Log.d(HybridMANETDTN.TAG, "GOT MESSAGE");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        public void cancel() {
            if(serverSocket != null && !serverSocket.isClosed()){
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }



    }



}
