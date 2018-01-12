package CombiEngine.Mobile;

import java.io.File;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.Calendar; //2016-09-14

public class ManagementFiles extends Thread
{
	protected String path;
	protected int lineCount;
	protected int terminate; //2016-09-14

	//2016-09-14
	//public ManagementFiles(String path, int lineCount)
	public ManagementFiles(String path, int lineCount, int term)
	{
		this.path = path;
		this.lineCount = lineCount;
		this.terminate = term; //2016-09-14

		this.start();
	}
	
	public void run()
	{
		while ( true )
		{
			processFile(new File(path));
			
			try
			{
				sleep(1000 * 3600);
				//sleep(10000);
			}
			catch(InterruptedException e) {}
		}
	}
	
	public void processFile(File f)
	{
		File[] fl = f.listFiles();
		if(fl != null)
		{
			for ( int i = 0 ; i < fl.length ; i++ )
			{
				if ( fl[i].isDirectory() )
					processFile(fl[i]);
				else
					checkAndRemoveFile(fl[i]);
			}
		}
	}
	
	public void checkAndRemoveFile(File f)
	{
		//2016-09-14
		Calendar now = Calendar.getInstance();
		Calendar fcd = Calendar.getInstance();
		fcd.setTimeInMillis(f.lastModified());
		fcd.add(Calendar.DATE, terminate);

		if ( fcd.before(now) )
		{
			System.out.println("[DeleteReserved] file delete Info. Name:"+f.getName() + ", modif:"+f.lastModified());
			//f.delete();
		}
		else
			System.out.println("[DeleteReserved] file not delete. Name:"+f.getName() + ", modif:"+f.lastModified());
		//

		/*
		ArrayList<String> lines = new ArrayList<String>();
		
		FileReader fr = null;
		BufferedReader br = null;
		
		try
		{
			fr = new FileReader(f);
			br = new BufferedReader(fr);
			
			String line = null;
			
			while ( ( line = br.readLine() ) != null )
			{
				lines.add(line);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if ( fr != null ) { try { fr.close(); fr = null; } catch(Exception e) {} }
			if ( br != null ) { try { br.close(); br = null; } catch(Exception e) {} }
		}
		
		while ( lines.size() > lineCount )
		{
			lines.remove(0);
		}
		
		FileWriter fw = null;
		
		try
		{
			fw = new FileWriter(f);
			
			for ( int i = 0 ; i < lines.size() ; i++ )
			{
				if ( fieldCount(lines.get(i)) >= 6 )
				{
					fw.write(lines.get(i) + "\n");
					fw.flush();
				}
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			if ( fw != null ) { try { fw.close(); fw = null; } catch(Exception e) {} }
		}
		*/
	}
	
	private int fieldCount(String s)
	{
		int cnt = 0;
		
		for ( int i = 0 ; i < s.length() ; i++ )
		{
			if(i == (s.length()-1))
				cnt++;

                        if ( s.charAt(i) == '\t')
                                cnt++;			

			/*
			if ( s.charAt(i) == '\t' || i == ( s.length() - 1 ) )
			{
				cnt++;
			}
			*/
		}
		
		return cnt;
	}
}
