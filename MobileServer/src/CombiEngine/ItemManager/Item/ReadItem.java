package CombiEngine.ItemManager.Item;

import java.nio.channels.SelectionKey;

public class ReadItem extends ItemBase
{
	public ReadItem(SelectionKey sk, byte[] buf, int size)
	{
		this.sk = sk;
		this.buf = buf;
		this.size = size;
		
		type = ItemBase.TYPE_RECEIVE;
	}
}