package CombiEngine.MonitorService;

import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Calendar;
import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

public class WebClient extends Thread
{
	InputStreamReader ir;
	BufferedReader br;
	PrintWriter pw;
	Socket s;
	StringBuffer rcvStr;
	String webRoot;
	
	public WebClient(InputStreamReader ir, BufferedReader br, PrintWriter pw, Socket s, String rcvStr, String webRoot)
	{
		this.ir = ir;
		this.br = br;
		this.pw = pw;
		this.s = s;
		this.webRoot = webRoot;
		this.rcvStr = new StringBuffer();
		this.rcvStr.append(rcvStr + "\r\n");
		
		start();
	}
	
	public void run()
	{
		FileInputStream fi = null;
		BufferedInputStream bfi = null;
		BufferedOutputStream bo = null;
		BufferedInputStream bi = null;
		
		try
		{
			String line = null;
			int contentLength = 0;
			while ( ( line = br.readLine() ) != null )
			{
				rcvStr.append(line + "\r\n");
				
				if ( rcvStr.length() > 4 && rcvStr.substring(0,4).equals("POST") && line.indexOf("Content-Length:") >= 0 )
					contentLength = Integer.parseInt(line.substring(line.indexOf(" ") + 1).trim());
				
				if ( rcvStr.indexOf("\r\n\r\n") >= 0 )
					break;
			}
			/*기존소스
			if ( contentLength > 0 )
			{
				char rbuf[] = new char[contentLength];
				br.read(rbuf, 0, contentLength);
				paramStr = new String(rbuf);
			}
			String filePath = webRoot + rcvStr.substring(4, rcvStr.indexOf(" ", 4));
			*/
			String filePath = "";
			if ( contentLength > 0 )
			{
				char rbuf[] = new char[contentLength];
				br.read(rbuf, 0, contentLength);
				filePath = webRoot + rcvStr.substring(5, rcvStr.indexOf(" ", 5));
			}
			else
			{
				filePath = webRoot + rcvStr.substring(4, rcvStr.indexOf(" ", 4));
			}
			
			File tf = new File(filePath);
			
			if ( !tf.exists() )
			{
				rcvStr.delete(0, rcvStr.length());
				rcvStr.append("HTTP/1.0 404 Not Found\r\n");
				rcvStr.append("Content-type: text/html\r\n\r\n");
				rcvStr.append("<HTML><HEAD><TITLE>Not Found - @Messenger</TITLE></HEAD><BODY>@Messenger Reply : Not Found</BODY></HTML>");
				
				pw.println(rcvStr.toString());
				
				return;
			}
			
			pw.print(getHeader(tf));
			pw.flush();
			
			fi = new FileInputStream(tf);
			bfi = new BufferedInputStream(fi);
			
			byte buffer[] = new byte[4096];
			
			int size = 0;
			bo = new BufferedOutputStream(s.getOutputStream());
			bi = new BufferedInputStream(s.getInputStream());
			while ( ( size = bfi.read(buffer, 0, 4096) ) >= 0 )
			{
				bo.write(buffer, 0, size);
				bo.flush();
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
			
			try { fi.close(); fi = null; } catch(Exception ee) {}
			try { bfi.close(); bfi = null; } catch(Exception ee) {}
			try { bo.close(); bo = null; } catch(Exception ee) {}
			try { bi.close(); bi = null; } catch(Exception ee) {}
		}
	}
	
	private String getHeader(File f)
	{
		String str = "";
		str += "HTTP/1.1 200 OK\r\n";
		str += "Date: ";
		
		String fileName = f.getName();
		fileName = fileName.toLowerCase();
		String type;
		
		if ( fileName.length() > 4 && fileName.substring(fileName.length()-4).equals(".jpg") )
		{
			type = "image/jpeg";
		}
		else if ( fileName.length() > 4 && fileName.substring(fileName.length()-4).equals(".gif") )
		{
			type = "image/gif";
		}
		else if ( fileName.length() > 4 && fileName.substring(fileName.length()-4).equals(".png") )
		{
			type = "image/png";
		}
		else if ( fileName.length() > 4 && fileName.substring(fileName.length()-4).equals(".htm") )
		{
			type = "text/html";
		}
		else if ( fileName.length() > 5 && fileName.substring(fileName.length()-5).equals(".html") )
		{
			type = "text/html";
		}
		else if ( fileName.length() > 5 && fileName.substring(fileName.length()-4).equals(".xml") )
		{
			type = "text/xml";
		}
		else if ( fileName.length() > 4 && fileName.substring(fileName.length()-4).equals(".txt") )
		{
			type = "text/plain";
		}
		else
		{
			type = "application/octet-stream";
		}
		
		Calendar cal = Calendar.getInstance();
		if ( cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY ) str += "Mon, ";
		else if ( cal.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY ) str += "Tue, ";
		else if ( cal.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY ) str += "Wed, ";
		else if ( cal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY ) str += "Thu, ";
		else if ( cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY ) str += "Fri, ";
		else if ( cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY ) str += "Sat, ";
		else str += "Sun, ";
		
		str += cal.get(Calendar.DAY_OF_MONTH);
		str += " ";
		
		if ( cal.get(Calendar.MONTH) == Calendar.JANUARY ) str += "Jan ";
		else if ( cal.get(Calendar.MONTH) == Calendar.FEBRUARY ) str += "Feb ";
		else if ( cal.get(Calendar.MONTH) == Calendar.MARCH ) str += "Mar ";
		else if ( cal.get(Calendar.MONTH) == Calendar.APRIL ) str += "Apr ";
		else if ( cal.get(Calendar.MONTH) == Calendar.MAY ) str += "May ";
		else if ( cal.get(Calendar.MONTH) == Calendar.JUNE ) str += "Jun ";
		else if ( cal.get(Calendar.MONTH) == Calendar.JULY ) str += "Jul ";
		else if ( cal.get(Calendar.MONTH) == Calendar.AUGUST  ) str += "Aug ";
		else if ( cal.get(Calendar.MONTH) == Calendar.SEPTEMBER  ) str += "Sep ";
		else if ( cal.get(Calendar.MONTH) == Calendar.OCTOBER ) str += "Oct ";
		else if ( cal.get(Calendar.MONTH) == Calendar.NOVEMBER ) str += "Nov ";
		else str += "Dec ";
		
		str += cal.get(Calendar.YEAR);
		str += " ";
		
		str += cal.get(Calendar.HOUR_OF_DAY);
		str += ":";
		str += cal.get(Calendar.MINUTE);
		str += ":";
		str += cal.get(Calendar.SECOND);
		str += " GMT\r\n";
		
		str += "Server: AtMessengerServer/3.0 (Linux) Combi\r\n";
		str += "Accept-Ranges: bytes\r\n";
		str += "Content-Length: " + f.length() + "\r\n";
		str += "Connection: Keep-Alive\r\n";
		str += "Content-Type: " + type + "\r\n\r\n";
		
		return str;
	}
}
