package CombiEngine.Selector;

import java.nio.ByteBuffer;
import java.nio.channels.Selector;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.util.Iterator;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.io.IOException;
import java.security.KeyStore;
import javax.net.ssl.SSLContext;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import CombiEngine.ItemManager.ItemQueue;
import CombiEngine.Processor.CombiService;
import CombiEngine.Processor.ProcessorThread;
import CombiEngine.SessionManager.SessionManager;

public class SelectProcessor extends Thread
{
	public	ItemQueue	queue;
	public	Selector	selector;
	public	boolean		serviceStarted = false;
	
	private	List<Connector> connectList;
	private	List<Connector> disconnectList;
	public List<Connector> connectorList;
	public List<Connector> readyCloseList;
	
	public	SessionManager	sessionManager = null;
	public	AtomicLong recvSize = new AtomicLong(0);
	public	AtomicLong sendSize = new AtomicLong(0);
	public	ProcessorThread processor;
	
	private	int MAX_BUFFER;
	private ByteBuffer	recvBuffer = null;
	
	//SSL
	//private ByteBuffer	inAppBuffer = null;
	
	private SSLContext	sslContext = null;
	private boolean		useSSL = false;
	
	private SSLSender	sslSender = null;	// 20170427
	
	public SelectProcessor(CombiService server, int MAX_BUFFER)
	{
		this.connectList = Collections.synchronizedList(new ArrayList<Connector>());
		this.disconnectList = Collections.synchronizedList(new ArrayList<Connector>());
		this.connectorList = Collections.synchronizedList(new ArrayList<Connector>());
		this.readyCloseList = Collections.synchronizedList(new ArrayList<Connector>());
		this.MAX_BUFFER = MAX_BUFFER;
		recvBuffer = ByteBuffer.allocateDirect(MAX_BUFFER);
		
		this.queue = new ItemQueue();
		
		if ( initSelector() )
		{
			processor = new ProcessorThread(server, queue);
			new WriteProcessor(connectorList);
			start();
			
			serviceStarted = true;
		}
	}
	
	public SelectProcessor(CombiService server, int MAX_BUFFER, String keyStorePath, char[] passPhrase)
	{
		this.connectList = Collections.synchronizedList(new ArrayList<Connector>());
		this.disconnectList = Collections.synchronizedList(new ArrayList<Connector>());
		this.connectorList = Collections.synchronizedList(new ArrayList<Connector>());
		this.readyCloseList = Collections.synchronizedList(new ArrayList<Connector>());
		this.MAX_BUFFER = MAX_BUFFER;
		recvBuffer = ByteBuffer.allocateDirect(MAX_BUFFER);
		
		this.queue = new ItemQueue();
		
		if ( initSelector() )
		{
			processor = new ProcessorThread(server, queue);
			new WriteProcessor(connectorList);	// 20170427-1
			sslSender = new SSLSender();	// 20170427
			start();
			
			serviceStarted = true;
		}
		
		java.io.FileInputStream keyFileIS = null;
		
		try
		{
			recvBuffer = ByteBuffer.allocateDirect(MAX_BUFFER);
			
			keyFileIS = new java.io.FileInputStream(keyStorePath);
			
			// initialize
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(keyFileIS, passPhrase);
			
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
			kmf.init(ks,passPhrase);
			
			sslContext = SSLContext.getInstance("TLSv1.2");
			sslContext.init(kmf.getKeyManagers(), null, null);
			
			useSSL = true;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if ( keyFileIS != null ) { try { keyFileIS.close(); keyFileIS = null; } catch(Exception e) { e.printStackTrace(); } }
		}
	}
	
	public SelectProcessor(CombiService server)
	{
		this(server, 4096);
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
	
	public int getCount()
	{
		return selector.keys().size();
	}
	
	public String getDetail()
	{
		StringBuffer str = new StringBuffer();
		
		Collection<SelectionKey> col = selector.keys();
		
		Iterator<SelectionKey> it = col.iterator();
		
		while ( it.hasNext() )
		{
			SelectionKey skey = (SelectionKey)it.next();
			
			if ( skey != null )
			{
				Connector conn = (Connector)skey.attachment();
				if ( conn != null )
				{
					str.append("#" + conn.item + "(" + conn.m_bFirst + ":" + conn.skey + ")" + "#");
				}
				else
				{
					str.append("#null#");
				}
			}
		}
		
		return str.toString();
	}
	
	public void unRegistConnector(Connector conn)// 20170422-2
	{
		try
		{
			synchronized(connectorList)
			{
				connectorList.remove(conn);
			}
		}
		catch(Exception e) { e.printStackTrace(); }
	}
	
	public void registConnector(SocketChannel channel)
	{
		try
		{
			if ( useSSL )
			{
				connectList.add(new Connector(channel, this, sessionManager, MAX_BUFFER));
				
				selector.wakeup();
			}
			else
			{
				connectList.add(new Connector(channel, this, sessionManager, MAX_BUFFER));
				
				selector.wakeup();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			try { channel.close(); } catch(Exception ee) {}
			try { selector.wakeup(); } catch(Exception ee) {}
		}
	}
	
	public void registAfterHandshake(Connector conn)
	{
		connectList.add(conn);
		
		selector.wakeup();
	}
	
	public void run()
	{
		while ( true )
		{
			try
			{
				while ( connectList.size() > 0 )	// 20170422-2	// 20170630
				{
					/*Connector conn = (Connector)connectList.remove(0);
					
					if ( useSSL == true )
					{
						if ( conn.sslProcessor == null )
						{
							conn.sslProcessor = new SSLProcessor(conn.sc, sslContext, queue, null, conn, this, sslSender);	// 20170427
							conn.sc.configureBlocking(true);
							conn.sslProcessor.beginHandShake();							// 20170411
							continue;
						}
					}
					
					conn.sc.configureBlocking( false );
					SelectionKey skey = null;
					
					try
					{
						skey = conn.sc.register(selector, SelectionKey.OP_READ);
						
						conn.sslProcessor.skey = skey;
					}
					catch(Exception e)
					{
						e.printStackTrace();
						conn.close();
						
						continue;
					}
					
					connectorList.add(conn);
					
					if ( skey != null )	// 20170422
					{
						conn.skey = skey;
						skey.attach(conn);
						queue.connect(skey);
					}*/
					
					Connector conn = (Connector)connectList.remove(0);
					
					conn.sc.configureBlocking( false );
					SelectionKey skey = conn.sc.register(selector, SelectionKey.OP_READ);
					connectorList.add(conn);
					
					//SSL
					if ( useSSL == true )
					{
						conn.sslProcessor = new SSLProcessor(conn.sc, sslContext, queue, skey, conn, this, sslSender);
						conn.sslProcessor.beginHandShake();
					}
					
					if ( skey != null )
					{
						conn.skey = skey;
						skey.attach(conn);
						queue.connect(skey);
					}
				}
				
				while ( disconnectList.size() > 0 )
				{
					Connector c = disconnectList.remove(0);
					System.out.println("[SelectProcessor] run disconnect1");					

					if ( c == null )
					{
						System.out.println("[SelectProcessor] run disconnect2 c null");
						continue;
					}
					
					if ( connectorList.contains(c) )
					{
						connectorList.remove(c);
						System.out.println("[SelectProcessor] run disconnect3 useSSL:"+ useSSL);						

						try
						{
							queue.disconnect(c);
							c.Canceled();
							
							
							if ( useSSL && c.sslProcessor != null )	// 20170427-1
							{
								c.sslProcessor.onClosed();
								System.out.println("[SelectProcessor] run disconnect4");
							}
							else
								System.out.println("[SelectProcessor] run disconnect5");
							
							try { c.skey.cancel(); } catch(Exception e) { e.printStackTrace(); }	// 20170427-1
							try { c.sc.close(); } catch(Exception e) { e.printStackTrace(); }	// 20170427-1
							
							selector.wakeup();
							System.out.println("[SelectProcessor] run disconnect6");
						}
						catch(Exception e) { e.printStackTrace(); }
					}
				}
				
				while ( readyCloseList.size() > 0 )
				{
					Connector conn = readyCloseList.remove(0);
					try
					{
						if ( conn != null && conn.skey != null && !conn.isCanceled() )
						{
							conn.skey.interestOps(SelectionKey.OP_WRITE);
						}
						else if ( conn != null && conn.skey != null )
						{
							if ( connectorList.contains(conn) )
							{
								connectorList.remove(conn);
								conn.Canceled();
	
								if ( useSSL && conn.sslProcessor != null )	// 20170427-1
								{
									conn.sslProcessor.onClosed();
								}
								
								try { conn.skey.cancel(); } catch(Exception e) {}	// 20170427-1
								try { conn.sc.close(); } catch(Exception e) {}		// 20170427-1
								
								selector.wakeup();
								queue.disconnect(conn);
							}
						}
					}
					catch(Exception ee)
					{
						if ( connectorList.contains(conn) )
						{
							connectorList.remove(conn);
							conn.Canceled();
							
							if ( useSSL && conn.sslProcessor != null )	// 20170427-1
							{
								conn.sslProcessor.onClosed();
							}
							
							try { conn.skey.cancel(); } catch(Exception e) {}	// 20170427-1
							try { conn.sc.close(); } catch(Exception e) {}	// 20170427-1
							
							selector.wakeup();
							queue.disconnect(conn);
						}
					}
				}
				
				if ( selector.select() > 0 )
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
									if ( conn != null ) conn.resetTimer();
									else								// 20170416 unlimit error occur
									{
										try
										{
											if ( skey != null )
											{
												skey.cancel();
												skey.channel().close();
											}
										}
										catch(Exception e) {}
										
										continue;
									}
									
									//SSL
									if ( useSSL == true )
									{
										if ( conn.sslProcessor == null )
										{
											try
											{
												Connector c = (Connector)skey.attachment();
												
												if ( connectorList.contains(c) )
												{
													connectorList.remove(c);
													c.Canceled();
												
													if ( useSSL && c.sslProcessor != null )	// 20170427-1
													{
														c.sslProcessor.onClosed();
													}
													
													try { c.skey.cancel(); } catch(Exception e) {}	// 20170427-1
													try { c.sc.close(); } catch(Exception e) {}	// 20170427-1
													
													queue.disconnect(c);
												}
											}
											catch(Exception e) { e.printStackTrace(); }
											
											continue;
										}
										
										// 20170716
										/*if ( !conn.getClosedStatus() && ( conn.sslProcessor.getHandShakeStatus() != HandshakeStatus.NOT_HANDSHAKING ) )	// 20170630
										{
											conn.sslProcessor.handshake(conn.sc);
											
											continue;
										}*/
									}
									
									recvBuffer.clear();
									
									int rcv = 0;
									
									try
									{
										if ( ( rcv = ((SocketChannel)skey.channel()).read(recvBuffer) ) >= 0 )
										{
											recvBuffer.flip();
											
											//SSL
											if ( useSSL == true )	// 20170422-1 && conn.sslProcessor != null )
											{
												//conn.sslProcessor.read(recvBuffer);
												// 20170716
												if ( conn.sslProcessor != null && ( conn.sslProcessor.handshakeFinished == false ) )
												{
													conn.sslProcessor.handshake(recvBuffer, rcv);
													
													continue;
												}
												else
												{
													conn.sslProcessor.read(recvBuffer);
												}
											}
											else
											{
												byte[] buf = new byte[recvBuffer.remaining()];	// 20170427-1
												
												recvBuffer.get(buf);							// 20170427-1
												
												queue.receive(skey, buf, rcv);
											}
										}
										else
										{
											try
											{
												Connector c = (Connector)skey.attachment();
												
												if ( connectorList.contains(c) )
												{
													connectorList.remove(c);
													c.Canceled();
												
													if ( useSSL && c.sslProcessor != null )	// 20170427-1
													{
														c.sslProcessor.onClosed();
													}
													
													try { c.skey.cancel(); } catch(Exception e) {}	// 20170427-1
													try { c.sc.close(); } catch(Exception e) {}	// 20170427-1
													
													selector.wakeup();
													queue.disconnect(c);
												}
											}
											catch(Exception e) 
											{ 
												e.printStackTrace(); 
											}
										}
									}
									catch(IOException ie)
									{
										//ie.printStackTrace();
										try
										{
											Connector c = (Connector)skey.attachment();
											if ( connectorList.contains(c) )
											{
												connectorList.remove(c);
												c.Canceled();
												
												if ( useSSL && c.sslProcessor != null )	// 20170427-1
												{
													c.sslProcessor.onClosed();
												}
												
												try { c.skey.cancel(); } catch(Exception e) {}	// 20170427-1
												try { c.sc.close(); } catch(Exception e) {}	// 20170427-1
												
												selector.wakeup();
												queue.disconnect(c);
											}
										}
										catch(Exception e) { e.printStackTrace(); }
									}
									catch (IllegalStateException ies)
									{
										ies.printStackTrace();
										
										try
										{
												Connector c = (Connector)skey.attachment();
												
												if ( connectorList.contains(c) )
												{
													connectorList.remove(c);
													c.Canceled();

													if ( useSSL && c.sslProcessor != null )	// 20170427-1
													{
														c.sslProcessor.onClosed();
													}
													
													try { c.skey.cancel(); } catch(Exception e) {}	// 20170427-1
													try { c.sc.close(); } catch(Exception e) {}	// 20170427-1

													queue.disconnect(c);
											}
										}
										catch(Exception e) { e.printStackTrace(); }
									}
								}
								else if ( skey.isWritable() )
								{
									disconnect(skey);
								}
								else
								{
									assert false;
								}
							}
						}
						catch(Exception ee)
						{
							ee.printStackTrace();
						}
					}
				}
				else
				{
					try
					{
						sleep(10);
					}
					catch(InterruptedException ie) {}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void disconnect(SelectionKey skey)
	{
		try
		{
			if ( skey == null || !skey.isValid() ) return;
			
			Connector c = (Connector)skey.attachment();
			
			if ( useSSL && c != null )	// 20170422-1
			{
				connectorList.remove(c);
				connectList.remove(c);	// 20170422-1
                                c.Canceled();

				if ( c.sslProcessor != null ) c.sslProcessor.onClosed();	// 20170422-1
				
				if ( c.skey != null ) try { c.skey.cancel(); } catch(Exception e) {}	// 20170422-1
				if ( c != null ) try { c.sc.close(); } catch(Exception e) {}	// 20170422-1
				
				selector.wakeup();
				queue.disconnect(c);
			}
			
			if ( skey.attachment() != null )
			{
				Connector conn = (Connector)skey.attachment();
				readyCloseList.remove(conn);
				
				if ( !conn.getClosedStatus() )
				{
					conn.setClosedStatus(true);
					
					disconnectList.add(conn);
					
					selector.wakeup();
				}
			}
		}
		catch ( Exception ee )
		{
			ee.printStackTrace();
		}
	}
	
	public void sendComplete(int size)
	{
		sendSize.addAndGet(size);
	}
	
	public String getStatus()
	{
		String ret = recvSize.get() + ":" + sendSize.get() + ":" + getCount();
		
		recvSize.set(0);
		sendSize.set(0);
		return ret;
	}
	
	public void readyClose(Connector conn)
	{
		if ( !readyCloseList.contains(conn) )
		{
			readyCloseList.add(conn);
		}
	}
}
