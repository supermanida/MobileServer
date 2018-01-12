package Hub;

import java.io.BufferedOutputStream;

public class SocketRunner extends Thread
{
	BufferedOutputStream os;
	boolean m_bFinished = false;	//  20170412
	
	public SocketRunner(BufferedOutputStream os)
	{
		this.os = os;
		
		start();
	}
	
	@Override
	public void run()
	{
		String nop = "NOOP\f";
		byte[] bnop = nop.getBytes();
		
		try
		{
			while ( !m_bFinished )	//  20170412
			{
				try
				{
					sleep(5000);
				}
				catch(Exception e) {}
				
				os.write(bnop);
				os.flush();
			}
		}
		catch(Exception e) {e.printStackTrace();}
	}
	
	public void finish()	//  20170412
	{
		m_bFinished = true;
	}
}