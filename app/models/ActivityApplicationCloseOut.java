package models;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.validation.Valid;

import play.data.validation.ValidationError;
import play.db.jpa.JPA;
import play.i18n.Messages;

/**
 * Model class used to support the partial update of Activity Applications
 * with close out data (either interim close out or final).
 * 
 * This is needed so that the framework validation can be used with the bind from request
 * without having to resubmit the entire ActivityApplication object.
 * 
 * @author david.simpson
 *
 */
public class ActivityApplicationCloseOut {

	protected Long id;
	
	@Valid
	protected List<ActivityLocation> activitylocations;

	protected String interimcloseout;
	
	/**
	 * Gets the id
	 * @return
	 */
	public Long getId() {
		return id;
	}

	/**
	 * Sets the id
	 * @param id
	 */
	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Gets the interim closeout status
	 * @return
	 */
	public String getInterimcloseout() {
		return interimcloseout;
	}

	/**
	 * Sets the interim closeout status
	 * @param interimcloseout
	 */
	public void setInterimcloseout(String interimcloseout) {
		this.interimcloseout = interimcloseout;
	}

	/**
	 * Gets the activity locations
	 * @return
	 */
	public List<ActivityLocation> getActivitylocations() {
		return activitylocations;
	}

	/**
	 * Sets the activity locations
	 * @param activitylocations
	 */
	public void setActivitylocations(List<ActivityLocation> activitylocations) {
		this.activitylocations = activitylocations;
	}
	
	/**
	 * Close out the application
	 * @param id the id of the application
	 * @throws Exception
	 */
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
	 * Custom validation for the ActivityApplicationCloseOut.  Attaches location records to the 
	 * parent ActivityApplication to ensure that the validation dependencies work correctly.
	 * 
	 */
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		//Get hold of the parent activity application, as the detached locations submitted may not be linked...
		ActivityApplication aa = ActivityApplication.findById(this.getId());
		boolean bHasActivity = false;
		try
		{
			if (this.getActivitylocations() != null) {
				//for each activity location, make sure that either no activity has been set, or date(s) entered
				Iterator<ActivityLocation> it = this.getActivitylocations().iterator();
				while (it.hasNext()) {
					ActivityLocation al = it.next();
					//if the location has previously been stored, make sure that the parent activity application
					//matches our current application
					if (al.getId()!=null) {
						ActivityLocation alDb = JPA.em().find(ActivityLocation.class, al.getId());
						if (alDb.getAa() != aa) {
							throw new Exception("Attempt to update ActivityLocation from different ActivityApplication.");
						}
					}
					//Ensure that the location is attached to the parent ActivityApplication entity
					if (al.getAa()==null) {
						al.setAa(aa);
					}
					List<ValidationError> location_errors = al.validate();
					if (location_errors!=null) {
						Iterator<ValidationError> itErr = location_errors.iterator();
						while (itErr.hasNext()) {
							ValidationError err=itErr.next();
							errors.add(new ValidationError("activitylocations[" + this.getActivitylocations().indexOf(al) + "]." + err.key(), err.message()));
						}
					}
				}
				//If the locations and optional dates are valid, we require that there is at least one date value set
				if (errors.isEmpty()) {
					it = this.getActivitylocations().iterator();
					while (it.hasNext()) {
						ActivityLocation al = it.next();
						if ((al.getNo_activity()==null || !al.getNo_activity()) && al.getActivitydates()!=null) {
							if (!al.getActivitydates().isEmpty()) {
								bHasActivity = true;
							}
						}
					}
					if (!bHasActivity) {
						errors.add(new ValidationError("",  Messages.get("validation.appcloseout.noactivity")));
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
