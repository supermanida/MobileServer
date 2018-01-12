package CombiEngine.FileAccess;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class CombiFileWriter
{
	private	FileOutputStream fo = null;
	private	FileChannel fc = null;
	
	File f;
	
	public CombiFileWriter(File f) throws Exception
	{
		this.f = f;
		fo = new FileOutputStream(f);
		
		fc = fo.getChannel();
	}
	
	public int write(ByteBuffer buf) throws Exception
	{
		return fc.write(buf);
	}
	
	public void close() throws Exception
	{
		if ( fo != null )
		{
			try
			{
				fo.close();
				fo = null;
			}
			catch(Exception e) {}
		}
		
		if ( fc != null )
		{
			try
			{
				fc.close();
				fc = null;
			}
			catch(Exception e) {}
		}
	}
}
