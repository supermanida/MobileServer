package CombiEngine.Mobile;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;

import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.OutputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

public class AndroidAlertSender extends Thread implements HostnameVerifier
{
	private String authKey;
	private String deviceKey;
	private String msg;
	private long collapse_key;
	
	public AndroidAlertSender(String authKey, String deviceKey, String msg, long collapse_key)
	{
		this.authKey = authKey;
		this.deviceKey = deviceKey;
		this.msg = msg;
		this.collapse_key = collapse_key;
		
		this.start();
	}
	
	public boolean verify(String hostname, SSLSession session)
	{
		return true;
	}
	
	public void run()
	{
		HttpURLConnection conn = null;
		
		OutputStream os = null;
		
		InputStreamReader ir = null;
		BufferedReader br = null;
		
		try
		{
			StringBuffer sb = new StringBuffer();
			
			sb.append("registration_id=");
			sb.append(deviceKey);
			sb.append("&collapse_key=" + collapse_key);
			sb.append("&delay_while_idle=0");
			sb.append("&data.msg=" + URLEncoder.encode(msg, "utf-8"));
			
			byte[] postData = sb.toString().getBytes("UTF-8");
			
			URL url = new URL("https://android.apis.google.com/c2dm/send");
			
			HttpsURLConnection.setDefaultHostnameVerifier(this);
			conn = (HttpURLConnection)url.openConnection();
			conn.setDoOutput(true);
			conn.setUseCaches(false);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			conn.setRequestProperty("Content-Length", postData.length + "");
			conn.setRequestProperty("Authorization", "GoogleLogin auth=" + authKey);
			
			os = conn.getOutputStream();
			os.write(postData);
			os.close();
			
			ir = new InputStreamReader(conn.getInputStream());
			br = new BufferedReader(ir);
			
			String line = null;
			
			while ( ( line = br.readLine() ) != null );
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if ( conn != null ) { try { conn.disconnect(); conn = null; } catch(Exception e) {} }
			if ( os != null ) { try { os.close(); os = null; } catch(Exception e) {} }
			if ( ir != null ) { try { ir.close(); ir = null; } catch(Exception e) {} }
			if ( br != null ) { try { br.close(); br = null; } catch(Exception e) {} }
		}
	}
}