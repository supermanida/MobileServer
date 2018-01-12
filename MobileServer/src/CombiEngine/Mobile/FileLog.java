package CombiEngine.Mobile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.text.SimpleDateFormat;
import java.util.Calendar; 

public class FileLog{
	
	public static synchronized void writeRandomAccesslog(String sLog)
    {
		Calendar cal = Calendar.getInstance();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        SimpleDateFormat formatter2 = new SimpleDateFormat("yyyyMMdd");
        String sToday = formatter.format(cal.getTime()); //for log-time
        String sDate = formatter2.format(cal.getTime()); //for file-name
        sLog = "\r\n"+ "[Login: " + sToday + "] " + sLog;
        
		try 
		{
		   File dir = new File("../Data/LOG");
    	   if(!dir.exists()){
    		   dir.mkdir();
    	   }
		      String name = "../Data/LOG/" + sDate + ".Log";
		      RandomAccessFile raf = new RandomAccessFile(name, "rw");
		      raf.seek(raf.length());
		      raf.writeBytes(new String(sLog.getBytes("KSC5601"),"8859_1"));
		      raf.close();
	    }
	    catch (IOException e) 
	    {
	      System.out.println("Error opening file: " + e);
	    }
		finally
		{
		}
    }
}
