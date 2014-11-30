package combined2;
import com.example.android.wifidirect.R;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import android.app.Activity;
import android.app.PendingIntent;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class SenderActivity extends Activity {
    private String phoneNum;
    private ArrayList<String> packetList = new ArrayList<String>();
    private BroadcastReceiver threeGMonitorBroadcastReceiver;
    SenderActivity senderact;
    //private int currentChannel; // 0 - SMS 1- MMS 2 - 3G
    private Boolean is3g = false; // true if sending via three g, false if via mms
    private int packetCount;
    private int headtracker = 0, tailtracker = 0; // current packet number
    private Boolean done = false; // to check for end of file sharing
    private Boolean check10Received; // for SMS Protocol
    private int send10Resends; // for SMS Protocol, number of resends per 10
    // packets
    private Boolean mmsReceived = false;
    private Boolean receiverIsOnline = false;
    private Boolean started = false;
    private static final int SEND_MMS = 1003;
    private Button btnDisconnect;
    private TextView txtCurrentChannel;
    private TextView txtSMS;
    private TextView txtMMS;
    TextView txt3G;
    private TextView txtTotalPackets;
    ProgressDialog dialog;
    private int smsCount = 0;
    private int mmsCount = 0;
    private int pidParent = 0;
    private int packetstobesent;
    int threeGCount = 0;
    Chat nchat = null;
    Sender3GListener sender3GListener;
    private XMPPConnection connection; // for 3G connection
    private String username = "jvbsantos@gmail.com";
    private String password = "jeby1413";
    private String text = "0";
    IntentFilter gIntentFilter = new IntentFilter();
    CountDownTimer timer;
    // variables for log files
    private Time time = new Time();
    private long t1, t2, t3;
    private FileWriter logfw = null, logfw1 = null;
    private BufferedWriter logbw = null, logbw1 = null;
    private File senderLog = new File("/sdcard/senderLog.txt");
    private File senderSignal = new File("/sdcard/senderSignal.txt");
    // for signal strength
    TelephonyManager Tel;
    MyPhoneStateListener MyListener;
    Handler handler;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        senderact = this;
        // Debug.startMethodTracing("sender");
        setContentView(R.layout.senderactivity);
        timer = new CountDownTimer(1800000, 1000) {

            public void onFinish() {
                handler.post(new Runnable() {
                    public void run() {
                        senderact.finishActivity(0);

                    }
                });
            }

            public void onTick(long arg0) {
                Log.i("Time", "Time left: " + arg0 / 1000);
            }
        };

        btnDisconnect = (Button) this.findViewById(R.id.btnConnect);
        txtCurrentChannel = (TextView) this
                .findViewById(R.id.txtCurrentChannel);
        txtSMS = (TextView) this.findViewById(R.id.txtSMS);
        txtMMS = (TextView) this.findViewById(R.id.txtMMS);
        txt3G = (TextView) this.findViewById(R.id.txt3G);
        txtTotalPackets = (TextView) this.findViewById(R.id.txtTotalPackets);
        Log.e("SENDER ACT", "SENDER ACT");
        Intent intent = getIntent();
        MyListener = new MyPhoneStateListener();
        Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        t1 = intent.getLongExtra("temp1", 0);
        t2 = intent.getLongExtra("temp2", 0);
        pidParent = intent.getIntExtra("pid", 0);
        phoneNum = intent.getStringExtra("phoneNum");
        // packetCount = intent.getIntExtra("packetCount", 0);
        // packetList = intent.getStringArrayListExtra("arraylist");
        try {
            packetList = Base64FileEncoder.encodeFile(
                    intent.getStringExtra("tempAddress"),
                    intent.getStringExtra("tempAddress2"));
            Log.i("Base 64", "After Base 64");

            packetCount = packetList.size();
            tailtracker = packetCount-1;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.e("ON CREATE PACKETLIST", Integer.toString(packetList.size()));
        handler = new Handler();
        try {
            logfw = new FileWriter(senderLog);
            logbw = new BufferedWriter(logfw);
            logfw1 = new FileWriter(senderSignal);
            logbw1 = new BufferedWriter(logfw1);
            time.setToNow();
            t1 = time.toMillis(true);
            logbw.write(time.toString() + "SENDER START\n");
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        Log.d("app", "3g BROADCAST RECEIVER");
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
                            handler.post(new Runnable() {

                                public void run() {
                                    txtCurrentChannel.setText("3G");
                                }
                            });
                            is3g = true;
                            sendBy3G("yetanotherslave@gmail.com", headtracker);

                        } else {
                            Log.e("BRECEIVER", "I AM AT BRECEIVER");
                            handler.post(new Runnable() {

                                public void run() {
                                    txtCurrentChannel.setText("MMS");
                                }
                            });
                            is3g = false;
                            Log.e("BRECEIVER MMS", "I AM AT BRECEIVER");
                            sendViaMms(headtracker);

                        }

                        // send sms na connected
                    }
                }
                if (intent.getExtras().getBoolean(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY,
                        Boolean.FALSE)) {
                    Log.e("app", "There's no network connectivity");

                    Log.e("BRECEIVER OFFLINE", "I AM AT BRECEIVER");
                    handler.post(new Runnable() {

                        public void run() {
                            txtCurrentChannel.setText("MMS");
                        }
                    });
                    is3g = false;
                    Log.e("BRECEIVER DISCONNECTED MMS", "I AM AT BRECEIVER");
                    sendViaMms(headtracker);
                    // send sms na di connected
                }

            }
        };
        IntentFilter gIntentFilter = new IntentFilter();
        gIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");

    }
    //THREADS
    class smsThread extends Thread {
        // This method is called when the thread runs
        public void run() {
            try {
                sendViaSms(phoneNum, headtracker);
                Log.e("smsThread","Inside smsThread");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

    }

    public void onNewIntent(Intent intent) {
        // SMS
        Log.e("NEW", "NEW INTENT");
        Log.e("AL SIZE", Integer.toString(packetList.size()));
        if ((intent.getStringExtra("start?").toString())
                .equals("start sending")) {
            Log.e("START SENDING", "START SENDING");
            // sms(intent.getStringExtra("phoneNum").toString(), 0);
            // DEPENDE SA KUNG ANONG CHANNEL
            Thread smsthread = new smsThread();
            smsthread.start();

            if (isOnline(getBaseContext())
                    && intent.getStringExtra("isOnline").equals("1")) {
                receiverIsOnline = true;
                handler.post(new Runnable() {

                    public void run() {
                        txtCurrentChannel.setText("3G");
                    }
                });
                is3g = true;
                //Thread threegthread = new threeGThread();
                //threegthread.start();
            } else {
                /*
				receiverIsOnline = false;
				handler.post(new Runnable() {

					public void run() {
						txtCurrentChannel.setText("MMS");
					}
				});
				is3g = false;
				Log.e("INITIAL MMSTHREAD", "I AM AT BRECEIVER");
				sendViaMms(headtracker);
				*/
            }
            started = true;
            registerReceiver(threeGMonitorBroadcastReceiver, gIntentFilter);

        }

        if ((intent.getStringExtra("start?").toString())
                .equals("done receiving")) {
            // Debug.stopMethodTracing();
            time.setToNow();
            t3 = time.toMillis(true);
            try {
                logbw.write("Receiving time:" + (t3 - t2) + "\n");
                logbw.write("Processing time:" + (t2 - t1) + "\n");
                logbw.write("Total time:" + (t3 - t1) + "\n");
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
            done = true;
            Toast.makeText(getBaseContext(), "Done Sending", Toast.LENGTH_SHORT);
            this.finish();
        }

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
                        sendSMS(phoneNum, "&% " + j + " " + packetList.get(j));
                        Log.i("RESENT", packetList.get(j));
                        send10Resends++;
                    }

                }

            }

            try {
                Log.i("send10", "Before send10");

                handler.post(new Runnable() {

                    public void run() {
                        txtCurrentChannel.setText("SMS");
                    }
                });

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
        // MMS
        /*
		if ((intent.getStringExtra("start?").toString())
				.equals("sendAnotherMms")) {
			
			Log.e("MMS", "SEND ANOTHER MMS");
			mmsReceived = true;
			try {
				if (headtracker < packetCount && !is3g && headtracker <= tailtracker) {
					handler.post(new Runnable() {

						public void run() {
							txtCurrentChannel.setText("MMS");
						}
					});

					send1mms(phoneNum);
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		*/
        // 3G
        if ((intent.getStringExtra("start?").toString())
                .equals("receiverConnectivity")) {
            Log.e("RECEIVER CONNECTIVITY","RECEIVER CONNECTIVITY");
            headtracker = Integer.parseInt(intent.getStringExtra("tracker")
                    .toString());
            Log.e("HEAD TRACKER", ""+headtracker);
            Log.e("IS ONLINE?", ""+intent.getCharExtra("isOnline", '0'));
            if (intent.getCharExtra("isOnline", '9')=='1') {
                receiverIsOnline = true;
                if (isOnline(this)) {

                    handler.post(new Runnable() {

                        public void run() {
                            txtCurrentChannel.setText("3G");
                        }
                    });
                    Log.e("SHIFT TO 3G","SHIFT TO 3G");

                    sendBy3G("yetanotherslave@gmail.com", headtracker);
                }


            } else {
                /*
				receiverIsOnline = false;
				handler.post(new Runnable() {

					public void run() {
						txtCurrentChannel.setText("MMS");
					}
				});
				Log.e("SHIFT TO MMS","SHIFT TO MMS");
				sendViaMms(headtracker);
				*/
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
        String submessage = new String();
        String headerBegin = new String();

        Log.i("send10", "I AM AT send10");
        timer.cancel();

        // dialog.show(SmsMessagingActivity.this, "Sending SMS", "Please Wait");
        for (int counter = 0; counter < 10 && tailtracker > headtracker ; counter++) {
            Log.i("send10", "inside send10 for loop");
            headerBegin = "&% " + tailtracker + " ";
            submessage = headerBegin + packetList.get(tailtracker);
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
        time.setToNow();
        logbw.write(time.toString() + "Sending via SMS INSIDE send10\n");

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
        this.smsCount++;
        handler.post(new Runnable() {

            public void run() {
                txtSMS.setText(Integer.toString(smsCount));
            }
        });

    }

    // ###################################################################################################
    // //
    // FOR MMS CHANNEL
    private void sendViaMms(int startIndex) {
        sendSMS(phoneNum, "%& sendViaMms" + startIndex); // EDIT, REMOVE SUB
        timer.cancel();
        Log.i("FINISHED", "DONE SENDING SMS");
        try {
            Log.i("SENDING MMS", "SENDING MMS");

            handler.post(new Runnable() {

                public void run() {
                    txtCurrentChannel.setText("MMS");
                }
            });

            send1mms(phoneNum);


        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    private void send1mms(String phoneNum) throws IOException {


        Log.i("CHLOE", "I AM AT PARSER NOW");
        try {
            // dialog.show(Mms2Activity.this, "Sending SMS", "Please Wait");
            // 1024b * 300kb = 307200/160 char = 1920 packets

            String msg = "";
            packetstobesent = 0;
            for (int i = 0; i < 100 && headtracker < packetCount && headtracker <= tailtracker; i++) {
                msg = msg + "&% " + headtracker + " " + packetList.get(headtracker)
                        + "\n";
                headtracker++;
                packetstobesent++;
                handler.post(new Runnable() {

                    public void run() {
                        txtTotalPackets.setText(Integer.toString(headtracker));
                    }
                });

                //Log.i("SUBMESSAGE", msg);
            }

            Log.i("parser", "before mms sending");
            mmsReceived = false;
            sendMMS(phoneNum, msg);
            timer.start();
            Thread thread = new mmsWaitThread();
            thread.start();
            waiting(20);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        time.setToNow();
        logbw.write(time.toString() + "Sending via MMS\n");
        mmsCount++;
        handler.post(new Runnable() {

            public void run() {
                txtMMS.setText(Integer.toString(mmsCount));
            }
        });

    }

    private void sendMMS(String phoneNumber, String message) throws IOException {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.putExtra("sms_body", message);
        intent.putExtra("address", phoneNumber);
        intent.putExtra("subject", "mms");
        intent.setType("image/*");
        intent.setClassName("com.android.mms",
                "com.android.mms.ui.ComposeMessageActivity");
        startActivityForResult(intent, SEND_MMS);

    }

    class mmsWaitThread extends Thread {
        // This method is called when the thread runs
        public void run() {
            long t0, t1;
            t0 = System.currentTimeMillis();
            do {
                t1 = System.currentTimeMillis();
            } while ((t1 - t0) < (300 * 1000) && mmsReceived == false
                    && done == false); // wait for 5 minutes
            if (mmsReceived || done == true) {
                // do nothing
            } else {
                Log.i("MMS FAIL", "MMS FAIL WAIT");
                headtracker = headtracker - packetstobesent;
                if(!is3g){
                    try {
                        send1mms(phoneNum);
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
            }

        }

    }

    // ###################################################################################################
    // //
    // FOR 3G CHANNEL

    public ArrayList<String> getPacketList() {
        return this.packetList;
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
        sender3GListener = new Sender3GListener(this);
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

        if (isOnline(this)) {
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

    public void disconnectWifi(View view) {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            wifi.disconnect();
            wifi.setWifiEnabled(false);
            btnDisconnect.setText("Reconnect");
        } else {
            wifi.setWifiEnabled(true);
            btnDisconnect.setText("Disconnect");
            wifi.reconnect();

        }

    }

    public void setText3G(String text2) {
        this.text = text2;
        handler.post(new Runnable() {

            public void run() {
                txt3G.setText(text);
            }
        });

    }

    public BufferedWriter getWriter() {
        return logbw;
    }
    public void finishAct(){
        finish();
    }
    public void finish() {
        Intent data = new Intent();

        setResult(RESULT_OK, data);

        super.finish();
    }

    public void onDestroy() {
        unregisterReceiver(threeGMonitorBroadcastReceiver);
        if (nchat != null) {
            nchat.removeMessageListener(sender3GListener);
        }

        logOut();
        try {
            logbw.write("\nSMS Count: " + smsCount);
            logbw.write("\nMMS Count: " + mmsCount);
            logbw.write("\n3G Count: " + threeGCount);
            logbw.close();
            logbw1.close();
            logfw.close();
            logfw1.close();

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        android.os.Process.killProcess(pidParent);
        super.onDestroy();
    }

    // FOR SIGNAL STRENGTH

    protected void onPause() {
        super.onPause();
        Tel.listen(MyListener, PhoneStateListener.LISTEN_NONE);
    }

	/* Called when the application resumes */

    protected void onResume() {
        super.onResume();
        Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    /* ���������� */
	/* Start the PhoneState listener */
	/* ���������� */
    private class MyPhoneStateListener extends PhoneStateListener {
		/*
		 * Get the Signal strength from the provider, each tiome there is an
		 * update
		 */

        public void onSignalStrengthsChanged(SignalStrength signalStrength) {
            super.onSignalStrengthsChanged(signalStrength);
            time.setToNow();
            if (logfw1 != null && logbw1 != null) {
                try {
                    logbw1.write("Signal Strength"
                            + time.toString()
                            + ": "
                            + String.valueOf(signalStrength
                            .getGsmSignalStrength()) + "\n");
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }
    };/* End of private Class */

    // FOR LOG

}
