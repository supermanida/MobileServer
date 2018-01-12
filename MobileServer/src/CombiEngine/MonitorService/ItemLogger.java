package CombiEngine.MonitorService;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class ItemLogger extends Thread
{
	InputStreamReader ir;
	BufferedReader br;
	PrintWriter pw;
	Socket s;
	ResetStatus logger;
	
	public ItemLogger(InputStreamReader ir, BufferedReader br, PrintWriter pw, Socket s, ResetStatus logger)
	{
		this.ir = ir;
		this.br = br;
		this.pw = pw;
		this.s = s;
		this.logger = logger;
		
		start();
	}
	
	public void run()
	{
		try
		{
			while ( true )
			{
				pw.println(logger.getQueueStatus() + "\f");
				
				try
				{
					sleep(1000);
				}
				catch(Exception ee) {}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { ir.close(); ir = null; } catch(Exception ee) {}
			try { br.close(); br = null; } catch(Exception ee) {}
			try { pw.close(); pw = null; } catch(Exception ee) {}
			try { s.close(); s = null; } catch(Exception ee) {}
		}
	}
}
