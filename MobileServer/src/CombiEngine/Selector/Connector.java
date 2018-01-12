package CombiEngine.Selector;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.*;
import CombiEngine.SessionManager.SessionManager;

public class Connector
{
	public	SocketChannel	sc;
	public	SelectProcessor	processor;
	public	SelectionKey	skey = null;
	public	ConcurrentLinkedQueue <byte[]> sendBufferList;
	public	SessionManager sessionManager;
	private	long timer;
	private	boolean autoClose = false;
	public	Object item = null;
	public	String itemKey = null;
	public	String itemType = null;
	private AtomicBoolean m_bClosed = new AtomicBoolean(false);
	
	public	int readItemCount = 0;
	
	public	AtomicBoolean m_bFirst = new AtomicBoolean(true);
	private	AtomicBoolean m_bWorking = new AtomicBoolean(false);
	private	AtomicBoolean isCanceled = new AtomicBoolean(false);
	
	private ByteBuffer sendBuffer = null;
	
	public	String ip = "";
	public	String password = "";
		
	public int MAX_BUFFER;
	
	//SSL
	public SSLProcessor sslProcessor = null;
	
	private boolean m_bUnRegistered;
	
	public long noopTimer;	// NOOP TIMER

	public Connector(SocketChannel sc, SelectProcessor processor, SessionManager sessionManager, int MAX_BUFFER)
	{
		this.processor = processor;
		this.sc = sc;
		this.sessionManager = sessionManager;
		this.MAX_BUFFER = MAX_BUFFER;
		
		sendBuffer = ByteBuffer.allocateDirect(MAX_BUFFER);
		
		sendBufferList = new ConcurrentLinkedQueue<byte[]>();
		
		sendBuffer.clear();
		sendBuffer.flip();
		
		resetTimer();
		
		m_bUnRegistered = false; 
		
		noopTimer = sessionManager.getTimer();
	}
	
	public void sendNoop(String str)
	{
		send(str + "\f");
		
		noopTimer = sessionManager.getTimer();
	}
	
	public String getIP()	// 20170716
	{
		try
		{
			String sa = sc.socket().getRemoteSocketAddress().toString();
			
			if ( sa.indexOf("/") == 0 && sa.indexOf(":") >= 0 )
			{
				sa = sa.substring(1);
				sa = sa.substring(0, sa.indexOf(":"));
				return sa;
			}
		}
		catch(Exception e) {}
		
		return "";
	}
	
	public String getServerIp()
	{
		String sa = sc.socket().getLocalSocketAddress().toString();
		
		if ( sa.indexOf("/") == 0 && sa.indexOf(":") >= 0 )
		{
			sa = sa.substring(1);
			sa = sa.substring(0, sa.indexOf(":"));
			return sa;
		}
		
		return "";
	}
	
	public boolean isCanceled()
	{
		return this.isCanceled.get();
	}
	
	public void Canceled()
	{
		this.isCanceled.set(true);
	}

	public void setWorking(boolean m_bWorking)
	{
		this.m_bWorking.set(m_bWorking);
	}

	public boolean getWorking()
	{
		return this.m_bWorking.get();
	}
	
	public void send(String msg)
	{
		byte[] ar = msg.getBytes();
		
		if ( m_bUnRegistered )
		{
			try
			{
				sslProcessor.write(ar);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				close();
			}
		}
		else
		{
			send(ar, ar.length);
		}
	}
	
	public boolean sendByteBuffer(byte[] buf)
	{
		send(buf, buf.length);
		
		return true;
	}
	
	public boolean send(byte[] buf)
	{
		send(buf, buf.length);
		
		return true;
	}
	
	public void send(byte[] ar, int size)
	{
		if ( !send_or_close(ar, size) )
		{
			close();
		}
	}
	
	public boolean send_or_close(byte[] ar, int size)
	{
		resetTimer();
	
		for ( int i = 0 ; i < size ; i += MAX_BUFFER )
		{
			byte[] buf;
			
			if ( ( i + MAX_BUFFER ) <= size )
			{
				buf = new byte[MAX_BUFFER];
			}
			else
			{
				buf = new byte[size - i];
			}
			
			System.arraycopy(ar, i, buf, 0, buf.length);
			
			sendBufferList.add(buf);
		}
		
		return true;
	}
	
	public void write()
	{
		if ( !write_or_close() && !isCanceled() )
		{
			close();
		}
	}
	
	public boolean write_or_close()
	{
		if ( sslProcessor != null && sendBufferList.size() > 0 )
		{
			byte[] b = sendBufferList.poll();
			
			try
			{
				sslProcessor.write(b);
			}
			catch(Exception e)
			{
				e.printStackTrace();
				
				return false;
			}
		}
		else if ( sslProcessor == null )
		{
			if ( sendBuffer.hasRemaining() )
			{
				try
				{
					if ( sc.write(sendBuffer) < 0 ) return false;
				}
				catch(java.io.IOException ie)
				{
					return false;
				}
			}
		}
		
		if ( !sendBuffer.hasRemaining() && sendBufferList.size() == 0 && autoClose )
		{
			return false;
		}
		
		return true;
	}
	
	public void close()
	{
		if ( m_bUnRegistered )
		{
			try
			{
				sc.close();
			}
			catch(Exception e) {}
		}
		else
		{
			if ( skey == null )
			{
				processor.connectorList.remove(this);
				try { sc.close(); } catch(Exception ee) {}
				sslProcessor.onClosed();
				Canceled();
			}
			else
			{
				processor.disconnect(skey);
			}
		}
	}
	
	public void resetTimer()
	{
		this.timer = sessionManager.getTimer();
	}
	
	public long getTimer()
	{
		return this.timer;
	}
	
	public void setTimer(long timer)
	{
		this.timer = timer;
	}
	
	public void setObject(Object obj)
	{
		this.item = obj;
	}
	
	public Object getObject()
	{
		return this.item;
	}
	
	public void setAutoClose(boolean autoClose)
	{
		this.autoClose = autoClose;
		
		if ( sendBufferList.size() == 0 && !sendBuffer.hasRemaining() )
		{
			processor.readyClose(this);
		}
	}
	
	public boolean getAutoClose()
	{
		return autoClose;
	}
	
	public boolean isClosable()
	{
		if ( autoClose == true && sendBufferList.size() == 0 && readItemCount == 0 && !sendBuffer.hasRemaining() ) return true;
		else return false;
	}
	
	public boolean isOpen()
	{
		return skey.channel().isOpen();
	}
	
	public boolean getClosedStatus()
	{
		return m_bClosed.get();
	}
	
	public void setClosedStatus(boolean m_bClosed)
	{
		this.m_bClosed.set(m_bClosed);
	}
	
	public void unRegist()
	{
		processor.unRegistConnector(this);
		
		synchronized(this)
		{
			m_bUnRegistered = true;
		}
		
		if ( sslProcessor != null )
		{
			sslProcessor.unRegisted();
		}
	}
}
