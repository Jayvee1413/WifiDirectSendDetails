package com.example.android.wifidirect;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class StartReceiverService extends AsyncTask<Void, Void, Void> {

    private WifiAcceptThread wifiAcceptThread;
    Context context;

    public StartReceiverService(Context context){
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        wifiAcceptThread = new WifiAcceptThread();
        wifiAcceptThread.run();

        return null;
    }

    @Override
    protected void onCancelled(){
        super.onCancelled();
        if(wifiAcceptThread != null)
            wifiAcceptThread.cancel();
    }


    private class WifiAcceptThread extends Thread{

        private ServerSocket serverSocket;
        private String host;
        private int port = 8003;
        private boolean isCancelled = false;
        private InputStream inputStream;
        private Socket socket;

        public WifiAcceptThread(){
            super();
        }

        public void run(){
            while(!isCancelled) {
                try {
                    if(serverSocket == null || (serverSocket!= null && serverSocket.isClosed())) {
                        serverSocket = new ServerSocket(8003);
                        socket = serverSocket.accept();
                        Log.d(HybridMANETDTN.TAG, "GOT MESSAGE FROM SENDER");
                        processMessage(socket.getInputStream());
                    }
                } catch (IOException e) {
                    Log.d(HybridMANETDTN.TAG, "");
                    e.printStackTrace();
                }
            }
        }

        private String getMessage(InputStream inputStream){
            BufferedReader br = null;
            StringBuilder sb = new StringBuilder();

            String line;
            try {

                br = new BufferedReader(new InputStreamReader(inputStream));
                while ((line = br.readLine()) != null) {
                    Log.d(HybridMANETDTN.TAG, "LINE: " + line);
                    sb.append(line);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            try {
                serverSocket.close();
                this.isCancelled = true;
            } catch (IOException e) {
                e.printStackTrace();
            }

            return sb.toString();
        }

        private void processMessage(InputStream inputStream){
            //TODO Save message to db
            String result = getMessage(inputStream);

            JSONObject json_data = null;
            try {
                json_data = new JSONObject(result);
                String number = json_data.getString("number");
                String name = json_data.getString("name");
                String age = json_data.getString("age");
                String address = json_data.getString("address");
                String message = json_data.getString("message");
                Double latitude = json_data.getDouble("latitude");
                Double longitude = json_data.getDouble("longitude");
                String image = json_data.getString("image");
                String status = "QUEUED";
                Log.i(HybridMANETDTN.TAG, "NAME: " + name);
                Log.i(HybridMANETDTN.TAG, "AGE: " + age);
                Log.i(HybridMANETDTN.TAG, "ADDRESS: " + address);
                Log.i(HybridMANETDTN.TAG, "MESSAGE: " + message);


                DataDAO data_dao = new DataDAO(context);
                Data data = new Data(number, name, age, address, message, latitude, longitude, image);
                data_dao.addData(data);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            this.isCancelled = false;
            this.run();


        }
        public void cancel() {
            if(serverSocket != null && !serverSocket.isClosed()){
                try {
                    serverSocket.close();
                    this.isCancelled = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        }
    }

}
