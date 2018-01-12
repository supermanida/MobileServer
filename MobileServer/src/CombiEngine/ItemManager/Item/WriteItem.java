package CombiEngine.ItemManager.Item;

import java.nio.channels.SelectionKey;

public class WriteItem extends ItemBase
{
	public WriteItem(SelectionKey sk)
	{
		this.sk = sk;
		
		type = ItemBase.TYPE_SEND;
	}
}