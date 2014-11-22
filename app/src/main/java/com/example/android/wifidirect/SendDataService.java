package com.example.android.wifidirect;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Created by vincentsantos on 11/22/14.
 */
public class SendDataService extends IntentService {
    private static final int SOCKET_TIMEOUT = 5000;
    public static final String EXTRAS_ADDRESS = "go_host";
    public static final String EXTRAS_MESSAGE = "message";
    public static final int  PORT = 8003;

    public SendDataService(String name) {
        super(name);
    }
    public SendDataService() {
        super("SendDataService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();
        String host = intent.getExtras().getString(EXTRAS_ADDRESS);
        String message = intent.getExtras().getString(EXTRAS_MESSAGE);
        int port = this.PORT;
        Socket socket = new Socket();

        try {
            Log.d(HybridMANETDTN.TAG, "MAKING SOCKET CONNECTION");
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
            if(socket != null){
                if (socket.isConnected()) {
                    try {
                        socket.close();
                        ((HybridMANETDTN)context).disconnect();
                    } catch (IOException e) {
                        // Give up
                        e.printStackTrace();
                    }
                }
            }
        }

    }



}
