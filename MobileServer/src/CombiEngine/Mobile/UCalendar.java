package CombiEngine.Mobile;


import java.util.Calendar;
//2017-12-13
public class UCalendar extends Thread
{
	static UCalendar uCalendar = null;
	
	Calendar cal;
	
	public static UCalendar Instance()
	{
		if ( uCalendar == null ) uCalendar = new UCalendar();
		
		return uCalendar;
	}
	
	public UCalendar()
	{
		cal = Calendar.getInstance();
		
		this.start();
	}
	
	public void run()
	{
		//System.out.println("Start");
		
		int resetCount = 0;
		
		while ( true )
		{
			try
			{
				sleep(10);
			}
			catch(InterruptedException e) {}
			
			resetCount++;
			
			if ( resetCount >= 1000 )
			{
				cal = Calendar.getInstance();
			}
			else
			{
				cal.add(Calendar.MILLISECOND, 10);
			}
		}
	}
	
	public static Calendar getInstance()
	{
		return UCalendar.Instance().cal;
	}
}