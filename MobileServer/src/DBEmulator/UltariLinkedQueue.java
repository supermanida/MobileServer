package DBEmulator;

import java.lang.reflect.Field;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

@SuppressWarnings({ "rawtypes", "serial"})
public class UltariLinkedQueue extends ConcurrentLinkedQueue
{
	public boolean has(String key, String str) throws Exception
	{
		Iterator it = this.iterator();
		while(it.hasNext())
		{
			Object obj = it.next();
			Class cls = obj.getClass();
			Field f = cls.getField(key);
			if(f.get(obj).equals(str))
				return true;
		}
		return false;
	}
	
	@Override
	public boolean add(Object obj)
	{
		Class cls = obj.getClass();
		Field f;
		try 
		{
			f = cls.getField("id");
			if(!has("id",(String)f.get(obj)))
			{
				return super.add(obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 		
		
		return false;
	}
	
	@Override
	public boolean remove(Object obj)
	{
		Class cls = obj.getClass();
		Field f;
		try 
		{
			f = cls.getField("id");
			if(!has("id",(String)f.get(obj)))
			{
				return super.remove(obj);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} 
		return false;
	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args)
	{
		UltariLinkedQueue queue = new UltariLinkedQueue();
		PartObject prt = new PartObject("test", "0", "test2", "000000", "tst");
		queue.add(prt);
		try 
		{
			System.out.println(queue.has("id", "start"));
			System.out.println(queue.has("id", "test"));
		} catch (Exception e) {
			e.printStackTrace();
		}

	}
}
