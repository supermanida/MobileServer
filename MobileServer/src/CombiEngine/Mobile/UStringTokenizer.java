package CombiEngine.Mobile;


import java.util.ArrayList;
//2017-12-13
public class UStringTokenizer
{
	private ArrayList<String> ar;
	private int returnIndex = 0;
	
	public UStringTokenizer(String text, String delemeter)
	{
		ar = new ArrayList<String>();
		
		int found = text.indexOf(delemeter);
		
		while ( found >= 0 )
		{
			ar.add(text.substring(0, found));
			
			text = text.substring(found + delemeter.length());
			
			found = text.indexOf(delemeter);
		}
		
		if ( text.length() > 0 ) ar.add(text);
	}
	
	public String nextToken()
	{
		return ar.get(returnIndex++);
	}
	
	public boolean hasMoreTokens()
	{
		if ( returnIndex < ar.size() ) return true;
		else return false;
	}
	
	public int countTokens()
	{
		return ar.size();
	}
}