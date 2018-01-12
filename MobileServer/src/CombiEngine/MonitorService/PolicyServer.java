package CombiEngine.MonitorService;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.PrintWriter;

//*************************************************************************************************
//
// Listening
//
//*************************************************************************************************
public class PolicyServer extends Thread
{
	//*********************************************************************************************
	//
	//*********************************************************************************************
	private ServerSocket	server = null;
	private Socket		soc = null;

	private int		port;
	private boolean		m_bFinish = false;

	
	//*********************************************************************************************
	//*********************************************************************************************
	public PolicyServer(int port)
	{
		this.port = port;
		start();
	}
	
	//*********************************************************************************************
	// GATE Server Thread
	//*********************************************************************************************
	public void run()
	{
		try
		{
			server = new ServerSocket(port);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			System.out.println("Cannot open port, another process using this port");
			try { server.close(); } catch(Exception ee) {ee.printStackTrace();}
			server = null;
			return;
		}
		
		while ( true )
		{
			try
			{
				//*********************************************************************************
				soc = server.accept();
				soc.setSoTimeout(1000);
				
				//*********************************************************************************
				// 
				InputStreamReader	ir = new InputStreamReader(soc.getInputStream());
				BufferedReader		br = new BufferedReader(ir);
				PrintWriter		pw = new PrintWriter(soc.getOutputStream(), true);
				
				//*********************************************************************************
				// 
				String line = br.readLine();
				if ( line != null)
				{

					soc.setSoTimeout(0);
					String strPolicy = "";
					strPolicy = "<?xml version=\"1.0\"?>\n";
//					strPolicy+= "<!DOCTYPE cross-domain-policy SYSTEM \"http://www.adobe.com/xml/dtds/cross-domain-policy.dtd\">\n";
//					strPolicy+= "<site-control permitted-cross-domain-policies=\"master-only\"/>\n";
					strPolicy+= "<cross-domain-policy>\n";
					strPolicy+= "<allow-access-from domain=\"*\" to-ports=\"8090\" />\n";
					strPolicy+= "</cross-domain-policy>\0";
					pw.println(strPolicy);
					br.close();
					ir.close();
					server.close();
					server = new ServerSocket(port);
				}
				else
				{
					pw.println(line);
					br.close();
					ir.close();
				}
				
				soc = null;
			}
			catch(Exception e)
			{
				e.printStackTrace();
				if ( m_bFinish ) return;
				continue;
			}

		}
	}
	
	//*********************************************************************************************
	//
	//*********************************************************************************************
	public void Finish() throws Exception
	{
		m_bFinish = true;
		try { server.close(); } catch(Exception ee) {ee.printStackTrace();}
		
		if ( soc != null )
		{
			try { soc.close(); } catch(Exception ee) {ee.printStackTrace();}
		}
	}
}
