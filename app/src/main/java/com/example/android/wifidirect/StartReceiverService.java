package com.example.android.wifidirect;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class StartReceiverService extends AsyncTask<Void, Void, Void> {

    private WifiAcceptThread wifiAcceptThread;
    Context context;
    private String file_name;

    public StartReceiverService(Context context){
        Log.d(HybridMANETDTN.TAG, "START RECEIVER CONSTRUCTOR");
        this.context = context;
    }

    @Override
    protected Void doInBackground(Void... params) {
        Log.d(HybridMANETDTN.TAG, "DOING IN BG");
        wifiAcceptThread = new WifiAcceptThread();
        wifiAcceptThread.run();
        Log.d(HybridMANETDTN.TAG, "DONE IN BG");
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
            try {
                if(serverSocket == null)
                    serverSocket = new ServerSocket(8003);
            } catch (IOException e) {
                e.printStackTrace();
            }
            while(!isCancelled) {
                try {
                    if((serverSocket != null && !serverSocket.isClosed())) {
                        Log.d(HybridMANETDTN.TAG, "READY TO ACCEPT CONNECTION");
                        socket = serverSocket.accept();
                        Log.d(HybridMANETDTN.TAG, "GOT MESSAGE FROM SENDER");

                    }
                } catch (IOException e) {
                    Log.d(HybridMANETDTN.TAG, "aaaaa");
                    e.printStackTrace();
                }

                if(socket != null){
                    try {
                    processMessage(socket.getInputStream(), socket.getOutputStream());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }
        }

        private String getMessage(InputStream inputStream, OutputStream outputStream){

            boolean stop = false;
            String fullMessage = "";
            int counter = 0;
            while(!stop) {
                byte[] buffer = new byte[1024 * 512];
                int bytes;

                try {
                    bytes = inputStream.read(buffer);
                    String receivedMessage = new String(buffer, 0, bytes);
                    if(receivedMessage.startsWith("<FILENAME>")){
                        file_name = receivedMessage.replace("<FILENAME>", "");
                        file_name = file_name.replace("</FILENAME>", "");
                    } else {
                        Log.d(HybridMANETDTN.TAG, "GOT MESSAGE: " + receivedMessage.substring(receivedMessage.length() - 6));
                        Log.d(HybridMANETDTN.TAG, "FULL MESSAGE LENGTH: " + fullMessage.length());
                        fullMessage += receivedMessage;
                        counter += receivedMessage.length();
                        Log.d(HybridMANETDTN.TAG, "COUNTER LENGTH: " + counter);
                    }


                    if (receivedMessage.substring(receivedMessage.length() - 6).equals("</END>")) {
                        Log.d(HybridMANETDTN.TAG, "GOT END, STOP!!!");
                        fullMessage = fullMessage.substring(0,fullMessage.length()-6);
                        Log.d(HybridMANETDTN.TAG, "FINAL FULL MESSAGE LENGTH: " + fullMessage.length());
                        stop = true;
                        outputStream.write(new String("<CLOSE>").getBytes());

                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            return fullMessage;
        }

        private void processMessage(InputStream inputStream, OutputStream outputStream){
            //TODO Save message to db
            Log.d(HybridMANETDTN.TAG, "PROCESSING MESSAGE");

            String result = getMessage(inputStream, outputStream);
            Log.d(HybridMANETDTN.TAG, "RESULT: " + result);
            JSONObject json_data = null;
            try {
                json_data = new JSONObject(result);
                PacketData packetData = new PacketData(json_data);
                PacketDataDAO packetDataDAO = new PacketDataDAO(context);
                packetDataDAO.addData(packetData);

                /*
                String number = json_data.getString("number");
                String name = json_data.getString("name");
                String age = json_data.getString("age");
                String address = json_data.getString("address");
                String message = json_data.getString("message");
                Double latitude = json_data.getDouble("latitude");
                Double longitude = json_data.getDouble("longitude");
                String image = json_data.getString("image");
                String status = "QUEUED";
                Log.i(HybridMANETDTN.TAG, "NUMBER: " + number);
                Log.i(HybridMANETDTN.TAG, "NAME: " + name);
                Log.i(HybridMANETDTN.TAG, "AGE: " + age);
                Log.i(HybridMANETDTN.TAG, "ADDRESS: " + address);
                Log.i(HybridMANETDTN.TAG, "MESSAGE: " + message);
                Log.i(HybridMANETDTN.TAG, "LATITUDE: " + message);
                Log.i(HybridMANETDTN.TAG, "LATITUDE: " + Double.toString(latitude));
                Log.i(HybridMANETDTN.TAG, "LONGITUDE: " + Double.toString(longitude));
                Log.i(HybridMANETDTN.TAG, "IMAGE: " + image);


                DataDAO data_dao = new DataDAO(context);
                Data data = new Data(number, name, age, address, message, latitude, longitude, image);
                data_dao.addData(data);

                String file_string = json_data.toString();
                Log.d(HybridMANETDTN.TAG, "SAVING TO FILE: " + file_name);
                HybridMANETDTN.saveData(file_string, number, file_name, context);
                */



            } catch (JSONException e) {
                Log.e(HybridMANETDTN.TAG, e.getMessage());
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
