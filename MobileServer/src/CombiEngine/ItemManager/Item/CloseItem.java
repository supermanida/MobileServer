package CombiEngine.ItemManager.Item;

import CombiEngine.Selector.Connector;

public class CloseItem extends ItemBase
{
	public CloseItem(Connector conn)
	{
		this.conn = conn;
		
		type = ItemBase.TYPE_DISCONNECT;
	}
}