package CombiEngine.Mobile;

import javapns.back.PushNotificationManager;
import javapns.back.SSLConnectionHelper;
import javapns.data.Device;
import javapns.data.PayLoad;

public class iPhoneAlertSender extends Thread
{
	public static int RUN_MODE_DEVELOPMENT = 1;
	public static int RUN_MODE_PRODUCTION = 2;
	
	String authKey;
	String deviceKey;
	String msg;
	String userId;
	
	public iPhoneAlertSender(String userId, String authKey, String deviceKey, String msg)
	{
		this.userId = userId;
		this.authKey = authKey;
                this.deviceKey = deviceKey;
		this.msg = msg;
		
		if ( deviceKey.indexOf("Error") >= 0 ) return;
		this.start();
	}
	
	public void run()
	{
		try
		{
			PayLoad payLoad = new PayLoad();
			System.out.println("AddAlert : " + msg);
			payLoad.addAlert(msg);
			
			payLoad.addBadge(0);
			payLoad.addSound("default");
			
			PushNotificationManager pushManager = PushNotificationManager.getInstance();
			System.out.println("addUser : " + userId + ":" + deviceKey);
			try
			{
				pushManager.addDevice(userId, deviceKey);
			}
			catch(Exception ee)
			{
				pushManager.removeDevice(userId);
				pushManager.addDevice(userId, deviceKey);
				ee.printStackTrace();
			}
			
			String host = null;
			String certificatePath = null;
			int runMode = RUN_MODE_PRODUCTION;

			if (runMode == RUN_MODE_DEVELOPMENT)
			{
				host = "gateway.sandbox.push.apple.com";
				certificatePath = authKey;
			}
			else if ( runMode == RUN_MODE_PRODUCTION )
			{
				host = "gateway.push.apple.com";
				certificatePath = authKey;
			}
			
			System.out.println("Keys : " + host + ":" + certificatePath);
			
			int port = 2195;
			
			String certificatePassword = "49100hsy";
			
			pushManager.initializeConnection(host, port, certificatePath, certificatePassword, SSLConnectionHelper.KEYSTORE_TYPE_PKCS12);
			System.out.println("userid : " + userId);
			Device client = pushManager.getDevice(userId);
			pushManager.sendNotification(client, payLoad);
			pushManager.stopConnection();
			pushManager.removeDevice(userId);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
