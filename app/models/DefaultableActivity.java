package models;

import java.util.Map;

public abstract class DefaultableActivity {
	public abstract void populateDefaults();
	
	public abstract void mergeActuals(Map<String, String> m);
	
	public Integer getSafeInt(Map<String, String> m, String s)
	{
		Integer i=null;
		try 
		{
			if (m.containsKey(s))
			{
				i = new Integer(m.get(s));
			}
		}
		catch (Exception e)	{}
		
		return i;
	}
	public Double getSafeDouble(Map<String, String> m, String s)
	{
		Double i=null;
		try 
		{
			if (m.containsKey(s))
			{
				i = new Double(m.get(s));
			}
		}
		catch (Exception e)	{}
		
		return i;
	}	
}
