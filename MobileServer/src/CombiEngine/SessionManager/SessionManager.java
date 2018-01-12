package CombiEngine.SessionManager;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import java.util.Collection;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import CombiEngine.Selector.Connector;
import CombiEngine.Codec.AmCodec;

public class SessionManager extends Thread
{
	private	long timer;
	private	long aliveSecond = 10;
	private	List<Selector> selectors;
	private List<Connector> closer;
	private long noopTimer = 300;	// NOOP TIMER, 5 minute
	private AmCodec codec;	// NOOP TIMER
	private String noopStr;	// NOOP TIMER
	
	public SessionManager(long aliveSecond, LinkedList<Selector> selectors)
	{
		this.timer = 0;
		this.aliveSecond = aliveSecond;
		this.selectors = Collections.synchronizedList(selectors);
		this.closer = Collections.synchronizedList(new LinkedList<Connector>());
		this.codec = new AmCodec();	// NOOP TIMER
		this.noopStr = codec.EncryptSEED("noop");	// NOOP TIMER
		
		this.start();
	}
	
	@Override
	public void run()
	{
		while ( true )
		{
			try
			{
				try
				{
					sleep(1000);
				}
				catch(InterruptedException ie) {}
				
				timer++;
				
				Iterator<Selector> si = selectors.iterator();
				{
					while ( si.hasNext() )
					{
						try
						{
							Selector sl = si.next();
							
							Collection<SelectionKey> kc = sl.keys();
							Iterator<SelectionKey> ki = kc.iterator();
							
							while ( ki.hasNext() )
							{
								SelectionKey sk = ki.next();
								if ( sk == null || sk.attachment() == null ) continue;
								Connector cn = (Connector)sk.attachment();
								if ( ( getTimer() - cn.getTimer() ) >= aliveSecond )
								{
									cn.close();
									ki.remove();
								}
								
								if ( ( getTimer() - cn.noopTimer ) > noopTimer )	// NOOP TIMER
								{
									cn.sendNoop(noopStr);
								}
							}
						}
						catch(Exception ee){}
					}
				}
				
				LinkedList<Connector> closeClone;
				closeClone = new LinkedList<Connector>(closer);
				closer.clear();
				
				Iterator<Connector> it = closeClone.iterator();
				
				while ( it.hasNext() )
				{
					try
					{	
						Connector c = it.next();
						if ( c != null )
						{ 
							c.close();
						}
					}
					catch(Exception eee){}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public long getTimer()
	{
		return timer;
	}
	
	public void appendCloseItem(Connector conn)
	{
		closer.add(conn);
	}	
}

