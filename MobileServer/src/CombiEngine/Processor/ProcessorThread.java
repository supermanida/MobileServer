package CombiEngine.Processor;

import CombiEngine.ItemManager.Item.ItemBase;

import CombiEngine.Selector.Connector;
import CombiEngine.ItemManager.ItemQueue;

public class ProcessorThread extends Thread
{
	private	ItemBase item;
	private	CombiService server;
	private	ItemQueue queue;
	
	private String statusString = "";
	
	public ProcessorThread(CombiService server, ItemQueue queue)
	{
		this.server  = server;
		this.queue   = queue;
		
		this.start();
	}
	
	public synchronized void run()
	{
		while ( true )
		{
			try
			{
				statusString = "pop";
				
				this.item = queue.pop();
				
				if ( this.item != null )
				{
					processItem();
				}
				else
				{
					statusString = "queue is null";
					sleep(5);
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public void setItem(ItemBase item)
	{
		this.item = item;
	}
	
	private void processItem()
	{
		Connector conn = null;
		
		if ( item.type == ItemBase.TYPE_CONNECT )
		{
			conn = (Connector)item.sk.attachment();
			
			if ( conn == null )
			{
				return;
			}
			
			statusString = "connect event";
			
			server.OnConnect(conn);
		}
		else if ( item.type == ItemBase.TYPE_DISCONNECT )
		{
			conn = item.conn;
			System.out.println("[ProcessorThread] TYPE_DISCONNECT1");		
	
			if ( conn == null )
			{
				System.out.println("[ProcessorThread] TYPE_DISCONNECT conn null. return");
				return;
			}
			
			statusString = "disconnect event from " + conn.itemKey;
			
			server.OnClose(item.conn);
			System.out.println("[ProcessorThread] TYPE_DISCONNECT2");
		}
		else if ( item.type == ItemBase.TYPE_RECEIVE )
		{
			conn = (Connector)item.sk.attachment();
			
			if ( conn == null )
			{
				return;
			}
			
			conn.setWorking(true);

			statusString = "receive event from " + conn.itemKey;
			
			server.OnReceive(conn, item.buf, item.size);

			conn.setWorking(false);
		}
		else if ( item.type == ItemBase.TYPE_SEND )
		{
			conn = (Connector)item.sk.attachment();
			
			if ( conn == null )
			{
				return;
			}
			
			statusString = "send event from " + conn.itemKey;
			
			conn.write();
		}
		else
		{
			statusString = "queue is nothing";
		}
	}
	
	public String getStatus()
	{
		String ret = this.toString() + ":" + statusString;
		
		return ret;
	}
}
