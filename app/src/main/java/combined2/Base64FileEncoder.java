package combined2;
//Sample program to encode a binary file into a Base64 text file.
//Author: Christian d'Heureuse (www.source-code.biz)

import android.util.Log;

import com.example.android.wifidirect.HybridMANETDTN;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

public class Base64FileEncoder {
	static ArrayList<String> packetlist = new ArrayList<String>();
	public static ArrayList<String> encodeFile (String inputFileName, String outputFileName) throws IOException {
		packetlist.clear();
		BufferedInputStream in = null;
		BufferedWriter out = null;
		try {
           Log.i(HybridMANETDTN.TAG, "INPUT FILE NAME: "+ inputFileName);
           Log.i(HybridMANETDTN.TAG, "OUTPUT FILE NAME: "+ outputFileName);
		   in = new BufferedInputStream(new FileInputStream(inputFileName));
		   out = new BufferedWriter(new FileWriter(outputFileName));
		   encodeStream (in, out);
		   out.flush();
        }finally {
		   if (in != null) in.close();
		   if (out != null) out.close(); 
		 }
		return packetlist;
	}

	private static void encodeStream (InputStream in, BufferedWriter out) throws IOException {
		int lineLength = 100;
		byte[] buf = new byte[lineLength/4*3];
		while (true) {
		   int len = in.read(buf);
		   if (len <= 0) break;
		   packetlist.add(String.valueOf(Base64Coder.encode(buf, 0, len)));
		   out.write (Base64Coder.encode(buf, 0, len));
		   out.newLine(); 
		}
	}

} // end class Base64FileEncoder
