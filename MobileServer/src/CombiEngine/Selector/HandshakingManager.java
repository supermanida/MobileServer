package CombiEngine.Selector;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

// 20170620 handshake finish

public class HandshakingManager extends Thread
{
	public static HandshakingManager manager = null;
	
	private int aliveSecond = 30;
	private ConcurrentLinkedQueue<Connector> ar;
	
	public HandshakingManager()
	{
		this.ar = new ConcurrentLinkedQueue<Connector>();
		
		this.start();
	}
	
	public static HandshakingManager getInstance()
	{
		if ( manager == null )
		{
			manager = new HandshakingManager();
		}
		
		return manager;
	}
	
	public void startHandshake(Connector conn)
	{
		ar.add(conn);
	}
	
	public void finishHandshake(Connector conn)
	{
		ar.remove(conn);
	}
	
	public void run()
	{
		while ( true )
		{
			try { sleep(1000); } catch(InterruptedException ee) {}
			
			Iterator<Connector> it = ar.iterator();
			
			while ( it.hasNext() )
			{
				Connector conn = it.next();
				
				if ( ( conn.sessionManager.getTimer() - conn.getTimer() ) > aliveSecond )
				{
					conn.close();
					
					finishHandshake(conn);
				}
			}
		}
	}
}