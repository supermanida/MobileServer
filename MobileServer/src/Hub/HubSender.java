package Hub;

import java.io.IOException;
import java.io.BufferedOutputStream;

public class HubSender extends Thread
{
	BufferedOutputStream os;
	StringBuffer str;
	
	boolean finish = false;
	
	public HubSender(BufferedOutputStream os, StringBuffer str)
	{
		this.os = os;
		this.str = str;
		
		this.start();
	}
	
	public void run()
	{
		while ( finish == false )
		{
			String nowStr = "";
			synchronized(str)
			{
				nowStr = str.toString();
				str.delete(0, str.length());
			}
			
			if ( nowStr.length() > 0 )
			{
				try
				{
					//System.out.println("SEND : " + nowStr);
					os.write(nowStr.getBytes());
					os.flush();
				}
				catch(IOException ie)
				{
					ie.printStackTrace();
				}
			}
			else
			{
				try
				{
					sleep(1);
				}
				catch(Exception e) {}
			}
		}
	}
	
	public void finish()
	{
		finish = true;
	}
}
