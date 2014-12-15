 package combined2;

 import android.text.format.Time;
 import android.util.Log;

 import org.jivesoftware.smack.Chat;
 import org.jivesoftware.smack.MessageListener;
 import org.jivesoftware.smack.XMPPException;
 import org.jivesoftware.smack.packet.Message;

 import java.io.IOException;

 /**
  * A listener for 3G messages received by the sender
  *
  */

 public class SenderDTN3GListener implements MessageListener{
     private SenderDTNService sender;
     private Integer currentPacket;
     private int threeGCount=0;

     public SenderDTN3GListener(SenderDTNService sender) {
         this.sender = sender;
     }

     /**
      * A waiting function
      *
      * @param n Time in seconds
      */

     public void waiting (int n){
         long t0, t1;
         t0 =  System.currentTimeMillis();
         Log.i("INSIDE WAITING", Integer.toString(n));
         do{
             t1 = System.currentTimeMillis();
         }
         while ((t1 - t0) < (n * 1000));
     }

     /**
      * Function that processes the messages received
      *
      * @param chat The chat it would listen to
      * @param message The message received
      */
     public void processMessage(Chat chat, Message message) {
         Log.e("XMPPSender:Receiving", "Received text [" + message.getBody() + "]");
         Time time = new Time();

         if (message.getBody().equals("%&proceed")) {
             time.setToNow();
             //sender.setInitial(time.toMillis(true));
             setCurrentPacket(sender.getTracker());
             for (int i = 0; i < 10; i ++) {
                 sendPackets(chat, time);
                 waiting(2);
             }

         }else if (message.getBody().equals("%&DONE")) {
             sender.setTracker(getCurrentPacket());
             Log.e("XMPPSender:Sending", "Sending file SUCCESSFUL");
             //setCurrentPacket(0);

             /*try {
                 sender.getWriter().write((t - sender.getT1()) + " : total time\n");
                 sender.getWriter().write(sender.getT2() - sender.getT1() + " : processing time\n");
                 sender.getWriter().write((t - sender.getInitial()) + " : sending time\n");
             } catch (IOException e1) {
                 e1.printStackTrace();
             }
             try {
                 sender.getWriter().close();
             } catch (IOException e) {
                 e.printStackTrace();
             }*/
             //sender.finish();

         }else if (message.getBody().equals("%&CONTINUE")) {
             sender.setTracker(getCurrentPacket());
             for (int i = 0; i < 10; i ++) {
                 sendPackets(chat, time);
                 waiting(2);
             }
         }

     }

     /**
      * Function that sends a message containing 40 packets
      *
      * @param chat The chat session where it would send the message
      * @param time The current time
      */
     private void sendPackets (Chat chat, Time time) {
         try {
             int counter = 0;
             String line;
             String packet = "";
             while (counter < 40) {

                 //if (getCurrentPacket() == sender.getPacketList().size()) {
                 if (getCurrentPacket() == sender.getPacketList().size()) {
                     time.setToNow();
                     //t = time.toMillis(true);
                     break;
                 }

                 line = sender.getPacketList().get(getCurrentPacket());
                 packet = packet + "%&" + getCurrentPacket() + " " + line;
                 //setCurrentPacket(getCurrentPacket() + 1);
                 setCurrentPacket(getCurrentPacket() + 1);
                 counter++;
             }

             if (!packet.equals("")) {
                 Message reply = new Message();
                 reply.setType(Message.Type.chat);
                 reply.setBody(packet);

                 chat.sendMessage(reply);


                 time.setToNow();


                 Log.e("XMPPSender:Sending", "Sending text [" + reply.getBody() + "] SUCCESS");
             }
         } catch (XMPPException e) {
             Log.e("XMPPSender:Sending", "Sending text packet FAILED");
         }
     }

     public Integer getCurrentPacket() {
         return currentPacket;
     }

     public void setCurrentPacket(Integer currentPacket) {
         this.currentPacket = currentPacket;
     }

 }
