package CombiEngine.ItemManager.Item;

import java.nio.channels.SelectionKey;

public class ConnectItem extends ItemBase
{
	public ConnectItem(SelectionKey sk)
	{
		this.sk = sk;
		
		type = ItemBase.TYPE_CONNECT;
	}
}