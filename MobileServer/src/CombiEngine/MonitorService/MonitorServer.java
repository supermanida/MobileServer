package CombiEngine.MonitorService;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import CombiEngine.Selector.SelectService;

public class MonitorServer extends Thread
{
	ServerSocket ss = null;
	int port;
	String webRoot;
	ResetStatus logger;
	String configFile;
	
	public MonitorServer(int port, String webRoot, SelectService selectService, String partition, String configFile)
	{
		this.port = port;
		this.webRoot = webRoot;
		this.configFile = configFile;
		
		logger = new ResetStatus(selectService, partition);
		
		this.start();
	}
	
	public void run()
	{
		try
		{
			ss = new ServerSocket(port);
			
			while ( true )
			{
				Socket s = null;
				BufferedReader br = null;
				InputStreamReader ir = null;
				PrintWriter pw = null;
				
				try
				{
					s = ss.accept();
					s.setSoTimeout(1000);
					ir = new InputStreamReader(s.getInputStream());
					br = new BufferedReader(ir);
					pw = new PrintWriter(s.getOutputStream(), true);
					String firstRecv = br.readLine();
					
					if ( firstRecv.length() >= 3 && firstRecv.substring(0,3).equals("GET") )
					{
						new WebClient(ir, br, pw, s, firstRecv, webRoot);
					}
					else if ( firstRecv.length() >= 4 && firstRecv.substring(0,4).equals("POST") )
					{
						new WebClient(ir, br, pw, s, firstRecv, webRoot);
					}
					else if ( firstRecv.length() >= 3 && firstRecv.substring(0,3).equals("ITM") )
					{
						new ItemLogger(ir, br, pw, s, logger);
					}
					else if ( firstRecv.length() >= 3 && firstRecv.substring(0,3).equals("SLT") )
					{
						new SelectorLogger(ir, br, pw, s, logger);
					}
					else if ( firstRecv.length() >= 3 && firstRecv.substring(0,3).equals("PRC") )
					{
						new ProcessorLogger(ir, br, pw, s, logger);
					}
					else if ( firstRecv.length() >= 3 && firstRecv.substring(0,3).equals("CPU") )
					{
						new CpuLogger(ir, br, pw, s, logger);
					}
					else if ( firstRecv.length() >= 3 && firstRecv.substring(0,3).equals("MEM") )
					{
						new MemoryLogger(ir, br, pw, s, logger);
					}
					else if ( firstRecv.length() >= 3 && firstRecv.substring(0,3).equals("DSK") )
					{
						new StorageLogger(ir, br, pw, s, logger);
					}
					else if ( firstRecv != null && firstRecv.length() >= 9 && firstRecv.substring(0,9).equals("GetConfig") )
					{
						//******************************************************************************
						// ���ӵ� Ŭ���̾�Ʈ�� ���� "Get"�� ������ 
						// Config ����
						// SendConfig ȣ��
						SendConfig(br, pw);
						
						try { s.close(); } catch(Exception ee) {}
						try { pw.close(); } catch(Exception ee) {}
						try { br.close(); } catch(Exception ee) {ee.printStackTrace();}
						try { ir.close(); } catch(Exception ee) {ee.printStackTrace();}
					}
					else if ( firstRecv != null && firstRecv.length() >= 9 && firstRecv.substring(0,9).equals("SetConfig") )
					{
						//******************************************************************************
						// ���ӵ� Ŭ���̾�Ʈ�� ���� "Set"�� ������ 
						// Config ������Ʈ
						// RecvConfig ȣ��
						RecvConfig(br, pw);
						
						try { s.close(); } catch(Exception ee) {}
						try { pw.close(); } catch(Exception ee) {}
						try { br.close(); } catch(Exception ee) {ee.printStackTrace();}
						try { ir.close(); } catch(Exception ee) {ee.printStackTrace();}
					}
					else
					{
						try { ir.close(); } catch(Exception ee) {}
						try { br.close(); } catch(Exception ee) {}
						try { pw.close(); } catch(Exception ee) {}
						try { s.close(); } catch(Exception ee) {}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
					
					try { ir.close(); } catch(Exception ee) {}
					try { br.close(); } catch(Exception ee) {}
					try { pw.close(); } catch(Exception ee) {}
					try { s.close(); } catch(Exception ee) {}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	//*********************************************************************************************
	//
	//*********************************************************************************************
	public void SendConfig(BufferedReader br, PrintWriter pw) throws Exception
	{
		StringBuffer str = new StringBuffer();
		
		pw.println("ConfigStart");
		
		BufferedReader fr = null;
		try
		{
			fr = new BufferedReader(new FileReader(configFile));
			String line = null;
			while ( ( line = fr.readLine() ) != null )
			{
				if ( str.length() != 0 ) str.append("\n");
				str.append(line);
			}
			
			fr.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		pw.println(str.toString());
		
		pw.println("ConfigComplete");
	}
	
	//*********************************************************************************************
	//
	//*********************************************************************************************
	public void RecvConfig(BufferedReader br, PrintWriter pw) throws Exception
	{
		StringBuffer str = new StringBuffer();
		
		pw.println("ConfigReady");
		
		String line = null;
		while ( ( line = br.readLine() ) != null )
		{
			if ( line.equals("ConfigComplete") )
				break;
			
			if ( !line.equals("ConfigStart") )
			{
				if ( str.length() != 0 ) str.append("\n");
				str.append(line);
			}
		}
		
		PrintWriter fw = null;
		try
		{
			fw = new PrintWriter(new FileWriter(configFile));
			fw.print(str.toString());
			fw.flush();
			fw.close();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		SendConfig(br, pw);
	}
}
