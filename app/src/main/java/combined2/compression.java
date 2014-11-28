package combined2;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Class responsible for the gzip compression algorithm
 */
public class compression {
	
	/**
	 * Function that compresses the given file via gzip algorithm
	 * @param filePath The file path where the file is located
	 * @param fileName The name of the file
	 */
	
	public static void compressGzip(String filePath, String fileName) {
		try {
			File file = new File(filePath+"/"+fileName);
			System.out.println("You are going to gzip the: " + file + " file ");
			FileOutputStream fos = new FileOutputStream(filePath+"/"+fileName + ".gz");
			System.out.println("Now the name of this gzip file is: " + file + ".gz");
			GZIPOutputStream gzos = new GZIPOutputStream(fos);
			System.out.println("Opening the input stream");
			FileInputStream fin = new FileInputStream(file);
			BufferedInputStream in = new BufferedInputStream(fin);
			System.out.print("Transferring file from " + fileName + " to " + file + ".gz");
			
			byte[] buffer = new byte[1024];
			int i;
			while ((i = in.read(buffer)) >= 0) {
				gzos.write(buffer, 0, i);
			}
			System.out.println(" file is in now gzip format");
			in.close();
			gzos.close();
			
			
		} catch (IOException e) {
			System.out.println("Exception is" + e);
		}
	}
	
	/**
	 * Function that decompresses the given gzip file
	 * 
	 * @param filename The file to be decompressed
	 */
	
	public static void decompressGzip(String filename){
		try{
			String inFilename = filename;
			System.out.println("Opening the gzip file.......................... :  opened");
	
	
			GZIPInputStream gzipInputStream = null;
			FileInputStream fileInputStream= new FileInputStream(inFilename);
			gzipInputStream = new GZIPInputStream(fileInputStream);
			System.out.println("Opening the output file............. : opened");
			String outFilename = inFilename.substring(0, inFilename.length()-3);
			OutputStream out = new FileOutputStream(outFilename);
			System.out.println("Transferring bytes from the compressed file to the output file........: Transfer successful");
			byte[] buf = new byte[1024];  //size can be changed according to programmer's need.
			int len;
			
			while ((len = gzipInputStream.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
			System.out.println("The file and stream is ......closing.......... : closed");
			gzipInputStream.close();
			out.close();
		}catch(IOException e){
			System.out.println("Exception has been thrown" + e);
		}
		
	}
}
