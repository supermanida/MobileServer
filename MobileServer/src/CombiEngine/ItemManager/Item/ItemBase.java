package CombiEngine.ItemManager.Item;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;

import CombiEngine.Selector.Connector;

public class ItemBase
{
	public	static char	TYPE_CONNECT	= 'a';
	public	static char	TYPE_DISCONNECT	= 'd';
	public	static char	TYPE_RECEIVE	= 'r';
	public	static char	TYPE_SEND	= 'w';
	
	public	SelectionKey	sk	= null;
	public	SocketChannel	sc	= null;
	public	byte[]		buf	= null;
	public	Connector	conn	= null;
	public	int		size;
	public	char		type;
}