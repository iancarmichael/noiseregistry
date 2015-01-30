package models;
import java.util.LinkedHashMap;

import play.i18n.Messages;

public enum ActivityTypes {
	Seismic_Survey(1L),
	Geophysical_Survey(2L),
	Piling(3L),
	Explosives(4L),
	Acoustic_Deterrent_Device(5L),
	Multibeam_Echosounders(6L),
	MoD(7L);
	
	private Long value;
	
	private ActivityTypes(Long i)
	{
		this.value=i;
	}
	public Long toLong() {
		return this.value;
	}
	public static String get(Long l)
	{
		if (l==1L)
			return Messages.get("activity_type.seismic");
		if (l==2L)
			return Messages.get("activity_type.geophysical");
		if (l==3L)
			return Messages.get("activity_type.piling");
		if (l==4L)
			return Messages.get("activity_type.explosives");
		if (l==5L)
			return Messages.get("activity_type.add");
		if (l==6L)
			return Messages.get("activity_type.mbes");
		if (l==7L)
			return Messages.get("activity_type.mod");
		return Messages.get("activity_type.unknown");
	}
	@Override
	public String toString()
	{
		return ActivityTypes.get(this.value);
	}
	
	public static LinkedHashMap<Long, String> getOptions() {
		LinkedHashMap<Long, String> options = new LinkedHashMap<Long, String>();
        for (int i=1; i<=7; i++) {
        	options.put(Long.valueOf(i), get(Long.valueOf(i)));
        }
    	return options;
	}
}