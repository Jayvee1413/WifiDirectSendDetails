package combined2;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;
import android.widget.Toast;

public class SmsReceiver extends BroadcastReceiver {

	String str = "";

	public void onReceive(Context context, Intent intent) {

		// ---get the SMS message passed in---
		Bundle bundle = intent.getExtras();
		SmsMessage[] msgs = null;

		if (bundle != null) {
			// ---retrieve the SMS message received---
			Object[] pdus = (Object[]) bundle.get("pdus");
			msgs = new SmsMessage[pdus.length];
			// Toast.makeText(context, pdus.length, Toast.LENGTH_SHORT).show();
			for (int i = 0; i < msgs.length; i++) {
				msgs[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);
				str += "SMS from " + msgs[i].getOriginatingAddress();
				str += " :";
				str += msgs[i].getMessageBody().toString();
				str += "\n";
			}
			// ---display the new SMS message---
			// abort broadcast if message is special
			if ((msgs[0].getMessageBody().toString()).startsWith("%&")
					|| (msgs[0].getMessageBody().toString()).startsWith("&%")) {
				this.abortBroadcast();
			}
			
			//DONE RECEIVING
			
			if(msgs[0].getMessageBody().toString().equals("%& done")){
				Log.e("DONE NA PO", "DONE");
            	this.abortBroadcast();
            	Intent i = new Intent(context, SenderActivity.class);
            	i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK   );
            	i.putExtra("start?", "done receiving");
                context.startActivity(i);
                
            }

			// SENDER SIDE
			//SMS
			if (msgs[0].getMessageBody().toString().startsWith("%& start ")) {
				Log.e("START!", "START!");
				Intent i = new Intent(context, SenderActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				String sub = msgs[0].getMessageBody().substring(9);
				i.putExtra("start?", "start sending");
				i.putExtra("isOnline", sub);
				i.putExtra("phoneNum", msgs[0].getOriginatingAddress()
						.toString());
				context.startActivity(i);

			}
			if (msgs[0].getMessageBody().toString().startsWith("%& doneSMS")) {
				Intent i = new Intent(context, SenderActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra("start?", "done SMS");
				Toast.makeText(context, "File sent.", Toast.LENGTH_SHORT)
						.show();
				context.startActivity(i);

			}
			
			if (msgs[0].getMessageBody().toString().startsWith("%& resend")) {
				Intent i = new Intent(context, SenderActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra("start?", "sendAgain");
				i.putExtra("phoneNum", msgs[0].getOriginatingAddress()
						.toString());
				String sub = msgs[0].getMessageBody().substring(9);

				i.putExtra("resendPackets", sub);

				context.startActivity(i);
			}
			
			if (msgs[0].getMessageBody().toString().startsWith("%& receiverConnectivity")) {
				Log.e("SMS NA NARECEIVE", msgs[0].getMessageBody().toString());
				Intent i = new Intent(context, SenderActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra("start?", "receiverConnectivity");
				i.putExtra("phoneNum", msgs[0].getOriginatingAddress()
						.toString());
				//String sub = msgs[0].getMessageBody().substring(24,24);
				char sub = msgs[0].getMessageBody().charAt(24);
				String sub2 = msgs[0].getMessageBody().substring(26);
				i.putExtra("isOnline", sub);
				i.putExtra("tracker", sub2);
				context.startActivity(i);
			}
			//MMS
			if(msgs[0].getMessageBody().toString().startsWith("%& doneMMS")){
            	this.abortBroadcast();
            	Intent i = new Intent(context, SenderActivity.class);
            	i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK   );
            	i.putExtra("start?", "doneMMS");
                context.startActivity(i);
            }
            if(msgs[0].getMessageBody().toString().startsWith("%& mmsreceived")){
            	this.abortBroadcast();
            	Intent i = new Intent(context, SenderActivity.class);
            	i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK   );
            	i.putExtra("start?", "sendAnotherMms");
                context.startActivity(i);
            }

			// RECEIVER SIDE
			//MMS
			if(msgs[0].getMessageBody().toString().startsWith("%& sendViaMms ")){
            	this.abortBroadcast();
            	Intent i = new Intent(context, ReceiverActivity.class);
            	Log.i("RECEIVE VIA MMS", "RECEIVE VIA MMS");
                i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK   );
                i.putExtra("start?","startMmsReceive");
                i.putExtra("phoneNum",msgs[0].getOriginatingAddress().toString());
                String sub = msgs[0].getMessageBody().substring(14);
                i.putExtra("startIndex", Integer.parseInt(sub));
                context.startActivity(i);
            }
			//SMS
			if (msgs[0].getMessageBody().toString().startsWith("%& check10")) {
				Intent i = new Intent(context, ReceiverActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra("start?", "check10");
				i.putExtra("phoneNum", msgs[0].getOriginatingAddress()
						.toString());
				String sub = msgs[0].getMessageBody().substring(11);
				Log.i("inside check10", sub);
				i.putExtra("tracker", sub);

				context.startActivity(i);
			}
			if (msgs[0].getMessageBody().toString().contains("%& sendFile ")) {
				Log.e("SendFile", "SendFile");
				Intent i = new Intent(context, ReceiverActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra("start?", "getConfirm");
				i.putExtra("phoneNum", msgs[0].getOriginatingAddress()
						.toString());
				String sub = msgs[0].getMessageBody().substring(12);
				int j, size;
				String num = "";
				for (j = 0; sub.charAt(j) != ' '; j++) {
					num = num + sub.charAt(j);
				}
				Log.i("NUM", num);
				size = Integer.parseInt(num);
				sub = sub.substring(j + 1);
				String tokens[] = sub.split(" ");
				i.putExtra("fileType", tokens[0]);
				Log.e("SIZE", Integer.toString(size));
				i.putExtra("size", size);
				i.putExtra("isOnline", tokens[1]);
				context.startActivity(i);

			}
			if (msgs[0].getMessageBody().toString().startsWith("%& sent")) {
				Intent i = new Intent(context, ReceiverActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra("start?", "getMore");
				i.putExtra("phoneNum", msgs[0].getOriginatingAddress()
						.toString());
				String sub = msgs[0].getMessageBody().substring(7);
				int j, size;
				String num = "";
				for (j = 0; j < sub.length(); j++) {
					num = num + sub.charAt(j);
				}
				Log.i("NUM", num);
				size = Integer.parseInt(num);

				i.putExtra("sentPackets", size);

				context.startActivity(i);
			}

			if (msgs[0].getMessageBody().toString().startsWith("&%")) {
				Intent i = new Intent(context, ReceiverActivity.class);
				i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION
						| Intent.FLAG_ACTIVITY_NEW_TASK);
				i.putExtra("start?", "start receiving");
				Log.i("MESSAGE", msgs[0].getMessageBody());
				String sub = msgs[0].getMessageBody().substring(3);
				Log.i("START RECEIVING SUB", sub);
				String num = "";
				int j;
				for (j = 0; sub.charAt(j) != ' '; j++) {
					num = num + sub.charAt(j);
				}
				Log.i("START RECEIVING NUM", num);
				sub = sub.substring(j + 1, sub.length());
				int packetNum = Integer.parseInt(num);
				i.putExtra("packetNum", packetNum);
				i.putExtra("message", sub);
				Log.i("MESSAGE TO BE DECODED", sub);
				context.startActivity(i);

			}
			// Toast.makeText(context,str, Toast.LENGTH_SHORT).show();
			
			//FOR 3G
			if(msgs[0].getMessageBody().toString().startsWith("%& sendVia3G")){
            	this.abortBroadcast();
            	Intent i = new Intent(context, ReceiverActivity.class);
            	Log.i("RECEIVE VIA 3G", "RECEIVE VIA 3G");
                i.setFlags(Intent.FLAG_ACTIVITY_NO_USER_ACTION | Intent.FLAG_ACTIVITY_NEW_TASK   );
                i.putExtra("start?","start3GReceive");
                context.startActivity(i);
            }

		}
	}

}