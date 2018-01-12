package CombiEngine.Selector;

import java.util.List;
import java.util.ArrayList;
public class WriteProcessor extends Thread
{
	private	List<Connector> connectorList;
	
	public WriteProcessor(List<Connector> connectorList)
	{
		this.connectorList = connectorList;
		
		this.start();
	}
	
	public void run()
	{
		while ( true )
		{
			ArrayList<Connector> cl = null;
			cl = new ArrayList<Connector>(connectorList);
			for ( int i = 0 ; i < cl.size() ; i++ )
			{
				try
				{
					Connector conn = (Connector)cl.get(i);
					conn.write();
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
			
			try
			{
				sleep(1);
			}
			catch(Exception ee) {}
		}
	}
}
