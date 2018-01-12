package CombiEngine.Selector;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentLinkedQueue;// 20170427
import java.util.ArrayList;	// send blocking situation, 20170331

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLException;

import CombiEngine.ItemManager.ItemQueue;

public class SSLProcessor
{
	private SSLEngine sslEngine;
	private SSLSession sslSession;

	private SocketChannel sc;
	
	ByteBuffer wrapSrc, unwrapSrc;
	ByteBuffer wrapDst, unwrapDst;
	
	private ItemQueue queue;
	public SelectionKey skey = null;
	
	public boolean isRegisted = true;
	
	private ByteBuffer lastRcv = null;
	
	public int whileCount = 0;
	
	private final boolean useSSLDebug = false;
	private final String debugClient = "211.190.4.125";	// 20170716 The IP of testing user
	
	private boolean isRemoved = false;
	
	public static ByteBufferPool bbPool = null;
	//public Executor taskWorkers = null;
	
	private Connector conn;
	
	public ConcurrentLinkedQueue<byte[]> sendArray;
	private SSLSender mySender;
	
	private SelectProcessor parent;
	
	private ByteBuffer sendBuffer = null;
	
	public boolean handshakeFinished = false;	// 20170716
	
	public SSLProcessor(SocketChannel sc, SSLContext sslContext, ItemQueue queue, SelectionKey skey, Connector conn, SelectProcessor parent, SSLSender sslSender)
	{
		this.sc = sc;
		this.skey = skey;
		this.sslEngine = sslContext.createSSLEngine();
		this.sslEngine.setUseClientMode(false);
		this.conn = conn;
		this.sendArray = new ConcurrentLinkedQueue<byte[]>();
		
		this.parent = parent;
		
		sslSession = sslEngine.getSession();
		
		if ( bbPool == null )
		{
			bbPool = new ByteBufferPool(getAppBufferSize(), getNetBufferSize());
		}
		
		this.wrapSrc = bbPool.getAppBuffer();
		this.wrapDst = bbPool.getNetBuffer();
		
		this.unwrapSrc = bbPool.getNetBuffer();
		this.unwrapDst = bbPool.getAppBuffer();
		
		this.queue = queue;
		
		this.unwrapSrc.clear();
		
		this.mySender = sslSender;
		this.mySender.addProcessor(this);
		
		Test.addSSL();
		
		sendBuffer = ByteBuffer.allocateDirect(getNetBufferSize());
		sendBuffer.clear();
		sendBuffer.flip();
	}
	
	public void unRegisted()
	{
		isRegisted = false;
		
		lastRcv = ByteBuffer.allocate(getNetBufferSize());
	}
	
	public void setReceiveBuffer(ByteBuffer buffer)
	{
		lastRcv = buffer;
	}
	
	public void beginHandShake()
	{
		try
		{
			sslEngine.beginHandshake();
		}
		catch(Exception e)
		{
			if ( conn != null )
			{
				try {  conn.close(); } catch(Exception ee) {}
			}
			else
			{
				try {  onClosed(); } catch(Exception ee) {}
			}
		}
	}
	
	// 20170716
	public void handshake(ByteBuffer rcvBuffer, int size)
	{
		try
		{
			Test.addHandshake();
			
			unwrapSrc.put(rcvBuffer);
			
			while ( true )
			{
				if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("Handshake");
				
				if (this.getHandShakeStatus() == HandshakeStatus.NEED_UNWRAP)
				{
					if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("Handshake : decrypt");
					
					if ( !decrypt() ) break;
				}
				
				if (this.getHandShakeStatus() == HandshakeStatus.NEED_TASK)
				{
					if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("Handshake : doTask");
					
					doTask();
				}
				
				if (this.getHandShakeStatus() == HandshakeStatus.NEED_WRAP)
				{
					if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("Handshake : encrypt");
					
					if ( !encrypt() ) break;
				}
				
				if (this.getHandShakeStatus() == HandshakeStatus.NOT_HANDSHAKING)
				{
					if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("Handshake : not handshaking");
					
					handshakeFinished = true;
					
					while ( decrypt() ) ;	// 20170716
					
					return;
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
			
			conn.close();
			
			return;
		}
		finally
		{
			Test.removeHandshake();
		}
	}
	
	public HandshakeStatus getHandShakeStatus()
	{
		return sslEngine.getHandshakeStatus();
	}
	
	public void read(ByteBuffer rcvBuffer) throws Exception
	{
		unwrapSrc.put(rcvBuffer);
		
		Test.addDecrypt();
		
		while ( decrypt() );
		
		Test.removeDecrypt();
	}
	
	public void write(byte[] sendData) throws Exception
	{
		wrapSrc.put(ByteBuffer.wrap(sendData));
		
		Test.addEncrypt();
		
		encrypt();
		
		Test.removeEncrypt();
	}
	
	public void write(byte[] sendData, int offset, int length) throws Exception
	{
		wrapSrc.put(ByteBuffer.wrap(sendData, offset, length));
		
		Test.addEncrypt();
		
		encrypt();
		
		Test.removeEncrypt();
	}
	
	// 20170716
	public boolean encrypt() throws Exception
	{
		SSLEngineResult wrapResult;
		
		try
		{
			wrapSrc.flip();
			
			wrapResult = sslEngine.wrap(wrapSrc, wrapDst);
			
			wrapSrc.compact();
		}
		catch(Exception e)
		{
			conn.close();
			
			return false;
		}
		
		switch (wrapResult.getStatus())
		{
			case OK:
				if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("encrypt ok");
				if (wrapDst.position() > 0)
				{
					wrapDst.flip();
					this.onOutboundData(wrapDst);
					wrapDst.compact();
				}
				
				break;

			case BUFFER_UNDERFLOW:
				if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("encrypt underflow");
				break;

			case BUFFER_OVERFLOW:
				if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("encrypt overflow");
				conn.close();
				return false;
				
			case CLOSED:
				conn.close();
				return false;
		}
		
		return true;
	}
	
	// 20170716
	public boolean decrypt() throws Exception
	{
		SSLEngineResult unwrapResult;
		
		try
		{
			unwrapSrc.flip();
			
			unwrapResult = sslEngine.unwrap(unwrapSrc, unwrapDst);
			
			unwrapSrc.compact();
		}
		catch(Exception e)
		{
			conn.close();
			
			return false;
		}
		
		switch (unwrapResult.getStatus())
		{
			case OK:
				if (unwrapDst.position() > 0)
				{
					unwrapDst.flip();
					this.onInboundData(unwrapDst);
					unwrapDst.compact();
				}
				
				break;
				
			case CLOSED:
				conn.close();
				return false;

			case BUFFER_OVERFLOW:
				if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("decrypt overflow");
				return false;

			case BUFFER_UNDERFLOW:
				if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("decrypt underflow");
				return false;
		}
		
		return true;
	}
	
	public void onHandshakeSuccess()
	{
		if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("OnHandshakingSuccess");
	}
	
	public void putSendArray(ByteBuffer bb)			// send blocking situation, 20170331
	{
		byte[] b = new byte[bb.remaining()];
		
		bb.get(b);
		
		Test.addSSLSendItem();
		
		sendArray.add(b);
	}
	
	public void sendNext()
	{
		if ( isRemoved ) return;
		if ( sendBuffer == null ) return;
		
		if ( sendBuffer.hasRemaining() )
		{
			try
			{
				int size = sc.write(sendBuffer);	// 20170427
				
				if ( size < 0 )
				{
					conn.close();
				}
			}
			catch(Exception e)
			{
				conn.close();
			}
		}
		else if ( sendArray.size() > 0 )
		{
			sendBuffer.clear();
			
			Test.removeSSLSendItem();
			sendBuffer.put(sendArray.poll());// 20170426
			sendBuffer.flip();
			
			try
			{
				int sndSize = sc.write(sendBuffer);
				
				conn.resetTimer();
				
				if ( sndSize < 0 )
				{
					conn.close();
				}
			}
			catch(Exception e)
			{
				conn.close();
			}
		}
	}
	
	public void onOutboundData(ByteBuffer encrypted)
	{
		try
		{
			conn.resetTimer();
			
			putSendArray(encrypted);
		}
		catch(Exception e)
		{
			conn.close();
		}
	}
	
	public void onInboundData(ByteBuffer decrypted)
	{
		if ( isRegisted )
		{
			int rcv = decrypted.remaining();
			
			byte[] dst = new byte[decrypted.remaining()];
			decrypted.get(dst);
			
			queue.receive(skey, dst, rcv);
		}
		else
		{
			if ( lastRcv != null )
			{
				lastRcv.put(decrypted);
			}
		}
	}
	
	public byte[] getReceiveDecryptedByte()
	{
		if ( lastRcv == null ) return null;
		
		lastRcv.flip();
		
		byte[] dst = new byte[lastRcv.remaining()];
		lastRcv.get(dst);
		
		lastRcv.clear();
		
		return dst;
	}
	
	public void onClosed()
	{
		if ( isRemoved ) return;
		isRemoved = true;
		
		for ( int i = 0 ; i < sendArray.size() ; i++ )
		{
			Test.removeSSLSendItem();
		}
		
		mySender.removeProcessor(this);
		
		Test.addSSLRequest();
		
		try
		{
			System.out.println("SSLProcess onClosed -> closeInbound");
			closeInbound();
		}
		catch(Exception e) { e.printStackTrace(); }
		
		try
		{
			closeOutbound();
			System.out.println("SSLProcess onClosed -> closeOutbound");
		}
		catch(Exception e) { e.printStackTrace(); }
		
		try
		{
			if ( sc != null ) 
			{
				sc.close();
				System.out.println("SSLProcess sc close");
			}
			else
			{
				System.out.println("SSLProcess sc null");
			}

			sc = null;
		}
		catch(Exception e) 
		{
			e.printStackTrace();
		}
		
		try
		{
			if ( skey != null ) 
			{
				skey.cancel();
				System.out.println("SSLProcess skey cancel");
			}
			else System.out.println("SSLProcess skey null");

			skey = null;
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		
		if ( wrapSrc != null ) bbPool.putAppBuffer(wrapSrc);
		if ( unwrapDst != null ) bbPool.putAppBuffer(unwrapDst);
		if ( wrapDst != null ) bbPool.putNetBuffer(wrapDst);
		if ( unwrapSrc != null ) bbPool.putNetBuffer(unwrapSrc);
		
		wrapSrc = null;
		unwrapSrc = null;
		wrapDst = null;
		unwrapDst = null;
		
		Test.removeSSLRequest();
		Test.removeSSL();
	}
	
	public int getNetBufferSize()
	{
		return sslSession.getPacketBufferSize();
	}
	
	public int getAppBufferSize()
	{
		return sslSession.getApplicationBufferSize();
	}
	
	private void doTask()
	{
		Runnable task;
		
		if((task = sslEngine.getDelegatedTask()) != null)
		{
			if ( useSSLDebug && conn.getIP().equals(debugClient) ) System.out.println("run task");
			task.run();
		}
	}
	
	public void closeInbound() throws SSLException
	{
		sslEngine.closeInbound();
	}
	
	public void closeOutbound()
	{
		sslEngine.closeOutbound();
	}
}
