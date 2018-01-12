package CombiEngine.Mobile;

public class Closer 
{
	//2017-12-04
	public static void close(AutoCloseable closer)
	{
		try {
			closer.close();
			closer = null;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
