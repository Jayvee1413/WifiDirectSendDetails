package combined2;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import com.example.android.wifidirect.FileData;
import com.example.android.wifidirect.FileDataDAO;
import com.example.android.wifidirect.HybridMANETDTN;
import com.example.android.wifidirect.PacketData;
import com.example.android.wifidirect.PacketDataDAO;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SenderDTNService extends IntentService {
    private String phoneNum = "+639178133292";

    private ArrayList<String> sender_packetList = new ArrayList<String>();
    private BroadcastReceiver threeGMonitorBroadcastReceiver;

    private Boolean is3g = false; // true if sending via three g, false if via mms
    private int packetCount;
    private int headtracker = 0, tailtracker = 0; // current packet number
    private Boolean done = true; // to check for end of file sharing
    private Boolean started = false;
    private Boolean receiverIsOnline = false;
    private Boolean check10Received; // for SMS Protocol
    private int send10Resends; // for SMS Protocol, number of resends per 10

    Chat nchat = null;
    SenderDTN3GListener sender3GListener;

    private XMPPConnection connection; // for 3G connection
    private String username = "jvbsantos@gmail.com";
    private String password = "jeby1413";
    private String text = "0";
    IntentFilter gIntentFilter = new IntentFilter();

    CountDownTimer timer;

    Handler handler;

    Context context;

    private ArrayList<String> packetList;

    private List<FileData> fileDataList;
    private Iterator<FileData> fileDataListIterator;
    FileData current_fileData;
    public int currentSignalStrength = 0;

    public SenderDTNService(){
        this("SenderDTNService");

    }
    public SenderDTNService(String name) {
        super(name);
        context = this;

        threeGMonitorBroadcastReceiver = new BroadcastReceiver() {

            public void onReceive(Context context, Intent intent) {
                Log.d("app", "Network connectivity change");
                if (intent.getExtras() != null) {
                    NetworkInfo ni = (NetworkInfo) intent.getExtras().get(
                            ConnectivityManager.EXTRA_NETWORK_INFO);
                    if (ni != null
                            && ni.getState() == NetworkInfo.State.CONNECTED
                            && started) {
                        Log.i("app", "Network " + ni.getTypeName()
                                + " connected");
                        if (receiverIsOnline) {
                            Log.e("BRECEIVER", "I AM AT BRECEIVER");
                            is3g = true;
                            sendBy3G("yetanotherslave@gmail.com", headtracker);

                        } else {
                            /*
                            Log.e("BRECEIVER", "I AM AT BRECEIVER");
                            is3g = false;
                            Log.e("BRECEIVER MMS", "I AM AT BRECEIVER");
                            sendViaMms(headtracker);
                            */
                            try {
                                sendViaSms(phoneNum, headtracker);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }

                        // send sms na connected
                    }
                }
                if (intent.getExtras().getBoolean(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY,
                        Boolean.FALSE)) {
                    Log.e(HybridMANETDTN.TAG, "There's no network connectivity");

                    Log.e("BRECEIVER OFFLINE", "I AM AT BRECEIVER");

                    is3g = false;
                    Log.e("BRECEIVER DISCONNECTED MMS", "I AM AT BRECEIVER");
                    try {
                        sendViaSms(phoneNum, headtracker);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    // send sms na di connected
                }

            }
        };
        IntentFilter gIntentFilter = new IntentFilter();
        gIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        context.unregisterReceiver(threeGMonitorBroadcastReceiver);
        context.registerReceiver(threeGMonitorBroadcastReceiver, gIntentFilter);

        //////////////////////////////////////////////////////////////////////////
        if ((intent.getStringExtra("start?").toString()).equals("start converting")) {

            FileDataDAO fileDataDAO = new FileDataDAO(context);
            fileDataList = fileDataDAO.getAllFiles();

            for (FileData fileData : fileDataList) {
                if (fileData.getStatus().equals("QUEUED")) {
                    // Start Sending Data To HQ
                    String fileName = fileData.getName();
                    try {
                        packetList = Base64FileEncoder.encodeFile(fileName, fileName + ".gz");
                        //SAVE PACKET TO DB
                        for (int i = 0; i < packetList.size(); i++) {
                            PacketDataDAO packetDataDAO = new PacketDataDAO(context);
                            packetDataDAO.addData(new PacketData(fileName, i, packetList.size(), packetList.get(i), "QUEUED", "QUEUED"));
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    fileData.setStatus("PROCESSED");
                    fileDataDAO.updateFile(fileData);
                }
            }

            fileDataDAO = new FileDataDAO(context);
            fileDataList = fileDataDAO.getAllFiles();
            fileDataListIterator = fileDataList.iterator();

            if(fileDataListIterator.hasNext()){
                while(true) {
                    current_fileData = fileDataListIterator.next();
                    if(current_fileData.getStatus().equals("PROCESSED")){
                        break;
                    }
                }
                PacketDataDAO packetDataDAO = new PacketDataDAO(context);
                List<PacketData> packetDataList = packetDataDAO.getAllPackets(current_fileData.getName());
                for(PacketData packetData: packetDataList){
                    sender_packetList = new ArrayList<String>();
                    sender_packetList.add(packetData.getPacket_no(), packetData.getPacket());
                }
            }

        }

        //////////////////////////////////////////////////////////////////////////
        if ((intent.getStringExtra("start?").toString())
                .equals("start sending")) {
            Log.e("START SENDING", "START SENDING");

            // DEPENDE SA KUNG ANONG CHANNEL
            Thread smsThread = new smsThread();
            smsThread.start();

            if (isOnline(context)
                    && intent.getStringExtra("isOnline").equals("1")) {
                receiverIsOnline = true;
                is3g = true;
            }
            started = true;
            registerReceiver(threeGMonitorBroadcastReceiver, gIntentFilter);

        }

        //////////////////////////////////////////////////////////////////////////
        if ((intent.getStringExtra("start?").toString())
                .equals("done receiving")) {

            done = true;
            FileDataDAO fileDataDAO = new FileDataDAO(context);
            current_fileData.setStatus("DONE");
            fileDataDAO.updateFile(current_fileData);
            Toast.makeText(getBaseContext(), "Done Sending", Toast.LENGTH_SHORT);
            unregisterReceiver(threeGMonitorBroadcastReceiver);
        }

        //////////////////////////////////////////////////////////////////////////
        if ((intent.getStringExtra("start?").toString()).equals("sendAgain")) {
            check10Received = true;
            Log.i("sendAgain", "inside sendAgain");
            String resend = intent.getStringExtra("resendPackets");
            if (resend.equals("none")) {
                // do nothing
            } else {
                String[] num;
                num = resend.split(" ");
                send10Resends = 0;
                for (int i = 0; i < num.length; i++) {
                    String expression = "[-+]?[0-9]*\\.?[0-9]+$";
                    CharSequence inputStr = num[i];
                    Pattern pattern = Pattern.compile(expression);
                    Matcher matcher = pattern.matcher(inputStr);

                    if (matcher.matches()) {
                        Log.e("-----NUM[i]-----", num[i]);
                        int j = Integer.parseInt(num[i]);
                        Log.e("RESEND LIST", num[i]);
                        sendSMS(phoneNum, "&% " + j + " " + sender_packetList.get(j));
                        Log.i("RESENT", sender_packetList.get(j));
                        send10Resends++;
                    }

                }

            }

            try {
                Log.i("send10", "Before send10");
                if (send10Resends < 5) {
                    send10(phoneNum);
                    // continue with sms since resends < 5
                } else {
                    // WAIT FOR 5 MINUTES THEN SEND 10
                    waiting(300);
                    send10(phoneNum);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //////////////////////////////////////////////////////////////////////////

        if ((intent.getStringExtra("start?").toString())
                .equals("receiverConnectivity")) {
            Log.e("RECEIVER CONNECTIVITY","RECEIVER CONNECTIVITY");
            headtracker = Integer.parseInt(intent.getStringExtra("tracker")
                    .toString());
            Log.e("HEAD TRACKER", ""+headtracker);
            Log.e("IS ONLINE?", ""+intent.getCharExtra("isOnline", '0'));
            if (intent.getCharExtra("isOnline", '9')=='1') {
                receiverIsOnline = true;
                if (isOnline(context)) {
                    Log.e("SHIFT TO 3G","SHIFT TO 3G");
                    sendBy3G("yetanotherslave@gmail.com", headtracker);
                }


            }
        }

    }

    //THREADS
    class smsThread extends Thread {
        // This method is called when the thread runs
        public void run() {
            try {
                sendViaSms(phoneNum, headtracker);
                Log.e("smsThread","Inside smsThread");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }



    // ###################################################################################################
    // //
    // FUNCTIONS FOR SMS CHANNEL

    public void sendViaSms(String phoneNo, int startIndex) throws IOException {
        sendSMS(phoneNum, "%& sendViaSms" + startIndex);
        send10(phoneNo);
    }

    private void send10(String phoneNo) throws IOException {
        String submessage = "";
        String headerBegin = "";

        Log.i("send10", "I AM AT send10");
        timer.cancel();

        // dialog.show(SmsMessagingActivity.this, "Sending SMS", "Please Wait");
        for (int counter = 0; counter < 10 && tailtracker > headtracker ; counter++) {
            Log.i("send10", "inside send10 for loop");
            headerBegin = "&% " + tailtracker + " ";
            submessage = headerBegin + sender_packetList.get(tailtracker);
            tailtracker--;

            Log.i("SUBMESSAGE", submessage);
            Log.i("PHONE NUMBER", phoneNo);
            sendSMS(phoneNo, submessage);
            waiting(3);

        }
        check10Received = false;
        sendSMS(phoneNo, "%& check10 " + (tailtracker + 10));
        Log.i("After send tailtracker", "tailtracker" + (tailtracker+10));
        timer.start();
        Thread thread = new smsWaitThread();
        thread.start();
        Log.d(HybridMANETDTN.TAG, "Sending via SMS INSIDE send10\n");

    }

    class smsWaitThread extends Thread {
        // This method is called when the thread runs
        public void run() {
            long t0, t1;
            t0 = System.currentTimeMillis();
            do {
                t1 = System.currentTimeMillis();
            } while ((t1 - t0) < (90 * 1000) && check10Received == false
                    && done == false); // wait for 90seconds
            if (check10Received || done == true) {
                //do nothing
            } else {
                Log.i("resend check10", "tailtracker" + (tailtracker+10));
                sendSMS(phoneNum, "%& check10 " + (tailtracker+10));
                //resend check10
            }

        }

    }

    // FUNCTIONS FOR ALL CHANNELS

    public static void waiting(int n) {
        long t0, t1;
        t0 = System.currentTimeMillis();
        Log.i("INSIDE WAITING", Integer.toString(n));
        do {
            t1 = System.currentTimeMillis();
        } while ((t1 - t0) < (n * 1000));
    }

    // ---sends an SMS message to another device---
    private void sendSMS(String phoneNumber, String message) {
        String SENT = "SMS_SENT";
        String DELIVERED = "SMS_DELIVERED";

        PendingIntent sentPI = PendingIntent.getBroadcast(this, 0, new Intent(
                SENT), 0);

        PendingIntent deliveredPI = PendingIntent.getBroadcast(this, 0,
                new Intent(DELIVERED), 0);

        SmsManager sms = SmsManager.getDefault();
        sms.sendTextMessage(phoneNumber, null, message, sentPI, deliveredPI);
        Log.e("SMS", "SMS Sent");

    }



    // ###################################################################################################
    // //
    // FOR 3G CHANNEL

    public ArrayList<String> getPacketList() {
        return this.sender_packetList;
    }

    public Integer getTracker() {
        return this.headtracker;
    }

    public void setTracker(int track) {
        this.headtracker = track;
    }

    public XMPPConnection getConnection() {
        return connection;
    }

    public void setConnection(XMPPConnection connection) {
        if (connection == null) {
            Log.e("Receiver:3GConnection", "Connection failure");
        } else {
            this.connection = connection;
        }
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void sendBy3G(String to, int startIndex) {
        timer.cancel();
        logIn();
        while (getConnection() == null) {
            // do nothing
        }
        sendSMS(phoneNum, "%& sendVia3G");
        waiting(30);
        Roster r = getConnection().getRoster();
        ChatManager chatManage = getConnection().getChatManager();
        sender3GListener = new SenderDTN3GListener(this);
        nchat = chatManage.createChat(to, sender3GListener);
        if (r.getPresence(to).isAvailable()) {
            Log.i("XMPPSender", "ONLINE: Available");
            Message message = new Message();
            message.setType(Message.Type.chat);

            // message.setBody("%&sendfile " + packetList.size() + " " +
            // getFileType());
            message.setBody("%&start3G");

            try {
                nchat.sendMessage(message);
                Log.e("XMPPSender:Sending",
                        "Sending text [" + message.getBody() + "] SUCCESS");
            } catch (XMPPException e) {
                Log.e("XMPPSender:Sending",
                        "Sending text [" + message.getBody() + "] FAILED");
            }
        } else {
            Log.i("XMPPSender", "OFFLINE si " + to);
        }

    }

    public void logIn() {
        Log.e("LOGIN", "LOGIN");

        if (isOnline(context)) {
            if (getUsername().equals("null") && getPassword().equals("null")) {
                LogInSettings lDialog;
                lDialog = new LogInSettings(this);
                Log.e("SHOW", "SHOWING DIALOG BOX");
                lDialog.show();
            } else {
                establishConnection(getUsername(), getPassword());
            }
        } else {
            Log.e("Receiver:3GConnection", "No internet connectivity available");
        }

    }

    public void logOut() {
        if (this.connection != null) {
            this.connection.disconnect();
        }
    }

    public void establishConnection(String user, String pwd) {
        SASLAuthentication.supportSASLMechanism("PLAIN");
        ConnectionConfiguration connConfig = new ConnectionConfiguration(
                "talk.google.com", 5222, "gmail.com");
        XMPPConnection conn = new XMPPConnection(connConfig);
        try {
            conn.connect();
            Log.i("XMPPClient",
                    "[SettingsDialog] Connected to " + conn.getHost());
            conn.login(user, pwd);
            Log.i("XMPPClient", "Logged in as " + conn.getUser());

            Presence presence = new Presence(Presence.Type.available, "", 24,
                    Presence.Mode.chat);
            conn.sendPacket(presence);

            setConnection(conn);
        } catch (XMPPException ex) {
            Log.e("XMPPClient",
                    "[SettingsDialog] Failed to connect to " + conn.getHost());
            setConnection(null);
        }
    }

    public boolean isOnline(Context ctx) {
        NetworkInfo info = (NetworkInfo) ((ConnectivityManager) ctx
                .getSystemService(Context.CONNECTIVITY_SERVICE))
                .getActiveNetworkInfo();

        if (info == null || !info.isConnected()) {
            return false;
        }
        // if (info.isRoaming()) {
        // here is the roaming option you can change it if you want to
        // disable internet while roaming, just return false
        // return false;
        // }
        return true;
    }

    public void disconnectWifi() {
        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            wifi.disconnect();
            wifi.setWifiEnabled(false);
        } else {
            wifi.setWifiEnabled(true);
            wifi.reconnect();

        }

    }

    public void onDestroy() {
        unregisterReceiver(threeGMonitorBroadcastReceiver);
        if (nchat != null) {
            nchat.removeMessageListener(sender3GListener);
        }

        logOut();

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }



}
