package combined2;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.Contacts;
import android.telephony.SmsManager;
import android.text.InputFilter;
import android.text.format.Time;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import com.example.android.wifidirect.R;

public class SenderInitial extends Activity {
	private SenderActivity Sender;
	private String phoneNo = new String();
	String tempAddress= "", tempAddress2= "";
	private Button btnSendSMS;
	private EditText txtPhoneNo;
	private EditText txtMessage;
	private EditText txtFileName;
	private File selectedFile;
	int packetCount; // total number of packets
	private Time time = new Time();
	private String sub;
	private ArrayList<String> packetList = new ArrayList<String>();
	private static final int CONTACT_PICKER_RESULT = 1001;
	private static final int FILE_EXPLORE_RESULT = 1002;
	private static final int SENDER_ACTIVITY_RESULT = 1010;
	private int pid;
	
	private long temp1, temp2;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		pid  = android.os.Process.myPid();
		//Debug.startMethodTracing("sender",32000000);
		//LOG FILES
		Intent intent = getIntent();
		intent.getStringExtra("start?");
		setContentView(R.layout.sender);
		btnSendSMS = (Button) findViewById(R.id.btnSendSMS);
		txtMessage = (EditText) findViewById(R.id.txtMessage);
		txtPhoneNo = (EditText) findViewById(R.id.phoneNumberText);
		txtFileName = (EditText) findViewById(R.id.fileNameText);

		InputFilter[] FilterArray = new InputFilter[1];
		FilterArray[0] = new InputFilter.LengthFilter(160);
		txtMessage.setFilters(FilterArray);

		btnSendSMS.setOnClickListener(new View.OnClickListener() {
			public void onClick(View v) {
				phoneNo = txtPhoneNo.getText().toString();
				String message = txtMessage.getText().toString();
				if (phoneNo.length() > 0 && message.length() > 0) {
					sendSMS(phoneNo, message);
					if(isOnline(getBaseContext())){
						sendSMS(phoneNo, "%& sendFile " + packetCount + " " + sub + " 1");
					}else{
						sendSMS(phoneNo, "%& sendFile " + packetCount + " " + sub + " 0");
					}
					
					Intent intent = new Intent(SenderInitial.this,
							SenderActivity.class);
					intent.putExtra("phoneNum", phoneNo);
					Log.e("SENDER INITIAL PACKETLIST", Integer.toString(packetList.size()));
					//intent.putStringArrayListExtra("arraylist", packetList);
					intent.putExtra("tempAddress", tempAddress);
					intent.putExtra("tempAddress2", tempAddress2);
					intent.putExtra("packetCount", packetCount);
					intent.putExtra("start?", "fromInitial");
					intent.putExtra("temp1", temp1);
					intent.putExtra("temp2", temp2);
					intent.putExtra("pid", pid);
					startActivityForResult(intent,SENDER_ACTIVITY_RESULT);
					
				} else
					Toast.makeText(getBaseContext(),
                            "Please enter both phone number and message.",
                            Toast.LENGTH_SHORT).show();

			}
		});

	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data){
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case CONTACT_PICKER_RESULT:
				Cursor cursor = null;
				String number = "";
				try {
					Uri result = data.getData();
					Log.v("Contact Picker",
                            "Got a contact result: " + result.toString());

					// get the contact id from the Uri
					String id = result.getLastPathSegment();

					cursor = getContentResolver().query(Phone.CONTENT_URI,
							null, Phone.CONTACT_ID + "=?", new String[] { id },
							null);

					int numberIdx = cursor.getColumnIndex(Phone.DATA);

					// let's just get the first contact
					if (cursor.moveToFirst()) {
						number = cursor.getString(numberIdx);
						Log.v("Contact Picker", "Got mobile number: " + number);
					} else {
						Log.w("Contact Picker", "No results");
					}
				} catch (Exception e) {
					Log.e("Contact Picker", "Failed to get contact number", e);
				} finally {
					if (cursor != null) {
						cursor.close();
					}
					EditText numberEntry = (EditText) findViewById(R.id.phoneNumberText);
					numberEntry.setText(number);
					if (number.length() == 0) {
						Toast.makeText(this,
                                "No mobile number found for contact.",
                                Toast.LENGTH_LONG).show();
					}
				}
				break;
			case FILE_EXPLORE_RESULT:
				
				time.setToNow();
				temp1 = time.toMillis(true);
				txtFileName.setText(data.getExtras().getString("fileName"));
				File file = new File(data.getExtras().getString("filePath")
						+ "/" + data.getExtras().getString("fileName"));
				selectedFile = file;
				int j;
				for (j = selectedFile.getName().length() - 1; selectedFile
						.getName().charAt(j) != '.'; j--)
					;

				sub = selectedFile.getName().substring(j + 1);

				txtFileName.setText(file.getName() + " "
						+ Long.toString((file.length())));
				
				
				compression.compressGzip(
						data.getExtras().getString("filePath"), data
								.getExtras().getString("fileName"));
				time.setToNow();
		
				Log.i("Base 64", "Before Base 64");
				
					Log.i("FILE", data.getExtras().getString("filePath") + "/"
                            + data.getExtras().getString("fileName") + ".gz");
					
					tempAddress= data.getExtras().getString("filePath") + "/" + data.getExtras().getString("fileName") + ".gz";
					tempAddress2= data.getExtras().getString("filePath") + "/"+ "encodedFile.txt"; 
				try {
					packetList = Base64FileEncoder.encodeFile(data.getExtras()
							.getString("filePath")
							+ "/"
							+ data.getExtras().getString("fileName") + ".gz",
							data.getExtras().getString("filePath") + "/"
									+ "encodedFile.txt");
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
					Log.i("Base 64", "After Base 64");
					

					packetCount = packetList.size();
				
				time.setToNow();
				temp2 = time.toMillis(true);
				
				break;
			case SENDER_ACTIVITY_RESULT:
				this.finish();
				break;
				
			}

		} else {
			Log.w("OnActivityResult", "Warning: activity result not ok");
		}
	}

	
	public void doLaunchContactPicker(View view) {
		Intent contactPickerIntent = new Intent(Intent.ACTION_PICK,
				Contacts.CONTENT_URI);
		startActivityForResult(contactPickerIntent, CONTACT_PICKER_RESULT);
	}

	public void exploreFiles(View view) {
		Log.i("INSIDE EXPLORE FILES", "CLICKED FILE");
		Intent fileExploreIntent = new Intent(SenderInitial.this,
				FileExplore.class);
		startActivityForResult(fileExploreIntent, FILE_EXPLORE_RESULT);
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
	}

	
	public boolean isOnline(Context ctx) {
		NetworkInfo info = (NetworkInfo) ((ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

	    if (info == null || !info.isConnected()) {
	        return false;
	    }
	    //if (info.isRoaming()) {
	        // here is the roaming option you can change it if you want to
	        // disable internet while roaming, just return false
	    //    return false;
	    //}
	    return true;
	}
	
}

