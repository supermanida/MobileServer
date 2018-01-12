package DBEmulator;

import java.util.concurrent.ConcurrentLinkedQueue;
import Hub.ConnectorWithList;

public class Observer
{
	public ConcurrentLinkedQueue<ConnectorWithList> observers;
	
	public Observer()
	{
		observers = new ConcurrentLinkedQueue<ConnectorWithList>();
	}
	
	public void addObserver(ConnectorWithList conn)
	{
		while ( !observers.contains(conn) )
			observers.add(conn);
	}
	
	public void removeObserver(ConnectorWithList conn)
	{
		while ( observers.contains(conn) )
		{
			observers.remove(conn);
		}
	}
	
	public ConcurrentLinkedQueue<ConnectorWithList> getObservers()
	{
		return observers;
	}
}