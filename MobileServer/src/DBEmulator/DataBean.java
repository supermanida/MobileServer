package DBEmulator;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import CombiEngine.Codec.AmCodec;
import CombiEngine.Selector.Connector;
import Hub.ConnectorWithList;
import Hub.HubProcessor;

public class DataBean
{
	public ConcurrentHashMap<String, PartObject> parts;
	public ConcurrentHashMap<String, UserObject> users;
	private AmCodec codec;
	private boolean m_bDebug;
	private ConcurrentHashMap<String, Observer> obs;
	public ConcurrentHashMap<String, String> phoneNumbers;
	
	public DataBean(boolean m_bDebug)
	{
		this.parts = new ConcurrentHashMap<String, PartObject>();
		this.users = new ConcurrentHashMap<String, UserObject>();
		
		this.codec = new AmCodec();
		this.m_bDebug = m_bDebug;
		
		PartObject topPart = new PartObject("0", "TOP", "TOP", "0", "");
		parts.put("0", topPart);
		
		obs = new ConcurrentHashMap<String, Observer>();
		
		this.phoneNumbers = new ConcurrentHashMap<String, String>();
	}

	public void addSubPart() {
		Iterator<PartObject> it = parts.values().iterator();
		
		while ( it.hasNext() ) {
			PartObject prt = it.next();
			
			if ( prt == null ) continue;
			
			PartObject parent = parts.get(prt.high); 
			
			if ( parent != null ) 
			{
				parent.subParts.add(prt);
			}
		}
	}
	
	public void addSubUser() 
	{
		Iterator<UserObject> it = users.values().iterator();
		
		while ( it.hasNext() ) {
			UserObject usr = it.next();
			
			if ( usr == null ) continue;
			
			PartObject parent = parts.get(usr.high);
			
			if ( parent != null ) {
				parent.subUsers.add(usr);
			}
		}
	}

	public boolean addPart(String id, String high, String name, String order, String param1)
	{
		PartObject newPart = new PartObject(id, high, name, order, param1);
		parts.put(id, newPart);
		
		if(HubProcessor.isOrgEnd)
		{
        		PartObject parentPart = parts.get(high);
        		if ( parentPart == null )
        		{
        			parentPart = new PartObject(high, "TOP", "TOP", "0", "");
        			parts.put(high, parentPart);
        		}
        		parentPart.subParts.add(newPart);
		}
		
		return true;
	}
	
	public boolean addUser(String id, String high, String ip, String icon, String name, String password, String order, String param1, String param2, String param3)
	{
		UserObject newUser = new UserObject(id, high, ip, Integer.parseInt(icon), name, password, order, param1, param2, param3);
		users.put(id, newUser);
		
		if(HubProcessor.isOrgEnd)
		{
        		PartObject parentPart = parts.get(high);
        		if ( parentPart == null )
        		{
        			parentPart = new PartObject(high, "0", "", "", "");
        			parts.put(high, parentPart);
        		}
        		
        		parentPart.subUsers.add(newUser);
    			setPhone(newUser);
		}
		
		setPhone(newUser);
		
		return true;
	}
	
	public void setPhone(UserObject user)
	{
		String phone1 = user.getUserInfo(3);
		String phone2 = user.getUserInfo(5);
		String phone3 = user.getUserInfo(4);
		
		phone1 = phone1.replaceAll("-", "");
		phone1 = phone1.replaceAll(" ", "");
		
		phone2 = phone2.replaceAll("-", "");
		phone2 = phone2.replaceAll(" ", "");
		
		phone3 = phone3.replaceAll("-", "");
		phone3 = phone3.replaceAll(" ", "");
		
		if ( !phone1.equals("") ) phoneNumbers.put(phone1, user.id + "\t" + user.name);
		if ( !phone2.equals("") ) phoneNumbers.put(phone2, user.id + "\t" + user.name);
		if ( !phone3.equals("") ) phoneNumbers.put(phone3, user.id + "\t" + user.name);
	}
	
	public String getIdFromPhone(String phone)
	{
		phone = phone.replaceAll("-", "");
		phone = phone.replaceAll(" ", "");
		
		String userId = null;
		
		userId = phoneNumbers.get(phone);
		
		if ( userId == null ) return "";
		else return userId;
	}
	
	public boolean modPart(String id, String high, String name, String order, String param1)
	{
		PartObject part = parts.get(id);
		
		if ( part == null )
		{
			this.addPart(id, high, name, order, param1);
		}
		else
		{
		
			if ( part.id.equals(id) && part.high.equals(high) && part.name.equals(name) && part.order.equals(order) && part.param1.equals(param1) ) return false;
			
			PartObject oldPart = parts.get(part.high);
			if ( oldPart != null ) {
				oldPart.subParts.remove(part);
			}
			PartObject newPart = parts.get(high);
			if ( newPart != null ) newPart.subParts.add(part);
			
			part.high = high;
			
			part.name = name;
			part.order = order;
			part.param1 = param1;
		}
		
		return true;
	}
	
	public boolean modUser(String id, String high, String name, String password, String order, String param1, String param2, String param3)
	{
		UserObject user = users.get(id);
		
		if ( user == null )
		{
			this.addUser(id, high, "", "0", name, password, order, param1, param2, param3);
		}
		else
		{
			if ( user.id.equals(id) && user.high.equals(high) && user.name.equals(name) && user.password.equals(password) && user.order.equals(order) && user.param1.equals(param1) && user.param2.equals(param2) && user.param3.equals(param3) ) return false;
			
			PartObject oldPart = parts.get(user.high);
			if ( oldPart != null ) oldPart.subUsers.remove(user);
			
			PartObject newPart = parts.get(high);
			if ( newPart != null ) newPart.subUsers.add(user);
			
			user.high = high;
			
			user.name = name;
			user.password = password;
			user.order = order;
			user.param1 = param1;
			user.param2 = param2;
			user.param3 = param3;
		}
		
		setPhone(user);
		
		return true;
	}
	
	public PartObject delPart(String id)
	{
		PartObject part = parts.get(id);
		
		if ( part == null ) return null;
		
		PartObject highPart = parts.get(part.high);
		
		if ( highPart != null ) highPart.subParts.remove(part);
		
		return parts.remove(id);
	}
	
	public UserObject delUser(String id)
	{
		UserObject user = users.get(id);
		
		if ( user == null ) return null;
		
		PartObject highPart = parts.get(user.high);
		
		if ( highPart != null ) highPart.subParts.remove(user);
		
		return users.remove(id);
	}
	
	public void SetIcon(String id, String icon)
	{
		UserObject usr = users.get(id);
		
		if ( usr != null ) usr.icon = Integer.parseInt(icon);
	}
	
	public void SetNickName(String id, String nickName)
	{
		UserObject usr = users.get(id);
		
		if ( usr != null ) usr.param2 = nickName;
	}
	
	public UserObject SendMyFolder(String id, String high, String name, String icon, Connector conn)
	{
		UserObject usr = users.get(id);
		String nick = "";
		String mobile = "0";
		
		if(icon.equals("0"))
		{
			mobile = Integer.toString(usr.mobileOn);
		}
		
		if ( !icon.equals("6") && usr == null ) return null;
		if ( !icon.equals("6") )
		{
			name = usr.name;
			nick = usr.param2;
			icon = usr.icon + "";
		}
		
		if ( id.equals("1") )
		{
			UserObject requestUser = users.get(conn.ip);
			
			if ( requestUser != null )
			{
				id = requestUser.high;
			}
		}
		
		sendMessage(conn, "MyFolder\t" + id + "\t" + high + "\t" + name + "\t" + icon + "\t" + nick + "\t" + mobile);
		
		return usr;
	}
	
	public void sendMessage(Connector conn, String message)
	{
		if ( message.indexOf("\f") >= 0 ) message = message.replaceAll("\f", "");
		
		conn.send(codec.EncryptSEED(message) + "\f");
	}
	
	public void addObserver(String id, ConnectorWithList connList)
	{
		Observer ob = obs.get(id);
		
		if ( ob == null )
		{
			ob = new Observer();
			obs.put(id, ob);
		}
		
		ob.addObserver(connList);
	}
	
	public void removeObserver(String id, ConnectorWithList connList)
	{
		Observer ob = obs.get(id);
		
		if ( ob == null ) return;
		
		ob.removeObserver(connList);
	}
	
	public ConcurrentLinkedQueue<ConnectorWithList> getObservers(String id)
	{
		Observer ob = obs.get(id);
		
		if ( ob == null ) return null;
		else return ob.observers;
	}
	
	public void sendName(Connector conn)
	{
		UserObject usr = users.get(conn.itemKey);
		
		System.out.println(conn.itemKey + " : " + usr);
		if ( usr != null )
			sendMessage(conn, "Name\t" + conn.itemKey + "\t" + usr.name);
	}
	
	public void sendName(String id, Connector conn)
	{
		UserObject usr = users.get(id);

		PartObject part = parts.get(usr.high);
		
		if ( usr != null )
			sendMessage(conn, "UserName\t" + id + "\t" + usr.name + "\t" + part.name);
	}
	
	public void sendNick(Connector conn)
	{
		UserObject usr = users.get(conn.itemKey);
		
		if ( usr != null )
			sendMessage(conn, "Nick\t" + conn.itemKey + "\t" + usr.param2);
	}
}
