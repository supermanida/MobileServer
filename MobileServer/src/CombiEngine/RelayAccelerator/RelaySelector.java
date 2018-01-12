package CombiEngine.RelayAccelerator;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Iterator;

import CombiEngine.Selector.Connector;

public class RelaySelector extends Thread
{
	Connector peer1;
	Connector peer2;
	Selector selector;
	
	private ByteBuffer	recvBuffer = ByteBuffer.allocateDirect(4096);
	
	public RelaySelector(Connector peer1, Connector peer2)
	{
		this.peer1 = peer1;
		this.peer2 = peer2;
		
		this.peer1.skey.cancel();
		this.peer2.skey.cancel();
		
		this.start();
	}
	
	public boolean registPeers() throws Exception
	{
		SelectionKey skey1 = peer1.sc.register(selector, SelectionKey.OP_READ);
		
		if ( skey1 != null )
		{
			peer1.skey = skey1;
			skey1.attach(peer1);
		}
		
		SelectionKey skey2 = peer2.sc.register(selector, SelectionKey.OP_READ);
		
		if ( skey2 != null )
		{
			peer2.skey = skey2;
			skey2.attach(peer2);
		}
		
		return true;
	}
	
	private boolean initSelector()
	{
		try
		{
			selector = Selector.open();
			
			return true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			return false;
		}
	}
	
	public void run()
	{
		try
		{
			peer1.send("type\f");
			if ( initSelector() && registPeers() )
			{
				while ( true )
				{
					if ( selector.select(10) > 0 )
					{
						Iterator<SelectionKey> i = selector.selectedKeys().iterator();
						
						while ( i.hasNext() )
						{
							try
							{
								SelectionKey skey = (SelectionKey) i.next();
								i.remove();
								
								if ( skey.isValid() )
								{
									if ( skey.isReadable() )
									{
										Connector conn = (Connector)skey.attachment();
										if ( conn == null ) return;
										
										recvBuffer.clear();
										int rcv = conn.sc.read(recvBuffer);
										recvBuffer.flip();
										
										if ( rcv < 0 ) return;
										
										if ( conn == peer1 )
										{
											while ( recvBuffer.hasRemaining() )
												peer2.sc.write(recvBuffer);
										}
										else if ( conn == peer2 )
										{
											while ( recvBuffer.hasRemaining() )
												peer1.sc.write(recvBuffer);
										}
									}
								}
							}
							catch(Exception ee)
							{
								ee.printStackTrace();
							}
						}
					}
					
					try
					{
						sleep(1);
					}
					catch(InterruptedException ie) {}
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			try { peer1.sc.close(); peer1.skey.cancel(); } catch(Exception e) {}
			try { peer2.sc.close(); peer2.skey.cancel(); } catch(Exception e) {}
		}
	}
}
