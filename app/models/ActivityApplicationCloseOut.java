package models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.validation.Valid;

import play.data.validation.ValidationError;
import play.i18n.Messages;

public class ActivityApplicationCloseOut {

	@Valid
	protected List<ActivityLocation> pa;

	protected String interimcloseout;
	
	public String getInterimcloseout() {
		return interimcloseout;
	}

	public void setInterimcloseout(String interimcloseout) {
		this.interimcloseout = interimcloseout;
	}

	public List<ActivityLocation> getPa() {
		return pa;
	}

	public void setPa(List<ActivityLocation> pa) {
		this.pa = pa;
	}
	
	public void closeOut(Long id) throws Exception {
		//Perform the close out operation
		if (this.getInterimcloseout()!=null) {
			ActivityApplication.closeOut(this, id, true);
		} else {
			ActivityApplication.closeOut(this, id, false);
		}
	}

	/**
	 * validate
	 * 
	 * Custom validation for the ActivityApplicationCloseOut.
	 */
	 
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		try
		{
			if (this.getPa() != null) {
				//for each activity location, make sure that either no activity has been set, or date(s) entered
				Iterator<ActivityLocation> it = this.getPa().iterator();
				while (it.hasNext()) {
					ActivityLocation al = it.next();
					List<ValidationError> location_errors = al.validate();
					if (location_errors!=null) {
						Iterator<ValidationError> itErr = location_errors.iterator();
						while (itErr.hasNext()) {
							ValidationError err=itErr.next();
							errors.add(new ValidationError("pa[" + this.getPa().indexOf(al) + "]." + err.key(), err.message()));
						}
					}
				}
			}
		} 
		catch (Exception e)
		{
			e.printStackTrace();
			errors.add(new ValidationError("",  Messages.get("error.something.went.wrong")));
		}
		
		return errors.isEmpty() ? null : errors;
		
    }
	
}
