package CombiEngine.Mobile;

public class PushItem
{
	private String mUserId = null;
	private String mDeviceToken = null;
	private String mChatRoomId = null;	
	private String mUserName = null;
	private int mPushRetryCount = 0;
	private String mSendTime = null;

	public PushItem(String userId,  String token, String roomId, String name, int count, String time)
	{
		this.mUserId = userId;
		this.mDeviceToken = token;
		this.mChatRoomId = roomId;
		this.mUserName = name;
		this.mPushRetryCount = count;
		this.mSendTime = time;	
	}

	public String getUserId()
	{
		return this.mUserId;
	}

	public String getDeviceToken()
	{
		return this.mDeviceToken;
	}

	public String getChatRoomId()
	{
		return this.mChatRoomId;
	}

	public String getUserName()
	{
		return this.mUserName;
	}

	public int getPushRetryCount()
	{
		return this.mPushRetryCount;
	}

	public void setPushRetryCount()
	{
		this.mPushRetryCount++;		
	}

	public String getSendTime()
	{
		return this.mSendTime;
	}
}
