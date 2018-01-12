package CombiEngine.FileAccess;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class CombiFileReader
{
	private	FileInputStream fi = null;
	private	FileChannel fc = null;

	int zero_count = 0;

	public CombiFileReader(File f) throws Exception
	{
		fi = new FileInputStream(f);

		fc = fi.getChannel();
	}

	public int read(ByteBuffer buf) throws Exception
	{
		int rcvSize = fc.read(buf);
		if ( rcvSize == 0 ) zero_count++;
		else zero_count = 0;
		if ( zero_count > 100 ) return -1;
		else return rcvSize;
	}

	public void close()
	{
		if ( fi != null )
		{
			try
			{
				fi.close();
				fi = null;
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
