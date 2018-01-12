package DBEmulator;

public class UserObject
{
	public String id;
	public String high;
	public String ip;
	public int icon;
	public String name;
	public String password;
	public String order;
	public String param1;
	public String param2;
	public String param3;
	public int mobileOn;

	public String[] userInfo;

	//2016-03-15
	private int photoVersion;
        private int nameVersion;
        private int nickVersion;
	
	public UserObject(String id, String high, String ip, int icon, String name, String password, String order, String param1, String param2, String param3)
	{
		this.id = id;
		this.high = high;
		this.ip = ip;
		this.icon = icon;
		this.name = name;
		this.password = password;
		this.order = order;
		this.param1 = param1;
		this.param2 = param2;
		this.param3 = param3;
		this.mobileOn = 0;
	
		//2016-03-15
		this.photoVersion = 0;		
		this.nameVersion = 0;
		this.nickVersion = 0;

		init();
	}

	//2016-03-15
	public void setNameVersion(int value)
	{
	        this.nameVersion = value;
	}
	
	public void setNickVersion(int value)
	{
	        this.nickVersion = value;
	}

	public void setNick(String value)
	{
			this.param2 = value;
            		System.out.println("[UserObject] UPDATE SET NICK =>:" + this.param2 + ", ID:"+this.id);
	}

	public void setPhotoVersion(int value)
	{
			this.photoVersion = value;
			System.out.println("[UserObject] UPDATE SET PHOTO VER =>"+ this.photoVersion + ", ID:"+this.id); 
	}

	public String getName()
	{
		String[] parse = name.split("#", -1);
		if(parse != null)
			return parse[0];
		
		return "";
	}

	public String getNameField()
	{
		return this.name;
	}

	public int getNameVersion()
	{
	        return this.nameVersion;
	}
	
	public int getNickVersion()
	{
	        return this.nickVersion;
	}
	
	public int getPhotoVersion()
	{
	        return this.photoVersion;
	}
	//

	public void setName(String value)
        {
                	//System.out.println("UPDATE OLD NAME:"+name);
               		String[] parse = name.split("#", -1);
                        parse[0] = value;

                        String ret = "";
                        for(int i=0; i<parse.length; i++)
                                ret += parse[i] + "#";

                        name = ret;
                        //System.out.println("UPDATE SET EMAIL => name:"+name);
        }

	public void init() {
		userInfo = name.split("#", -1);
	}
	
	public String getUserInfo(int index) {
		String info = "";
		
		try {
			info = userInfo[index];
		} catch(Exception e) {
			info = "";
		}
		
		return info;
	}
}
