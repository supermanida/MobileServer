package CombiEngine.ItemManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.nio.channels.SelectionKey;

import CombiEngine.ItemManager.Item.ItemBase;
import CombiEngine.ItemManager.Item.ConnectItem;
import CombiEngine.ItemManager.Item.ReadItem;
import CombiEngine.ItemManager.Item.CloseItem;

import CombiEngine.Selector.Connector;

public class ItemQueue
{
	private	ArrayList <ItemBase> itemQueue = null;
	
	public int recvCount_put = 0;
	public int closCount_put = 0;
	
	public int recvCount_get = 0;
	public int closCount_get = 0;
	
	public ItemQueue()
	{
		itemQueue = new ArrayList <ItemBase> ();
	}
	
	public void connect(SelectionKey sk)
	{
		put(new ConnectItem(sk));
	}
	
	public void disconnect(Connector conn)
	{
		closCount_put++;
		
		put(new CloseItem(conn));
	}
	
	public void receive(SelectionKey sk, byte[] buf, int size)
	{
		recvCount_put++;
		
		put(new ReadItem(sk, buf, size));
	}
	
	private synchronized void put(ItemBase item)
	{
		if ( item.type == ItemBase.TYPE_RECEIVE && item.sk != null && item.sk.attachment() != null )
			((Connector)item.sk.attachment()).readItemCount++;
		
		itemQueue.add(item);
	}
	
	public synchronized ItemBase pop() throws Exception
	{
		if ( itemQueue.size() == 0 ) return null;
		
		int i = 0;
		ItemBase ib = null;

		ib = itemQueue.remove(i);

		while ( ib.type == ItemBase.TYPE_RECEIVE && ((Connector)ib.sk.attachment()).getWorking() == true )
		{
			itemQueue.add(i, ib);

			i++;
			if ( i >= itemQueue.size() )
			{
				return null;
			}
			else
			{
				ib = itemQueue.remove(i);
			}
		}

		if ( ib.type == ItemBase.TYPE_RECEIVE )
		{
			((Connector)ib.sk.attachment()).readItemCount--;
			recvCount_get++;
		}
		else if ( ib.type == ItemBase.TYPE_DISCONNECT )
		{
			closCount_get++;
		}
		
		return ib;
	}
	
	public synchronized void clearItem(SelectionKey skey)
	{
		Iterator<ItemBase> it = itemQueue.iterator();
		
		while ( it.hasNext() )
		{
			ItemBase i = (ItemBase)it.next();
			
			if ( i.type == ItemBase.TYPE_RECEIVE )
			{
				it.remove();
			}
		}
	}
	
	public synchronized int size()
	{
		return itemQueue.size();
	}
	
	public synchronized int count(SelectionKey skey)
	{
		if ( skey != null && skey.attachment() != null )
			return ((Connector)skey.attachment()).readItemCount;
		
		return 0;
	}
	
	public synchronized int count(Connector conn)
	{
		if ( conn != null )
			return conn.readItemCount;
		
		return 0;
	}
	
	public int[] getStatus()
	{
		int[] ret = new int[5];
		
		ret[0] = recvCount_put;
		ret[1] = closCount_put;
		ret[2] = recvCount_get;
		ret[3] = closCount_get;
		ret[4] = itemQueue.size();
		
		recvCount_put = 0;
		closCount_put = 0;
		recvCount_get = 0;
		closCount_get = 0;
		
		return ret;
	}
}
