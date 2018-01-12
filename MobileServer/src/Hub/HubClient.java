package Hub;

import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.BufferedOutputStream;

public class HubClient extends Thread
{
	private HubProcessor hubProcessor = null;
	
	private String			ip;
	private int			port;
	private String			name;
	
	private Socket			soc = null;
	private InputStreamReader	ir  = null;
	private BufferedReader		br  = null;
	
	private BufferedOutputStream bo = null;
	
	private SocketRunner runner = null;
	private	StringBuffer rcvStr = null;
	
	//*********************************************************************************************
	//
	//*********************************************************************************************
	public HubClient(HubProcessor hubProcessor, String ip, int port, String name)
	{
		this.hubProcessor = hubProcessor;
		this.ip = ip;
		this.port = port;
		this.name = name;
		this.rcvStr = new StringBuffer();
		
		start();
	}
	
	@Override
	public void run()
	{
		while ( true )
		{
			try
			{
				soc = new Socket(ip, port);
				ir  = new InputStreamReader(soc.getInputStream());
				br  = new BufferedReader(ir);
				bo  = new BufferedOutputStream(soc.getOutputStream());
				
				String msg = "Hub" + name;
				bo.write(msg.getBytes());
				bo.flush();
				
				runner = new SocketRunner(bo);
				
				int rcvCount = 0;
				char[] buf = new char[4096];
				
				while (true)
				{
					rcvCount = br.read(buf, 0, 4095);
					
					if ( rcvCount < 0 ) break;
					
					try
					{
						String processStr = ParseString(new String(buf, 0, rcvCount));
						
						if ( processStr.length() > 0 )
						{
							//synchronized(hubProcessor.dataBean.hubClientDebugStr)
							//{
								//hubProcessor.dataBean.hubClientDebugStr.append(processStr);
							//}
							
							//hubProcessor.log("HubClient", processStr);
							hubProcessor.ProcessCommand(processStr);
						}
					}
					catch(Exception ee)
					{
						ee.printStackTrace();
						//hubProcessor.exception("HubClient", ee);
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
				//hubProcessor.exception("HubClient", e);
			}
			finally
			{
				try { br.close(); } catch(Exception ee) {}
				try { ir.close(); } catch(Exception ee) {}
				try { bo.close(); } catch(Exception ee) {}
				try { soc.close();} catch(Exception ee) {}
				
				try { this.runner.finish(); } catch(Exception ee) {}
				
				rcvStr.delete(0, rcvStr.length());
			}
			
			try
			{
				sleep(10000);
			}
			catch(Exception ee){}
		}
	}
	
	public void sendString(String nowStr)
	{
		if ( nowStr.length() > 1 && nowStr.charAt(nowStr.length() - 1) != '\f' )	//  20170412
		{
			nowStr += '\f';
		}
		
		try
		{
			bo.write(nowStr.getBytes(), 0, nowStr.getBytes().length);
			bo.flush();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private String ParseString(String rcvStr)
	{
		String retStr = null;
		
		this.rcvStr.append(rcvStr);
		
		int breakPoint = this.rcvStr.lastIndexOf("\f");
		
		if ( breakPoint <= 0 ) return "";
		
		retStr = this.rcvStr.substring(0, breakPoint + 1);
		this.rcvStr.delete(0, breakPoint + 1);
		
		return retStr;
	}
}
