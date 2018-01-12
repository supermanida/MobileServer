package CombiEngine.Mobile;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import CombiEngine.Selector.Connector;

public class InvokeThread extends Thread 
{
	//2017-12-04
	Queue<InvokeObj> InvokeList = null;
	Method m = null;
	Object cls = null;
	class InvokeObj
	{
		
		Object[] objParam = new Object[3];
		
		public InvokeObj(String command, ArrayList<String> param, Connector conn)
		{
			objParam[0] = command;
			objParam[1] = param;
			objParam[2] = conn;
		}
		
	}
	
	public Queue<InvokeObj> getBroadcastList() {
		return InvokeList;
	}

	public void setQueue(String command, ArrayList<String> param, Connector conn) 
	{
		InvokeList.add(new InvokeObj(command,param,conn));
	}

	public InvokeThread (Object cls, Method m)
	{
		this.cls = cls;
		this.m = m;
		InvokeList = new ConcurrentLinkedQueue<InvokeObj>();
		start();
	}
	
	public void run()
	{
		while ( true )
		{
			if(InvokeList.size()>0)
			{
				
				InvokeObj itm = InvokeList.poll();
				if(itm!=null)
				{
					try 
					{
						if(itm.objParam!=null)
							m.invoke(cls, itm.objParam);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			else
			{
				try 
				{
					sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}