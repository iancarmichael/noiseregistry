package models;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CheckDateValidator implements ConstraintValidator<CheckDate, Date> {
	private Date dt = null;
	
	@Override
	public void initialize(CheckDate constraintAnnotation) {
		String sEarly = constraintAnnotation.earliest();
		SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
		try
		{
			dt = formatter.parse(sEarly);
		}
		catch (Exception e) {}
    }

	@Override
	public boolean isValid(Date ad, ConstraintValidatorContext cvc) {
		try
		{
			if (ad==null)
				return false;
			
			Calendar cal = Calendar.getInstance();
			cal.setLenient(false);

			cal.setTime(ad);
			
			cal.getTime();
			
			if (dt!=null && ad.before(dt))
			{
				return false;
			}
			
			return true;
		}
		catch (Exception e)
		{
			// an exception means the date isn't valid
		}
		
		return false;
	}

}
