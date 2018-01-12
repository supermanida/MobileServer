package CombiEngine.Selector;

import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SSLSender extends Thread
{
	private ConcurrentLinkedQueue<SSLProcessor> processors;
	
	public SSLSender()
	{
		this.processors = new ConcurrentLinkedQueue<SSLProcessor>();
		
		Test.addSSLSender();
		
		this.start();
	}
	
	public void addProcessor(SSLProcessor processor)	// 20170427
	{
		if ( !processors.contains(processor) )
			processors.add(processor);
	}
	
	public void removeProcessor(SSLProcessor processor)	// 20170427
	{
		processors.remove(processor);
	}
	
	public int getProcessorCount()	// 20170427
	{
		return processors.size();
	}
	
	public void run()
	{
		while ( true )
		{
			Iterator<SSLProcessor> it = processors.iterator();
			
			while ( it.hasNext() )				// 20170427-1
			{
				try
				{
					SSLProcessor p = it.next();
					p.sendNext();
				}
				catch(Exception e) {}
			}
			
			try
			{
				sleep(1);
			}
			catch(InterruptedException ie) {}
		}
	}
}