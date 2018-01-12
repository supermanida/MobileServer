package CombiEngine.Selector;

import java.nio.channels.ServerSocketChannel;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.net.InetSocketAddress;
import java.util.LinkedList;
import java.util.Iterator;

import CombiEngine.Processor.CombiService;
import CombiEngine.SessionManager.SessionManager;

public class SelectService extends Thread
{
	private	Selector selector;
	private	ServerSocketChannel ssc;
	
	public	LinkedList<SelectProcessor> selectors;
	private	int selectorCount = 10;
	
	private	int port;
	private	SessionManager sessionManager;
	private	long aliveSecond;
	private	CombiService server;
	private	int MAX_BUFFER;
	
	//SSL
	private String keyStorePath = null;
	private char[] passPhrase = null;
	private boolean useSSL = false;
	
	public static Test test;
	
	public SelectService(int port, CombiService server, int selectorCount, long aliveSecond)
	{
		this(port, server, selectorCount, aliveSecond, 4096, null, null);
	}
	
	public SelectService(int port, CombiService server, int selectorCount, long aliveSecond, String keyStorePath, char[] passPhrase)
	{
		this(port, server, selectorCount, aliveSecond, 4096, keyStorePath, passPhrase);
	}
	
	public SelectService(int port, CombiService server, int selectorCount, long aliveSecond, int MAX_BUFFER, String keyStorePath, char[] passPhrase)
	{
		this.port = port;
		this.MAX_BUFFER = MAX_BUFFER;
		this.selectorCount = selectorCount;
		this.aliveSecond = aliveSecond;
		this.server = server;
		
		//this.test = new Test(); // 2016-10-27
		
		//SSL
		if ( keyStorePath != null && passPhrase != null )
		{
			this.keyStorePath = keyStorePath;
			this.passPhrase = passPhrase;
			this.useSSL = true;
		}
		
		if ( initService() )
		{
			start();
		}
	}
	
	private boolean initService()
	{
		try
		{
			selector = Selector.open();
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		
		try
		{
			ssc = ServerSocketChannel.open();
			ssc.configureBlocking(false);
			(ssc.socket()).bind(new InetSocketAddress(this.port));
			
			ssc.register(selector, SelectionKey.OP_ACCEPT);
		}
		catch(Exception e)
		{
			e.printStackTrace();
			return false;
		}
		
		selectors = new LinkedList<SelectProcessor>();
		
		LinkedList<Selector> selectorsList = new LinkedList<Selector>();
		
		for ( int i = 0 ; i < selectorCount ; i++ )
		{
			SelectProcessor sp = null;
			//SSL
			if ( useSSL == true )
			{
				sp = new SelectProcessor(server, MAX_BUFFER, keyStorePath, passPhrase);
			}
			else
			{
				sp = new SelectProcessor(server, MAX_BUFFER);
			}
			
			selectors.add(sp);
			selectorsList.add(sp.selector);
		}
		
		sessionManager = new SessionManager(aliveSecond, selectorsList);
		
		Iterator<SelectProcessor> it = selectors.iterator();
		
		while (it.hasNext())
		{
			SelectProcessor sp = (SelectProcessor)it.next();
			sp.sessionManager = sessionManager;
		}
		
		return true;
	}
	
	public void run()
	{
		while ( true )
		{
			try
			{
				if ( selector.select() > 0 )
				{
					Iterator<SelectionKey> i = (selector.selectedKeys()).iterator();
					
					while ( i != null && i.hasNext() )
					{
						try
						{
							SelectionKey skey = (SelectionKey) i.next();
							i.remove();
							
							if ( skey.isValid() )
							{
								if ( skey.isAcceptable() )
								{
									try
									{
										SocketChannel channel = ssc.accept();
										
										if ( channel == null )
										{
											continue;
										}
										
										GetAvailableSelector().registConnector(channel);
									}
									catch(Exception e)
									{
										e.printStackTrace();
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
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			try
			{
				sleep(1);
			}
			catch(Exception ee) { ee.printStackTrace();}
		}
	}
	
	private SelectProcessor GetAvailableSelector()
	{
		int min = 0;
		SelectProcessor selectProcessor = null;
		
		for ( int i = 0 ; i < selectors.size() ; i++ )
		{
			if ( !selectors.get(i).serviceStarted ) continue;
			int t = selectors.get(i).getCount();
			if ( selectProcessor == null || t < min )
			{
				selectProcessor = selectors.get(i);
				min = t;
			}
		}
		return selectProcessor;
	}
}
