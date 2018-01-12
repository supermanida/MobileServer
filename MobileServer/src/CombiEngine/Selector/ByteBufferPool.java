package CombiEngine.Selector;

import java.util.concurrent.ConcurrentLinkedQueue;

import java.nio.ByteBuffer;

public class ByteBufferPool
{
	public static ConcurrentLinkedQueue<ByteBuffer> appBuffer;
	public static ConcurrentLinkedQueue<ByteBuffer> netBuffer;
	
	int appBufferSize;
	int netBufferSize;
	
	public ByteBufferPool(int appBufferSize, int netBufferSize)
	{
		this.appBufferSize = appBufferSize;
		this.netBufferSize = netBufferSize;
		
		appBuffer = new ConcurrentLinkedQueue<ByteBuffer>();
		netBuffer = new ConcurrentLinkedQueue<ByteBuffer>();
	}
	
	public ByteBuffer getAppBuffer()
	{
		if ( appBuffer.size() > 0 )
		{
			ByteBuffer buf = appBuffer.poll();
			buf.clear();
			
			return buf;
		}
		else
		{
			return ByteBuffer.allocate(appBufferSize);
		}
	}
	
	public void putAppBuffer(ByteBuffer buf)
	{
		buf.clear();
		
		appBuffer.add(buf);
	}
	
	public ByteBuffer getNetBuffer()
	{
		if ( netBuffer.size() > 0 )
		{
			ByteBuffer buf = netBuffer.poll();
			buf.clear();
			
			return buf;
		}
		else
		{
			return ByteBuffer.allocate(netBufferSize);
		}
	}
	
	public void putNetBuffer(ByteBuffer buf)
	{
		buf.clear();
		
		netBuffer.add(buf);
	}
}