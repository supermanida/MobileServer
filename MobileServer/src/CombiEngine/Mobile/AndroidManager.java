package CombiEngine.Mobile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.MulticastResult;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;

public class AndroidManager
{
	private final static String TAG = "Android Manager";
	
	public final static int INVALID_USERID = -1;
	public final static int NONEXISTED_REGISTRATIONID = -2;
	public final static int GCM_SEND_SUCCESS = 0; 
	
	Sender sender;
	
	public AndroidManager()
	{
		// GOOGLE GCM Server API Key
		sender = new Sender("AIzaSyDVPHu-UXZ3gpNfIEl_0bkgb3wKCOVnRjA");
	}
	
	public int sendAndroidPushMessage(String userId, String regId) throws IOException
	{
		new SendAndroidPushMessage(userId, regId);
		
		return GCM_SEND_SUCCESS;
	}

	public int sendAndroidMultiPushMessage(String message, List<String> list)
	{
		new SendMultiPush(message, list);
		return GCM_SEND_SUCCESS;
	}

	class SendMultiPush extends Thread
	{
		String msg;
		List<String> list;

		public SendMultiPush(String _msg, List<String> _list)
		{
			this.msg = _msg;
			this.list = _list;

			this.start();
		}

		public void run()
		{
			Message.Builder messageBuilder = new Message.Builder();
			messageBuilder.delayWhileIdle(false);
			messageBuilder.timeToLive(0);
			messageBuilder.collapseKey("collapseKey" + System.currentTimeMillis());
			messageBuilder.addData("msg",msg);

            try
            {
            		
                    sender.send(messageBuilder.build(), list, 5);
	/*
                    if (multiResult != null)
                    {
                            List<Result> resultList = multiResult.getResults
                                            ();
                            for (Result result : resultList)
                            {
                                    System.out.println("Message ID : " + result.getMessageId());
                                    System.out.println("ErrorCode  : " + result.getErrorCodeName());
                                    System.out.println("canonicalID: " + result.getCanonicalRegistrationId());
                            }
                    }
	*/
            }
            catch(Exception e)
            {
                    e.printStackTrace();
            }
		}
	}
	
	class SendAndroidPushMessage extends Thread
	{
		String userId;
		String regId;
		
		public SendAndroidPushMessage(String userId, String regId)
		{
			this.regId = regId;
			this.userId = userId;
			
			this.start();
		}
		
		public void run()
		{
			System.out.println("sendAndroidPushMessage");
	
			Message message = new Message.Builder().addData("msg", "push notify").build();
			List<String> list = new ArrayList<String>();
			list.add(regId);
			
			try
			{
				MulticastResult multiResult = sender.send(message, list, 5);
				
				if (multiResult != null)
				{
					List<Result> resultList = multiResult.getResults
							();
					for (Result result : resultList)
					{
						System.out.println("Message ID : " + result.getMessageId());
						System.out.println("ErrorCode  : " + result.getErrorCodeName());
						System.out.println("canonicalID: " + result.getCanonicalRegistrationId());
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
}
