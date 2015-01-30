package models;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import play.Logger;
import play.data.format.Formats;
import play.data.validation.Constraints.Required;
import play.data.validation.ValidationError;
import play.db.jpa.JPA;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="activitylocationdate")
//@CheckActivityLocationDate 		//custom class validator (problems with stack overflow exceptions?) 
public class ActivityLocationDate {

	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "activitylocationdate_seq_gen")
    @SequenceGenerator(name = "activitylocationdate_seq_gen", sequenceName = "activitylocationdate_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;   
	
	@JsonIgnore
	@ManyToOne(optional=false)
    @JoinColumn(name="activitylocation_id", referencedColumnName="id")
	protected ActivityLocation activitylocation;
	
	@Formats.DateTime(pattern="yyyy-MM-dd")
    @Temporal (TemporalType.DATE)
	@Required
	protected Date activity_date;
	
	
	public ActivityLocation getActivitylocation() {
		return activitylocation;
	}
	public void setActivitylocation(ActivityLocation activitylocation) {
		this.activitylocation = activitylocation;
	}
	public Date getActivity_date() {
		return activity_date;
	}
	public void setActivity_date(Date activity_date) {
		this.activity_date = activity_date;
	}
	
	/**
	 * Custom validation for ActivityLocationDates.
	 * 
	 * If date values are provided, they must fall within the range specified by the parent activity application.
	 * 
	 * @return List of validation failures or null (for success)
	 */
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		Logger.error("ActivityLocationDate validate method()");
		
		// Work around a problem where the call to getActivityApplication on the parent location record
		// returns an empty entity record...
		ActivityApplication aa = this.getActivitylocation().getAa();
	    if (aa != null) {
	    	if (aa.getId() != null && aa.getDate_start()==null) {
	    		aa = JPA.em().find(ActivityApplication.class, aa.getId());
	    	}
	    }
	    
		if (aa.getDate_start() != null) {
			if (this.getActivity_date().before(aa.getDate_start())) {
				errors.add(new ValidationError("activity_date", "validation.activitybeforestart"));
			}
		}
		if (aa.getDate_end() != null) {
			if (this.getActivity_date().after(aa.getDate_end())) {
				errors.add(new ValidationError("activity_date", "validation.activityafterend"));
			}
		}
		return errors.isEmpty() ? null : errors;
	}
}
