package models;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;

import javax.validation.Valid;

import org.hibernate.annotations.Type;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.Range;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.Point;

import play.Logger;
import play.data.validation.ValidationError;
import play.db.jpa.JPA;
import play.i18n.Messages;

@Entity
@Table(name="activitylocation")
//@CheckActivityLocation 			//custom class validator (stack overflow exception problems?)
public class ActivityLocation
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "activitylocation_seq_gen")
    @SequenceGenerator(name = "activitylocation_seq_gen", sequenceName = "activitylocation_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;   
    
    @JsonIgnore
    @ManyToOne(optional=false)
    @JoinColumn(name="activityapplication_id",referencedColumnName="id")
    protected ActivityApplication aa;
    
    protected String entered_ogb_code;
   
    @Range(max=64, min=48, message="validation.invalidrange")
	protected Double lat;
	
    @Range(max=4, min=-24, message="validation.invalidrange")
    protected Double lng;
	
    @JsonIgnore
    @Type(type = "org.hibernate.spatial.GeometryType")
  	protected Point entered_point;     
    
    @JsonIgnore
    @Type(type = "org.hibernate.spatial.GeometryType")  
	protected Polygon entered_polygon;
    
    @Column(length=10)
	@NotBlank(message="validation.required")
    protected String creation_type;
    
    @Column(columnDefinition = "BIT", length = 1)
    protected Boolean no_activity;
    
    @JsonIgnore
	@Transient
	protected String entrytype;
	
	@JsonIgnore
	@Transient
	protected String activitydate;
	
	//@Transient
	//protected String wktgeom;
	
	@JsonIgnore
	@Transient
	protected String description;
	
	@JsonManagedReference("activitylocation-dates")
	@OneToMany(mappedBy="activitylocation",targetEntity=ActivityLocationDate.class)
	@Valid
	protected List<ActivityLocationDate> activitydates;
	
	/*
	 * Getters/ Setters...
	 */
	public String getActivitydate() {
		if (this.activitydate==null) {
			if (this.getActivitydates()!=null) {
				SimpleDateFormat sdf = new SimpleDateFormat("d/M/y");
				Iterator<ActivityLocationDate> it = this.getActivitydates().iterator();
				StringBuffer result = new StringBuffer();
				while (it.hasNext()) {
					String dtVal = sdf.format(it.next().getActivity_date());
					if(result.length()>0) {
						result.append(",");
					}
					result.append(dtVal);
				}
				if (result.length()>0) {
					this.activitydate = result.toString(); 
				}
			}
		}
		return activitydate;
	}
	//Setting the activity date field also populates the list of activitydates for this item...
	public void setActivitydate(String activitydate) {
		try {
			this.activitydate = activitydate;
			if (!activitydate.equals("")) {
				
				String dates[] = this.getActivitydate().split(",");
				List<ActivityLocationDate> dateList = new ArrayList<ActivityLocationDate>();
				for (int i=0; i<dates.length; i++) {
					ActivityLocationDate ald = new ActivityLocationDate();
					//convert the string to date
					//@Note this is the same format as the date-picker uses.
					SimpleDateFormat sdf = new SimpleDateFormat("d/M/y");
					
					Date dt = null;
					try {
						dt = sdf.parse(dates[i].trim());
						ald.setActivity_date(dt);
						ald.setActivitylocation(this);
						dateList.add(ald);
					} catch (java.text.ParseException e) {
						//if the date is not valid flag it as an error?
						
					}
				}
				this.setActivitydates(dateList);
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
		}
	}
	
	public String getEntered_ogb_code() {
		return entered_ogb_code;
	}
	public void setEntered_ogb_code(String entered_ogb_code) {
		this.entered_ogb_code = entered_ogb_code;
	}
	public Point getEntered_point() {
		return entered_point;
	}
	public void setEntered_point(Point entered_point) {
		this.entered_point = entered_point;
	}
	public Polygon getEntered_polygon() {
		return entered_polygon;
	}
	public void setEntered_polygon(Polygon entered_polygon) {
		this.entered_polygon = entered_polygon;
	}
	public String getCreation_type() {
		return creation_type;
	}
	public void setCreation_type(String creation_type) {
		this.creation_type = creation_type;
	}
	public Boolean getNo_activity() {
		return no_activity;
	}
	public void setNo_activity(Boolean no_activity) {
		this.no_activity = no_activity;
	}
	
	public Double getLat() {
		return lat;
	}
	public void setLat(Double lat) {
		this.lat = lat;
	}
	public Double getLng() {
		return lng;
	}
	public void setLng(Double lng) {
		this.lng = lng;
	}
	
    public List<ActivityLocationDate> getActivitydates() {
		return activitydates;
	}
	public void setActivitydates(List<ActivityLocationDate> activitydates) {
		this.activitydates = activitydates;
	}
	public String getEntrytype() {
		return entrytype;
	}
	public void setEntrytype(String entrytype) {
		this.entrytype = entrytype;
	}
	
	public String getDescription() {
		StringBuffer res = new StringBuffer("");
		if (!(this.getLat()==null)) {
			res.append(Messages.get("location.type.latlng"));
			res.append(" ");
			res.append(Double.toString(this.getLat()));
			res.append(" / ");
			res.append(Double.toString(this.getLng()));
		} else if (!this.getEntered_ogb_code().equals("")) {
			res.append(Messages.get("location.type.ogb"));
			res.append(" ");
			res.append(this.getEntered_ogb_code());
		}
		return res.toString();
	}
	public void setDescription(String description) {
		this.description = description;
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public ActivityApplication getAa() {
		return aa;
	}
	public void setAa(ActivityApplication aa) {
		this.aa = aa;
	}	
    
    /**
     * Update this activity location.
     */
    public void update() {
    	//this.saveTheDates();
    	if (this.getId()==null) {
    		JPA.em().persist(this);
    	} else {
    		//When we're updating, we need to remove any existing date info from the
    		//database and add the new values.  This is due to the way that date entry 
    		//is handled in the form, with a single field.
    		//Only if the current entity is not managed.
    		if (!JPA.em().contains(this)) { 
	    		ActivityLocation al = JPA.em().find(ActivityLocation.class, this.getId());
	    		if (al.getActivitydates()!=null) {
	    			Iterator<ActivityLocationDate> it = al.getActivitydates().iterator();
	    			while (it.hasNext()) {
	    				JPA.em().remove(it.next());
	    			}
	    		}
    		}
    		JPA.em().merge(this);
    	}
    	//Add the date values created from the form.
    	if (this.getActivitydates() != null && (this.getNo_activity()==null || !this.getNo_activity())) {
        	Iterator<ActivityLocationDate> it = this.getActivitydates().iterator();
	        while(it.hasNext()) {
	        	ActivityLocationDate ad = it.next();
	        	if (ad.getActivity_date()!=null) {
	        		ad.setActivitylocation(this);
	        		JPA.em().merge(ad);
	        	}
	        }
        }
    }
	
    /**
	 * validate
	 * 
	 * Custom validation for the ActivityLocation entity.
	 * 
	 */
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		//Logger.error("ActivityLocation validate method()");
		try
		{
			ActivityApplication aa = this.getAa();
			if (aa != null) {
		    	if (aa.getId() != null && aa.getStatus()==null) {
		    		aa = JPA.em().find(ActivityApplication.class, aa.getId());
		    	}
		    }
			
			if (this.getEntered_ogb_code()!=null && !this.getEntered_ogb_code().equals("")) {
	    		//Block code
	    		//Logger.error("Entered code: " + this.getEntered_ogb_code());
	    		List<OilAndGasBlock> blocks = OilAndGasBlock.findByCode(this.getEntered_ogb_code());
	    		if (blocks.isEmpty()) {
	    			errors.add(new ValidationError("entered_ogb_code",  Messages.get("validation.entered_ogb_code.invalid")));
	    		}
			}
			
			if (aa!=null && aa.isSavingAsClosed()) {
				//for closing, there must be dates or this item must be marked as no activity...
				if (this.getCreation_type().equals("Proposed") && (this.getNo_activity() == null)) {
					errors.add(new ValidationError("no_activity",  Messages.get("error.required")));
				}
				if (this.getNo_activity() != null && !this.getNo_activity()) {
					if (this.getActivitydates() == null || this.getActivitydates().isEmpty()) {
						errors.add(new ValidationError("activitydate", "validation.appcloseout.datesrequired"));
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
