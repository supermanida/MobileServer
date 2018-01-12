package CombiEngine.Processor;

import CombiEngine.Selector.Connector;

public class CombiService
{
	public void OnConnect(Connector conn) {}
	
	public void OnClose(Connector conn) {}
	
	public void OnReceive(Connector conn, byte[] buf, int size) {}
}