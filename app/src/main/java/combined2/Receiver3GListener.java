package combined2;

import android.text.format.Time;
import android.util.Log;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

import java.io.IOException;

/**
 * A listener for 3G messages received by the receiver
 *
 */

public class Receiver3GListener implements PacketListener{
	
	private ReceiverActivity receiver;
	private long messCtr;

	public Receiver3GListener (ReceiverActivity receiver) {
		this.receiver = receiver;
	}
	
	/**
	 * Function that gets all packets in a given message
	 * 
	 * @param body Message that contains the packets
	 * @param time The current time
	 */

	public void getPackets(String body, Time time) {
		int packetNum;
		String packetLine;
		
		String[] p = body.split("%&");
		for (int i = 1; i < p.length; i++) {
			String[] data = p[i].split(" ");
			
			packetNum = Integer.parseInt(data[0]);
			packetLine = data[1];
			receiver.al.put(packetNum, packetLine);
			
			time.setToNow();

			Log.i("XMPPReceiver:Receiving", "Received Packet number " + packetNum + " with value " + packetLine);
			Log.i("XMPPReceiver:Receiving", "Current Map size " + Integer.toString(receiver.al.size()));
		}
	}
	
	/**
	 * Function that processes every messages received
	 * 
	 * @param packet The message received
	 */

	public void processPacket(Packet packet) {
		Time time = new Time();

		Message message = (Message)packet;
		String from = StringUtils.parseBareAddress(message.getFrom());
		Log.e("XMPPReceiver:Receiving", "Received text [" + message.getBody() + "] from " + from);

		Message reply = new Message();
		reply.setTo(from);
		reply.setType(Message.Type.chat);
		reply.setThread(message.getThread());

		if (message.getBody().startsWith("%&start3G")) {

			setMessCtr(0);
			reply.setBody("%&proceed");
			Log.e("XMPPReceiver:Sending", "Sending text [" + reply.getBody() + "]");
			receiver.getConnection().sendPacket(reply);
			receiver.threeGCount++;
		
		}else if (message.getBody().startsWith("%&")) {
			int packetNum;
			String packetLine;
			
			String s = message.getBody().substring(2);
			
			String n = "";
			int i;
			for (i = 0; s.charAt(i) != ' '; i++) {
				n = n + s.charAt(i);
			}
			packetNum = Integer.parseInt(n);
			packetLine = s.substring(i+1);
			
			time.setToNow();
			try {
				receiver.getWriter().write(time.toString() + " : Message " + packetNum + " Received\n");
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			Log.i("XMPPReceiver:Receiving", "Received Packet number " + packetNum + " with value " + packetLine);
			//Log.i("XMPPReceiver:Receiving", "Goal Map size " + packetSize);
			
			//String s = message.getBody().substring(2);
			getPackets(message.getBody(),time);
			setMessCtr(getMessCtr() + 1);
			Log.e("AL SIZE", Integer.toString(receiver.al.size()));
			Log.e("SIZE", Integer.toString(receiver.size));
			if (receiver.al.size() >= receiver.size) {
					
				Log.i("XMPPReceiver:File", "Finished");
				reply.setBody("%&DONE");
				Log.e("XMPPReceiver:Sending", "Sending text [" + reply.getBody() + "]");
				//receiver.getConnection().sendPacket(reply);
				receiver.receiveFile();
				
			} else if (getMessCtr() == 10) {
				reply.setBody("%&CONTINUE");
				setMessCtr(0);
				Log.e("XMPPReceiver:Sending", "Sending text [" + reply.getBody() + "]");
				receiver.getConnection().sendPacket(reply);
			}
		}
	}
	
	public long getMessCtr() {
		return messCtr;
	}

	public void setMessCtr(long packetCtr) {
		this.messCtr = packetCtr;
	}
}
