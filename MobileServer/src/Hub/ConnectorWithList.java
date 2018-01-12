package Hub;

import CombiEngine.Selector.Connector;
import DBEmulator.PartObject;
import DBEmulator.UserObject;
import DBEmulator.DataBean;

import java.util.ArrayList;

public class ConnectorWithList
{
	public Connector conn; //2017-10-20
	
	public ArrayList<PartObject> parts;
	public ArrayList<UserObject> users;
	
	boolean m_bDebug;
	DataBean dataBean;
	
	public ConnectorWithList(Connector conn, boolean m_bDebug, DataBean dataBean)
	{
		this.conn = conn;
		this.m_bDebug = m_bDebug;
		this.dataBean = dataBean;
		
		this.parts = new ArrayList<PartObject>();
		this.users = new ArrayList<UserObject>();
	}
	
	public synchronized void addObserver(PartObject part)
	{
		if ( !parts.contains(part) )
		{
			dataBean.addObserver(part.id, this);
			parts.add(part);
		}
		
		if ( m_bDebug ) System.out.println("ConnectorWithList(" + part.name + ") Part Added : " + parts.size());
		if ( m_bDebug ) System.out.println("Part Observer Added : " + dataBean.getObservers(part.id).size());
	}
	
	public synchronized void addObserver(UserObject user)
	{
		if ( !users.contains(user) )
		{
			dataBean.addObserver(user.id, this);
			users.add(user);
		}
		
		if ( m_bDebug ) System.out.println("ConnectorWithList(" + user.name + ") User Added : " + users.size());
		if ( m_bDebug ) System.out.println("User Observer Added : " + dataBean.getObservers(user.id).size());
	}
	
	public synchronized void removeObserver(PartObject part)
	{
		parts.remove(part);
		
		dataBean.removeObserver(part.id, this);
		
		if ( m_bDebug ) System.out.println("ConnectorWithList(" + part.name + ") Part Removed : " + parts.size());
		if ( m_bDebug ) System.out.println("Part Observer Removed : " + dataBean.getObservers(part.id).size());
	}
	
	public synchronized void removeObserver(UserObject user)
	{
		users.remove(user);
		
		dataBean.removeObserver(user.id, this);
		
		if ( m_bDebug ) System.out.println("ConnectorWithList(" + user.name + ") User Removed : " + users.size());
		if ( m_bDebug ) System.out.println("User Observer Removed : " + dataBean.getObservers(user.id).size());
	}
	
	public void close()
	{
		for ( int i = 0 ; i < parts.size() ; i++ )
		{
			dataBean.removeObserver(parts.get(i).id, this);
			
			if ( m_bDebug ) System.out.println("Part Observer Removed : " + dataBean.getObservers(parts.get(i).id).size());
		}
		
		for ( int i = 0 ; i < users.size() ; i++ )
		{
			dataBean.removeObserver(users.get(i).id, this);
			
			if ( m_bDebug ) System.out.println("User Observer Removed : " + dataBean.getObservers(users.get(i).id).size());
		}
		
		parts.clear();
		users.clear();
		
		if ( m_bDebug ) System.out.println("ConnectorWithList Part Removed : " + parts.size());
		if ( m_bDebug ) System.out.println("ConnectorWithList User Removed : " + users.size());
		
		parts = null;
		users = null;
		
		this.conn = null;
	}
}