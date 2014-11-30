package combined2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Debug;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.PhoneStateListener;
import android.telephony.SignalStrength;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import com.example.android.wifidirect.R;

public class ReceiverActivity extends Activity {
    Boolean received = false;
    Boolean initialR = false;
    String phoneNo = new String();
    Button btnSendConfirmation;
    Button btnDisconnect;
    EditText txtPhoneNo;
    SmsReceiver rcvd;
    HashMap<Integer, String> al = new HashMap<Integer, String>();
    int size; // number of messages to be received
    String fileT;
    int messageSize = 10;
    long last, current;
    private XMPPConnection connection;
    public static final String MMSMON_RECEIVED_MMS = "MMStesting.intent.action.MMSMON_RECEIVED_MMS";
    int temporary = 0;
    int tempalsize;
    int alsize = 0;
    private Uri mmsInURI = Uri.parse("content://mms-sms");
    private BroadcastReceiver mmsMonitorBroadcastReceiver;
    private BroadcastReceiver threeGMonitorBroadcastReceiver;
    private int initial;
    private int end;
    private String fileType;
    private boolean started;
    // variables for log files
    private Time time = new Time();
    private long t1, t2, t3;
    private FileWriter logfw = null, logfw1 = null;
    private BufferedWriter logbw = null, logbw1 = null;
    private File receiverLog = new File("/sdcard/receiverLog.txt");
    private File receiverSignal = new File("/sdcard/receiverSignal.txt");

    private int smsCount = 0;
    int threeGCount = 0;
    private String username = "yetanotherslave@gmail.com";
    private String password = "kenankenankenan";
    IntentFilter mIntentFilter = new IntentFilter();
    IntentFilter gIntentFilter = new IntentFilter();
    // for signal strength
    TelephonyManager Tel;
    MyPhoneStateListener MyListener;

    /*
	ContentObserver mmsObserver = new ContentObserver(null) {
		@Override
		public void onChange(boolean selfChange) {

			Thread mmsNotify = new Thread() {
				@Override
				public void run() {
					Intent mIntent = new Intent(MMSMON_RECEIVED_MMS);
					sendBroadcast(mIntent);
					super.run();
				}
			};
			mmsNotify.start();
			super.onChange(selfChange);
			// try here

			Log.i("MMS Received", "MMS RECEIVED HAHA");
			try {
				Log.i("SEARCHING", "SEARCHING MMS AGAIN");
				// checkMMSMessages();
				// temporary=tempalsize;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// end try

		}
	};
	*/

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //Debug.startMethodTracing("receiver");
        getContentResolver().delete(Uri.parse("content://mms"), null, null);
        super.onCreate(savedInstanceState);
        getContentResolver().delete(Uri.parse("content://mms"), null, null);
        setContentView(R.layout.receiver);
        Intent intent = getIntent();
        MyListener = new MyPhoneStateListener();
        Tel = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
        size = intent.getIntExtra("size", 10);
        fileT = intent.getStringExtra("fileType");
        rcvd = new SmsReceiver();
        btnSendConfirmation = (Button) findViewById(R.id.btnSendConfirmation);
        txtPhoneNo = (EditText) findViewById(R.id.phoneNumberText);
        btnDisconnect = (Button) findViewById(R.id.btnDisconnect);
        btnSendConfirmation.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // get starting time
                try {
                    //logfw = new FileWriter(receiverLog);
                    //logbw = new BufferedWriter(logfw);
                    logfw1 = new FileWriter(receiverSignal);
                    logbw1 = new BufferedWriter(logfw1);
                    time.setToNow();
                    t1 = time.toMillis(true);

                    if(logbw1==null){

                        Log.e("START", "bw1 is null");
                    }
                } catch (IOException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                phoneNo = txtPhoneNo.getText().toString();
                if (phoneNo.length() > 0) {
                    if (isOnline(getBaseContext())) {
                        Log.e("ONLINE", "ONLINE AKO!");
                        sendSMS(phoneNo, "%& start 1");
                        Log.e("PHONE NUMBER: ", phoneNo);
                        Log.e("SMS SENT", "SMS SENT");
                    } else {
                        Log.e("OFFLINE", "OFFLINE AKO!");
                        sendSMS(phoneNo, "%& start 0");
                        Log.e("PHONE NUMBER: ", phoneNo);
                        Log.e("SMS SENT", "SMS SENT");
                    }
                    Toast.makeText(getBaseContext(),
                            "Please do not close this application.",
                            Toast.LENGTH_SHORT).show();
                    registerReceiver(threeGMonitorBroadcastReceiver,
                            gIntentFilter);
                    registerReceiver(mmsMonitorBroadcastReceiver, mIntentFilter);
                    btnSendConfirmation.setClickable(false);

                } else
                    Toast.makeText(getBaseContext(),
                            "Please enter phone number.", Toast.LENGTH_SHORT)
                            .show();
            }
        });

        threeGMonitorBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("app", "Network connectivity change");
                int tracker=0;
                while(al.containsKey(tracker)) {
                    tracker++;
                }

                if (intent.getExtras() != null) {
                    NetworkInfo ni = (NetworkInfo) intent.getExtras().get(
                            ConnectivityManager.EXTRA_NETWORK_INFO);
                    if (ni != null
                            && ni.getState() == NetworkInfo.State.CONNECTED
                            && !ni.getTypeName().equals("mobile_mms")) {
                        Log.i("app", "Network " + ni.getTypeName()
                                + " connected");

                        sendSMS(phoneNo, "%& receiverConnectivity 1 "+tracker);
                        logIn();
                        // send sms na connected
                    }
                }
                if (intent.getExtras().getBoolean(
                        ConnectivityManager.EXTRA_NO_CONNECTIVITY,
                        Boolean.FALSE)) {
                    Log.e("app", "There's no network connectivity");
                    sendSMS(phoneNo, "%& receiverConnectivity 0 "+tracker);
                    // send sms na di connected
                }

            }
        };
        // FOR MMS
        /*
		mmsMonitorBroadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {

				Log.i("MMS Received", "MMS RECEIVED HAHA");
				try {
					Log.i("SEARCHING", "SEARCHING MMS AGAIN");
					checkMMSMessages();
					temporary = tempalsize;
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		mIntentFilter.addAction(MMSMON_RECEIVED_MMS);

		gIntentFilter.addAction("android.net.conn.CONNECTIVITY_CHANGE");
		getApplicationContext().getContentResolver().registerContentObserver(
				mmsInURI, true, mmsObserver);
		getApplicationContext().getContentResolver().notifyChange(mmsInURI,
				mmsObserver);
        */
    }

    // FOR SMS
    public void onNewIntent(Intent intent) {
        Log.i("INTENT", intent.getStringExtra("start?").toString());
        if ((intent.getStringExtra("start?").toString()).equals("getConfirm")) {
            registerReceiver(threeGMonitorBroadcastReceiver, gIntentFilter);
            registerReceiver(mmsMonitorBroadcastReceiver, mIntentFilter);
            Log.i("GETCONFIRM", "GETCONFIRM");
            size = intent.getIntExtra("size", 10);
            Log.e("SIZE", Integer.toString(size));
            fileT = intent.getStringExtra("fileType");
            try {
                logfw = new FileWriter(receiverLog);
                logbw = new BufferedWriter(logfw);
                logbw.write(time.toString() + "RECEIVER START\n");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            // reply on button click
        }
        if ((intent.getStringExtra("start?").toString()).equals("check10") && !received) {
            Boolean okay = true;
            Log.i("check10", "check for missing packets");
            waiting(30);
            String resend = "";
            int currentp;
            currentp = Integer.parseInt(intent.getStringExtra("tracker"));
            Log.e("AL CURRENT SIZE: ", Integer.toString(al.size()));
            for(Map.Entry<Integer, String> entry : al.entrySet()){
                int key = entry.getKey();
                String value = entry.getValue();
                Log.e("AL KEY",  Integer.toString(key));
                Log.e("AL VALUE", value);
            }
            Log.e("CURRENT P: ", Integer.toString(currentp));
            resend = "%& resend ";
            // Check if packet 0 is received
            if(al.containsKey(0) == false && received == false){
                resend = resend + "0 ";
                okay = false;
            }
            for (int i = (currentp % 10); i >= 0 && currentp < size; i--) {
                if (al.containsKey(currentp - i - 1) == false
                        && received == false) {
                    resend = resend + (currentp - i - 1) + " ";
                    Log.i("if not containskey", "checking for missing packets");
                    Log.i("resend", resend);
                    okay = false;
                }

            }/*
			if (al.size() < currentp) {
				for (int k = 0; k < currentp - 1; k++) {
					if (!al.containsKey(k)) {
						resend = resend + k + " ";
						Log.i("if not containskey",
								"checking for missing packets");

						Log.i("resend", resend);
						okay = false;
					}
				}
			}*/
            if (okay == true) {
                sendSMS(phoneNo, "%& resend none");
            } else {
                sendSMS(phoneNo, resend);
            }

        }
        if ((intent.getStringExtra("start?").toString())
                .equals("start receiving")) {
            if (received) {
                // do nothing
            } else if(!received && al !=null) {

                int pn;
                pn = intent.getIntExtra("packetNum", 1000);

                try {
                    if(al !=null){
                        logbw1.write(time.toString() + " : Message " + pn
                                + " Received\n");
                    }

                } catch (IOException e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }

                al.put(intent.getIntExtra("packetNum", 1000), intent
                        .getStringExtra("message").toString());
                Toast.makeText(getBaseContext(), "Received Packet #" + pn,
                        Toast.LENGTH_SHORT).show();

                Log.i("AL SIZE", Integer.toString(al.size()));
                Log.i("SIZE", Integer.toString(size));
                if (al.size() == size) {
                    receiveFile();
                }

            }

        }
        // FOR MMS
        // Log.i("NEWINTENT", "NEW INTENT " + intent.getStringExtra("start?"));
        if ((intent.getStringExtra("start?")).equals("startMmsReceive")) {
            time = new Time();
            if (started == false) {
                Debug.startMethodTracing("mmsreceiver");
                started = true;
            }
            Log.i("RECEIVED SMS", "RECEIVED SMS");
            phoneNo = intent.getStringExtra("phoneNum");
            Log.i("Phone num", phoneNo);
            initial = intent.getIntExtra("initial", 0);
            Log.i("initial", Integer.toString(initial));
            end = intent.getIntExtra("end", 0);
            Log.i("end", Integer.toString(end));
            // size = end - initial;
            Log.i("size", "SIZE: " + Integer.toString(size));

            // fileType = intent.getStringExtra("filetype");
            Log.i("fileType", fileType);

        }
        // FOR 3G
        if ((intent.getStringExtra("start?")).equals("start3GReceive")) {
            logIn();
        }
    }

    // FOR MMS
    private void checkMMSMessages() throws IOException {

        tempalsize = alsize;
        alsize = al.size();
        String[] coloumns = null;
        String[] values = null;
        ArrayList<Integer> mid = new ArrayList<Integer>();

        Cursor curPart = this
                .getApplicationContext()
                .getContentResolver()
                .query(Uri.parse("content://mms/inbox"), null, null, null, null);

        curPart.moveToFirst();
        for (int i = 0; i < curPart.getCount(); i++) {

            // add to List the mid of the MMS that is currently in the
            // inbox

            mid.add(curPart.getInt(curPart.getColumnIndex("_id")));

            curPart.moveToNext();
        }

        curPart.moveToFirst();

        curPart = this.getApplicationContext().getContentResolver()
                .query(Uri.parse("content://mms/part"), null, null, null, null);

        if (curPart.moveToFirst()) {
            do {
                coloumns = curPart.getColumnNames();

                if (values == null)
                    values = new String[coloumns.length];

                // just get the TEXT part
                for (int i = 0; i < mid.size(); i++) {

                    String[] packets;
                    String[] packets2;

                    if (curPart.getInt(curPart.getColumnIndex("mid")) == mid
                            .get(i)) {
                        String text = curPart.getString(curPart
                                .getColumnIndex("text"));
                        if (text != null) {
                            if (text.startsWith("&%")) {
                                packets = text.split("&% ");
                                try {
                                    logbw1.write(time.toString()
                                            + "Received packets via MMS\n");
                                } catch (IOException e1) {
                                    // TODO Auto-generated catch block
                                    e1.printStackTrace();
                                }
                                // bw.write("MID: " + mid.get(i) + "\tTEXT:"
                                // + text + "\n");
                                for (int j = 0; j < packets.length; j++) {
                                    // bw.write("packet " + j + ": " +
                                    // packets[j]
                                    // + "\n");
                                    Log.i("PACKETS", packets[j]);
                                    if (packets[j] != null) {
                                        packets2 = packets[j].split(" ");
                                        if (!packets2[0].equals("")) {
                                            Log.i("packets2[0]", packets2[0]);
                                            Log.i("packets2[1]", packets2[1]);
                                            int pNum = Integer
                                                    .parseInt(packets2[0]);
                                            Log.i("pNum", "pNum: " + pNum);
                                            Log.i("packet", packets2[1]);
                                            if (!al.containsKey(pNum)) {
                                                al.put(pNum, packets2[1]);
                                            }

                                            Log.i("AL SIZE",
                                                    "AL SIZE: " + al.size());
                                            if (al.size() == size) {
                                                receiveFile();
                                            }
                                        }

                                    }

                                }
                            }
                        }

                    }

                }

            } while (curPart.moveToNext());
            alsize = al.size();

            if (tempalsize != alsize) {
                Log.i("SIZES", "ALSIZES: tempalsize: " + tempalsize
                        + " alsize: " + alsize);
                Log.i("MMS", "NARECEIVE KO NA SI MMS");
                if (received == false) {
                    sendSMS(phoneNo, "%& mmsreceived");
                }

                getContentResolver().delete(Uri.parse("content://mms"), null,
                        null);
            }
        }
        curPart.close();

    }

    public void receiveFile() {

        if (al.size() >= size) {
            time.setToNow();
            t2 = time.toMillis(true);
            try {
                FileWriter fw1 = new FileWriter(new File("/sdcard/decode.txt"));
                for (int i = 0; i < size; i++) {
                    fw1.write(al.get(i) + "\n");
                }
                fw1.close();

                Log.i("WRITING TO FILE", "FILEWRITER");

                Base64FileDecoder.decodeFile("/sdcard/decode.txt",
                        "/sdcard/file." + fileT + ".gz");
                File fl = new File("/sdcard/decode.txt");
                fl.delete();

                compression.decompressGzip("/sdcard/file." + fileT + ".gz");
                fl = new File("/sdcard/file." + fileT + ".gz");
                fl.delete();
                time.setToNow();
                t3 = time.toMillis(true);
                Log.i("DONE!!!", "DONE");

                logbw.write("Receiving time:" + (t2 - t1) + "\n");
                logbw.write("Processing time:" + (t3 - t2) + "\n");
                logbw.write("Total time:" + (t3 - t1) + "\n");

                Log.e("LOG","DONE LOGGING");


                sendSMS(phoneNo, "%& done");
                //Debug.stopMethodTracing();
                Log.e("DONE","DONE");
                this.finish();
                al = null;
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            received = true;
        }
    }

    // FOR 3G

    public XMPPConnection getConnection() {
        return connection;
    }

    public void setConnection(XMPPConnection connection) {
        if (connection == null) {
            Log.e("Receiver:3GConnection", "Connection failure");
        } else {
            this.connection = connection;
            PacketFilter filter = new MessageTypeFilter(Message.Type.chat);
            connection.addPacketListener(new Receiver3GListener(this), filter);
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

    public void logIn() {
        if (isOnline(this)) {
            if (getUsername() == null && getPassword() == null) {
                LogInSettings lDialog;
                lDialog = new LogInSettings(this);
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

    public BufferedWriter getWriter() {
        return logbw;
    }

    // COMMON FUNCTIONS
    public void disconnectWifi(View view) {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        if (wifi.isWifiEnabled()) {
            wifi.disconnect();
            wifi.setWifiEnabled(false);
            btnDisconnect.setText("Reconnect");
        } else {
            wifi.setWifiEnabled(true);
            wifi.reconnect();

            btnDisconnect.setText("Disconnect");
        }

    }

    private static final int CONTACT_PICKER_RESULT = 1001;

    public void doLaunchContactPicker(View view) {
        Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
                Contacts.CONTENT_URI);
        startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case CONTACT_PICKER_RESULT:
                    Cursor cursor = null;
                    String email = "";
                    try {
                        Uri result = data.getData();
                        Log.v("chloe", "Got a contact result: " + result.toString());

                        // get the contact id from the Uri
                        String id = result.getLastPathSegment();

                        // query for everything email --> contact
                        cursor = getContentResolver().query(Phone.CONTENT_URI,
                                null, Phone.CONTACT_ID + "=?", new String[] { id },
                                null);

                        int emailIdx = cursor.getColumnIndex(Phone.DATA);

                        // let's just get the first contact
                        if (cursor.moveToFirst()) {
                            email = cursor.getString(emailIdx);
                            Log.v("chloe", "Got mobile number: " + email);
                        } else {
                            Log.w("chloe", "No results");
                        }
                    } catch (Exception e) {
                        Log.e("chloe", "Failed to get contact number", e);
                    } finally {
                        if (cursor != null) {
                            cursor.close();
                        }
                        EditText emailEntry = (EditText) findViewById(R.id.phoneNumberText);
                        emailEntry.setText(email);
                        if (email.length() == 0) {
                            Toast.makeText(this,
                                    "No mobile number found for contact.",
                                    Toast.LENGTH_LONG).show();
                        }

                    }
                    break;

            }

        } else {
            Log.w("chloe", "Warning: activity result not ok");
        }
    }

    private void sendSMS(String phoneNumber, String message) {
        smsCount++;
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

    public static void waiting(int n) {
        long t0, t1;
        t0 = System.currentTimeMillis();
        Log.i("INSIDE WAITING", Integer.toString(n));
        do {
            t1 = System.currentTimeMillis();
        } while ((t1 - t0) < (n * 1000));
    }

    public void onDestroy() {
        unregisterReceiver(mmsMonitorBroadcastReceiver);
        unregisterReceiver(threeGMonitorBroadcastReceiver);

        try {
            logbw.write("\nSMS Count: "+smsCount);
            logbw.write("\n3G Count: "+threeGCount);
            logbw.close();
            logbw1.close();
            logfw.close();
            logfw1.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        logOut();
        super.onDestroy();

    }

    // FOR SIGNAL STRENGTH

    protected void onPause() {

        super.onPause();
        Tel.listen(MyListener, PhoneStateListener.LISTEN_NONE);
    }

    /* Called when the application resumes */
    @Override
    protected void onResume() {
        super.onResume();
        Tel.listen(MyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
    }

    /* Start the PhoneState listener */
    private class MyPhoneStateListener extends PhoneStateListener {
        /*
         * Get the Signal strength from the provider, each tiome there is an
         * update
         */
        @Override
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
}
