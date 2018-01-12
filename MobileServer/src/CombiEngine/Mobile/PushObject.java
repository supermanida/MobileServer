package CombiEngine.Mobile;

import java.util.concurrent.ConcurrentLinkedQueue;

public class PushObject
{
	String msgId = null;
	
	private ConcurrentLinkedQueue<PushItem> list = null;
	
	public PushObject()
	{
		list = new ConcurrentLinkedQueue<>();
	}
	
	public PushObject(String msgId)
	{
		this.msgId = msgId;
		new PushObject();
	}
	
	public ConcurrentLinkedQueue<PushItem> getList() 
	{
		return list;
	}

	public void setList(ConcurrentLinkedQueue<PushItem> list) 
	{
		this.list = list;
	}
	
	public PushItem remove()
	{
		return list.remove();
	}
	
	public boolean remove(PushItem itm)
	{
		return list.remove(itm);
	}
	
	public boolean remove(String userId)
	{
		return list.remove(userId);
	}
	
	public boolean put(PushItem itm)
	{
		return list.add(itm);
	}
}
