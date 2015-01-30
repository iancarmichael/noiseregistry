package models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.FilterDefs;
import org.hibernate.annotations.ParamDef;
import org.hibernate.annotations.Filters;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonManagedReference;

import play.Logger;
import play.data.format.*;
import play.data.validation.*;
import play.db.jpa.*;
import play.i18n.Messages;

/**
 * Named queries for ActivityApplication functions.  The names are scoped at the persistence unit
 * level, hence the use of the entity class name as a prefix. 
 *
 */
@NamedQueries({
	@NamedQuery(name = "ActivityApplication.findAll", query = "from ActivityApplication" ),
	@NamedQuery(name = "ActivityApplication.findById", query = "from ActivityApplication where id=:id)"),
	@NamedQuery(name = "ActivityApplication.findLateByRegulator", query = "from ActivityApplication where regulator=:reg and status!='Closed' and date_due < now()"),
	@NamedQuery(name = "ActivityApplication.findLinkedByParent", query = "from ActivityApplication where parent=:parent order by id"),
	@NamedQuery(name = "ActivityApplication.findApplicationsByStatus", query = "from ActivityApplication where status in (:status) and (noiseproducer.organisation.id = :orgid or regulator.organisation.id = :orgid) order by date_start desc")
})

/**
 * FilterDefs apply across the package - they are defined here as this is the first entity
 * where row level access is required (to prevent users from seeing other activity applications
 * by simply changing the id in the address bar of the browser).
 * 
 */
@FilterDefs( {
	@FilterDef(name="myNoiseProducers", parameters=@ParamDef( name="np_id", type="long" ) ),
	@FilterDef(name="myRegulators", parameters=@ParamDef( name="reg_id", type="long" ) )
})

@Entity
@Filters( {
	@Filter(name="myNoiseProducers", condition="noiseproducer_id in (:np_id)"),
	@Filter(name="myRegulators", condition="regulator_id in (:reg_id) and status!='Draft'")
})
@Table(name="activityapplication")
@CheckActivityApplication
public class ActivityApplication
{
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "activityapplication_seq_gen")
    @SequenceGenerator(name = "activityapplication_seq_gen", sequenceName = "activityapplication_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;
    
	@ManyToOne(optional=true)
    @JoinColumn(name="parent_id")
	protected ActivityApplication parent;
		
    @ManyToOne(optional=false)
    @JoinColumn(name="noiseproducer_id")
    @Valid
    @NotNull(message="validation.required")
    protected NoiseProducer noiseproducer;
    
    //@ManyToOne(optional=false)
    //@JoinColumn(name="activitytype_id")
    //private ActivityType act_type;    
    
    @NotNull(message="validation.required")
    protected Long activitytype_id;
    
    @ManyToOne(optional=false)
    @JoinColumn(name="regulator_id")    
    @Valid
    @NotNull(message="validation.required")
    protected Regulator regulator;
    
    @Column(length=10)
    protected String status;
    
    @NotNull(message="validation.required")
    protected Boolean non_licensable;

	public Boolean getNon_licensable() {
		return non_licensable;
	}
	public void setNon_licensable(Boolean non_licensable) {
		this.non_licensable = non_licensable;
	}

	@Formats.DateTime(pattern="yyyy-MM-dd")
    @Temporal (TemporalType.DATE)
	@CheckDate(message="validation.not.valid.start",earliest="2000-01-01")
    private Date date_start;
    
    @Transient
    @JsonIgnore
    private Integer date_start_day;
    
    @Transient
    @JsonIgnore
    private Integer date_start_month;
    
    @Transient
    @JsonIgnore
    private Integer date_start_year;
    
	@Formats.DateTime(pattern="yyyy-MM-dd")
    @Temporal (TemporalType.DATE)
	@CheckDate(message="validation.not.valid.end")
    protected Date date_end;	

	@Transient
    @JsonIgnore
    private Integer date_end_day;
    
    @Transient
    @JsonIgnore
    private Integer date_end_month;
    
    @Transient
    @JsonIgnore
    private Integer date_end_year;
	
    @Formats.DateTime(pattern="yyyy-MM-dd")
    @Temporal (TemporalType.DATE)
    protected Date date_due;
    
    @Transient
    @JsonIgnore
    private Integer date_due_day;
    
    @Transient
    @JsonIgnore
    private Integer date_due_month;
    
    @Transient
    @JsonIgnore
    private Integer date_due_year;
    
    @Formats.DateTime(pattern="yyyy-MM-dd")
    @Temporal (TemporalType.DATE)
	//@CheckDate(message="validation.not.valid.closed")
    protected Date date_closed;
    
    @Transient
    @JsonIgnore
    private Integer date_closed_day;
    
    @Transient
    @JsonIgnore
    private Integer date_closed_month;
    
    @Transient
    @JsonIgnore
    private Integer date_closed_year;

    @JsonIgnore
	@Column(columnDefinition="TIMESTAMP DEFAULT CURRENT_TIMESTAMP")
    protected Timestamp date_updated;
    
    @Transient
    @JsonIgnore
    protected String saveasdraft;
    
    @Transient
    @JsonIgnore
    protected boolean closing=false;  //flag to indicate that the item is being processed for closing - used for validation
    
    @PreUpdate
    @PrePersist
    public void updateTimeStamps() {
    	date_updated = new Timestamp(new java.util.Date().getTime());
    }
    
    @NotNull(message="validation.required")
    @Min(value=1, message="validation.min")
    protected Integer duration;

    
    /**
     * The ActivityXXX entities need to have CascadeType set to ALL to allow the
     * orphan removal setting to apply correctly.  This will remove entries from the 
     * database if the reference is removed, which happens if the user edits the 
     * activity application and changes the activity type in the form.
     */
    @JsonManagedReference("activity-activityseismic")
    @OneToOne(mappedBy="aa",
    			targetEntity=ActivitySeismic.class,
    			fetch=FetchType.LAZY,
    			optional=true,
    			orphanRemoval=true,
		    	cascade = {CascadeType.ALL})
    @Valid
    protected ActivitySeismic activitySeismic;
    

	public ActivitySeismic getActivitySeismic() {
		return activitySeismic;
	}
	public void setActivitySeismic(ActivitySeismic activitySeismic) {
		this.activitySeismic = activitySeismic;
	}

	@JsonManagedReference("activity-activitygeophysical")
    @OneToOne(mappedBy="aa",
    			targetEntity=ActivityGeophysical.class,
    			fetch=FetchType.LAZY,
    			optional=true,
    			orphanRemoval=true,
    			cascade = {CascadeType.ALL})
	@Valid
    protected ActivityGeophysical activityGeophysical;
    

	public ActivityGeophysical getActivityGeophysical() {
		return activityGeophysical;
	}
	public void setActivityGeophysical(ActivityGeophysical activityGeophysical) {
		this.activityGeophysical = activityGeophysical;
	}

	@JsonManagedReference("activity-activityacousticdd")
    @OneToOne(mappedBy="aa",
    			targetEntity=ActivityAcousticDD.class,
    			fetch=FetchType.LAZY,
    			optional=true,
    			orphanRemoval=true,
    			cascade = {CascadeType.ALL})
	@Valid
    protected ActivityAcousticDD activityAcousticDD;

	public ActivityAcousticDD getActivityAcousticDD() {
		return activityAcousticDD;
	}
	public void setActivityAcousticDD(ActivityAcousticDD activityAcousticDD) {
		this.activityAcousticDD = activityAcousticDD;
	}

	@JsonManagedReference("activity-activityexplosives")
    @OneToOne(mappedBy="aa",
    			targetEntity=ActivityExplosives.class,
    			fetch=FetchType.LAZY,
    			optional=true,
    			orphanRemoval=true,
    			cascade = {CascadeType.ALL})
	@Valid
    protected ActivityExplosives activityExplosives;    
	
	public ActivityExplosives getActivityExplosives() {
		return activityExplosives;
	}
	public void setActivityExplosives(ActivityExplosives activityExplosives) {
		this.activityExplosives = activityExplosives;
	}

	@JsonManagedReference("activity-activitymultibeames")
    @OneToOne(mappedBy="aa",
    			targetEntity=ActivityMultibeamES.class,
    			fetch=FetchType.LAZY,
    			optional=true,
    			orphanRemoval=true,
    			cascade = {CascadeType.ALL})
	@Valid
    protected ActivityMultibeamES activityMultibeamES;

	public ActivityMultibeamES getActivityMultibeamES() {
		return activityMultibeamES;
	}
	public void setActivityMultibeamES(ActivityMultibeamES activityMultibeamES) {
		this.activityMultibeamES = activityMultibeamES;
	}
	
	@JsonManagedReference("activity-activitypiling")
    @OneToOne(mappedBy="aa",
    			targetEntity=ActivityPiling.class,
    			fetch=FetchType.LAZY,
    			optional=true,
    			orphanRemoval=true,
    			cascade = {CascadeType.ALL})
	@Valid
    protected ActivityPiling activityPiling;    

	public ActivityPiling getActivityPiling() {
		return activityPiling;
	}
	public void setActivityPiling(ActivityPiling activityPiling) {
		this.activityPiling = activityPiling;
	}

	@JsonManagedReference("activity-activitymod")
    @OneToOne(mappedBy="aa",
    			targetEntity=ActivityMoD.class,
    			fetch=FetchType.LAZY,
    			optional=true,
    			orphanRemoval=true,
    			cascade = {CascadeType.ALL})
	@Valid
    protected ActivityMoD activityMoD;    

	public ActivityMoD getActivityMoD() {
		return activityMoD;
	}
	public void setActivityMoD(ActivityMoD activityMoD) {
		this.activityMoD = activityMoD;
	}	
	
	@JsonManagedReference("activity-activitylocation")
	@OneToMany(mappedBy="aa",
				targetEntity=ActivityLocation.class,
				fetch=FetchType.EAGER,
				cascade = {CascadeType.ALL})
	@Valid
    protected List<ActivityLocation> pa; 
    
    public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
 
    public Integer getDate_start_day() {
    	return this.date_start_day;
	}
	public void setDate_start_day(Integer date_start_day) {
		this.date_start_day = date_start_day;
		getDate_start();
	}
	public Integer getDate_start_month() {
		return this.date_start_month;
	}
	public void setDate_start_month(Integer date_start_month) {
		this.date_start_month = date_start_month;
		getDate_start();
	}
	public Integer getDate_start_year() {
		return this.date_start_year;
	}
	public void setDate_start_year(Integer date_year) {
		this.date_start_year = date_year;
		getDate_start();
	}
	
	public Integer getDate_end_day() {
	   	return this.date_end_day;
	}
	public void setDate_end_day(Integer date_day) {
		this.date_end_day = date_day;
		getDate_end();
	}
	public Integer getDate_end_month() {
		return this.date_end_month;
	}
	public void setDate_end_month(Integer date_month) {
		this.date_end_month = date_month;
		getDate_end();
	}
	public Integer getDate_end_year() {
		return this.date_end_year;
	}
	public void setDate_end_year(Integer date_year) {
		this.date_end_year = date_year;
		getDate_end();
	}
		
	public Integer getDate_due_day() {
		return date_due_day;
	}
	public void setDate_due_day(Integer date_due_day) {
		this.date_due_day = date_due_day;
		getDate_due();
	}
	public Integer getDate_due_month() {
		return date_due_month;
	}
	public void setDate_due_month(Integer date_due_month) {
		this.date_due_month = date_due_month;
		getDate_due();
	}
	public Integer getDate_due_year() {
		return date_due_year;
	}
	public void setDate_due_year(Integer date_due_year) {
		this.date_due_year = date_due_year;
		getDate_due();
	}
	public Integer getDate_closed_day() {
		return date_closed_day;
	}
	public void setDate_closed_day(Integer date_closed_day) {
		this.date_closed_day = date_closed_day;
		getDate_closed();
	}
	public Integer getDate_closed_month() {
		return date_closed_month;
	}
	public void setDate_closed_month(Integer date_closed_month) {
		this.date_closed_month = date_closed_month;
		getDate_closed();
	}
	public Integer getDate_closed_year() {
		return date_closed_year;
	}
	public void setDate_closed_year(Integer date_closed_year) {
		this.date_closed_year = date_closed_year;
		getDate_closed();
	}
	
	public NoiseProducer getNoiseproducer() {
		return noiseproducer;
	}
	public void setNoiseproducer(NoiseProducer noiseproducer) {
		this.noiseproducer = noiseproducer;
	}
	
	public Regulator getRegulator() {
		return regulator;
	}
	public void setRegulator(Regulator regulator) {
		this.regulator = regulator;
	}
	public String getStatus() {
		return status;
	}
	public void setStatus(String status) {
		this.status = status;
	}
	public Date getDate_start() {
		try
		{
			if (this.date_start==null) {
				if (this.date_start_day!=null && this.date_start_month!=null && this.date_start_year!=null) {
					Calendar cal = Calendar.getInstance();
					cal.setLenient(false);
					cal.set(this.date_start_year, this.date_start_month-1, this.date_start_day, 0, 0, 0);
					this.date_start = cal.getTime();
				}
			} else {
				if (this.date_start_day == null || this.date_start_month == null || this.date_start_year ==null) {
					Calendar cal = Calendar.getInstance();
		    		cal.setTime(this.date_start);
		    		this.date_start_day = cal.get(Calendar.DAY_OF_MONTH);
		    		this.date_start_month = cal.get(Calendar.MONTH) + 1;
		    		this.date_start_year = cal.get(Calendar.YEAR);
				}
			}
		}
		catch (Exception e) {}
		
		return this.date_start;
	}
	
	public void setDate_start(Date date_start) {
		this.date_start = date_start;
	}
	
	public Date getDate_end() {
		try
		{
			if (this.date_end==null) {
				if (this.date_end_day!=null && this.date_end_month!=null && this.date_end_year!=null) {
					Calendar cal = Calendar.getInstance();
					cal.setLenient(false);
					cal.set(this.date_end_year, this.date_end_month-1, this.date_end_day, 0, 0, 0);
					this.date_end = cal.getTime();
				}
			} else {
				if (this.date_end_day == null || this.date_end_month == null || this.date_end_year ==null) {
					Calendar cal = Calendar.getInstance();
		    		cal.setTime(this.date_end);
		    		this.date_end_day = cal.get(Calendar.DAY_OF_MONTH);
		    		this.date_end_month = cal.get(Calendar.MONTH) + 1;
		    		this.date_end_year = cal.get(Calendar.YEAR);
				}
			}
		}
		catch (Exception e) {}
		
		return date_end;
	}
	public void setDate_end(Date date_end) {
		this.date_end = date_end;
	}
	public Date getDate_due() {
		try
		{
			if (this.date_due==null) {
				if (this.date_due_day!=null && this.date_due_month!=null && this.date_due_year!=null) {
					Calendar cal = Calendar.getInstance();
					cal.setLenient(false);
					cal.set(this.date_due_year, this.date_due_month-1, this.date_due_day, 0, 0, 0);
					this.date_due = cal.getTime();
				}
			}
		}
		catch (Exception e) {}
		
		return date_due;
	}
	public void setDate_due(Date date_due) {
		this.date_due = date_due;
	}
	public Date getDate_closed() {
		try
		{
			if (this.date_closed==null) {
				if (this.date_closed_day!=null && this.date_closed_month!=null && this.date_closed_year!=null) {
					Calendar cal = Calendar.getInstance();
					cal.setLenient(false);
					cal.set(this.date_closed_year, this.date_closed_month-1, this.date_closed_day, 0, 0, 0);
					this.date_closed = cal.getTime();
				}
			}
		}
		catch (Exception e) {}
		
		return date_closed;
	}
	public void setDate_closed(Date date_closed) {
		this.date_closed = date_closed;
	}
	public Timestamp getDate_updated() {
		return date_updated;
	}
	public void setDate_updated(Timestamp date_updated) {
		this.date_updated = date_updated;
	}
	public Integer getDuration() {
		return duration;
	}
	public void setDuration(Integer duration) {
		this.duration = duration;
	}
	public List<ActivityLocation> getPa() {
		return pa;
	}
	public void setPa(List<ActivityLocation> pa) {
		this.pa = pa;
	}
	
	public String getSaveasdraft() {
		return saveasdraft;
	}
	public void setSaveasdraft(String saveasdraft) {
		this.saveasdraft = saveasdraft;
	}
		
	public boolean getClosing() {
		return closing;
	}
	public void setClosing(boolean closing) {
		this.closing = closing;
	}
	public ActivityApplication getParent() {
		return parent;
	}
	public void setParent(ActivityApplication parent) {
		this.parent = parent;
	}
	/**
	 * validate
	 * 
	 * Custom validation for the ActivityApplication entity form binding.
	 * 
	 */
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		//Logger.error("ActivityApplication validate method()");
		try
		{
			if (this.getDate_start() != null && this.getDate_end() != null)
			{
				if (this.getDate_end().before(this.getDate_start()))
					errors.add(new ValidationError("date_start",  Messages.get("validation.startafterend")));
				else if (this.getDate_start() != null && this.getDate_end() != null)
				{
					long diff = date_end.getTime() - date_start.getTime();
					long daysDiff = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
								
					if ((long) duration > (daysDiff+1)) {
						errors.add(new ValidationError("duration",  Messages.get("validation.duration.too_long")));
					}
				}
			}
			//Ensure that the NoiseProducer and Regulator values exist in the database... 
			if (JPA.em().find(NoiseProducer.class, this.getNoiseproducer().getId())==null) {
				errors.add(new ValidationError("noiseproducer",  Messages.get("error.noiseproducer.invalid")));
			}
			
			if (JPA.em().find(Regulator.class, this.getRegulator().getId())==null) 
			{
				errors.add(new ValidationError("regulator",  Messages.get("error.regulator.invalid")));
			}
			
			
			if (this.getPa()!=null) {
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
			errors.add(new ValidationError("",  Messages.get("error.something.went.wrong")));
		}
		return errors.isEmpty() ? null : errors;
		
    }
	@Transactional
	public void cancel()
	{
		this.setStatus("Cancelled");
		JPA.em().persist(this);
	}
	private void calculateDueDate()
	{		
		try
		{
			Calendar cal = Calendar.getInstance();
			cal.setTime(getDate_end());
		
			Regulator reg = JPA.em().find(Regulator.class, this.getRegulator().getId());
			cal.add(Calendar.DATE, reg.getCloseoutdays());
			setDate_due(cal.getTime());
		}
		catch (Exception e) {
			// not a problem
			Logger.error("Application Due Date calculation: " + e.getMessage());
		}
	}
	
	public static void closeOut(ActivityApplicationCloseOut aaco, Long id, boolean interim) throws Exception {
		//Set status to proposed for new items
		//Logger.error("Closing the activity application!");
		ActivityApplication aa = ActivityApplication.findById(id);
		if (!aa.getStatus().equals("Proposed") && !aa.getStatus().equals("Interim Close-out")) {
    		throw new Exception("Forbidden operation");
    	}
		if (aa.getDate_due()==null) {
			aa.calculateDueDate();
		}
		if (interim) {
			aa.setStatus("Interim Close-out");
		} else {
			aa.setStatus("Closed");
			aa.setDate_closed(new Date());
		}
		
        JPA.em().merge(aa);
		
        //Logger.error("Processing location entries...");
		//Persist any proposed activity location details
        if (aaco.getPa() != null) {
	        Iterator<ActivityLocation> it = aaco.getPa().iterator();
	        while(it.hasNext()) {
	        	//Logger.error("Processing location...");
	        	ActivityLocation pa = it.next();
	        	pa.setAa(aa);
	        	pa.update();
	        }
        }
	}
	
	/**
     * Insert this new activity application.
     */
	@Transactional
    public void save() 
	{     	    
		if (this.getSaveasdraft()==null) {
			calculateDueDate();
	        //Set status to proposed for new items
	        this.setStatus("Proposed");
		} else {
			this.setStatus("Draft");
		}
        
    	this.unsetNotNeeded();
    	this.setNeeded();
    	
        JPA.em().persist(this);
    }
	
	private void setNeeded()
	{
		if (getActivitytype_id()==ActivityTypes.Seismic_Survey.toLong())
        {       	
        	this.getActivitySeismic().setAa(this);        
        }
        else if (getActivitytype_id()==ActivityTypes.Geophysical_Survey.toLong())
        {
        	this.getActivityGeophysical().setAa(this);
        }
        else if (getActivitytype_id()==ActivityTypes.Piling.toLong()) 
        {
        	this.getActivityPiling().setAa(this);
        }
        else if (getActivitytype_id()==ActivityTypes.Explosives.toLong())
        {
        	this.getActivityExplosives().setAa(this);
        }
        else if (getActivitytype_id()==ActivityTypes.Acoustic_Deterrent_Device.toLong()) 
        {
        	this.getActivityAcousticDD().setAa(this);
        }
        else if (getActivitytype_id()==ActivityTypes.Multibeam_Echosounders.toLong()) 
        {
        	this.getActivityMultibeamES().setAa(this);
        }
        else if (getActivitytype_id()==ActivityTypes.MoD.toLong()) 
        {
        	this.getActivityMoD().setAa(this); 
        }
        
        //Persist any activity location details
        if (this.getPa() != null) {
	        Iterator<ActivityLocation> it = this.getPa().iterator();
	        while(it.hasNext()) {
	        	ActivityLocation pa = it.next();
	        	pa.setAa(this);
	        }
        }
	}
	public void unsetNotNeeded()
	{
		if (getActivitytype_id()!=ActivityTypes.Seismic_Survey.toLong()) {
			this.setActivitySeismic(null);
		}
		
		if (getActivitytype_id()!=ActivityTypes.Geophysical_Survey.toLong()) {
			this.setActivityGeophysical(null);
		}
		
		if (getActivitytype_id()!=ActivityTypes.Piling.toLong()) {
			this.setActivityPiling(null);  
		}
			     
		if (getActivitytype_id()!=ActivityTypes.Explosives.toLong()) {
			this.setActivityExplosives(null);
		}
		if (getActivitytype_id()!=ActivityTypes.Acoustic_Deterrent_Device.toLong()) {  
			this.setActivityAcousticDD(null);
		}
		if (getActivitytype_id()!=ActivityTypes.Multibeam_Echosounders.toLong()) {
			this.setActivityMultibeamES(null);
		}
		if (getActivitytype_id()!=ActivityTypes.MoD.toLong()) {
			this.setActivityMoD(null);
		}
	}
    
    /**
     * Update this activity application.
     * Throws an exception if updates are not allowed for the current item.
     * @throws Exception 
     */
    public void update(Long id) throws Exception {
    	ActivityApplication aaDb = ActivityApplication.findById(id);
    	if (aaDb==null || !aaDb.getStatus().equals("Draft")) {
    		throw new Exception("Forbidden operation");
    	}
    	
    	if (this.getSaveasdraft()==null) {
			calculateDueDate();
	        //Set status to proposed for new items
	        this.setStatus("Proposed");
		} else {
			this.setStatus("Draft");
		}
    	
        this.id = id;
        this.unsetNotNeeded();
    	this.setNeeded();
    	
    	JPA.em().merge(this);
    }
    
    public void delete() throws Exception {
    	if (!this.getStatus().equals("Draft")) {
    		throw new Exception("Forbidden operation");
    	}
    	JPA.em().remove(this);
    }
    
    /**
     * This method should always be used to locate items to allow the enforcing of the application security. The 
     * EntityManager find method must not be used, as it will return an item by its id if that id exists in the database.
     * This method uses a query to locate the item by id, which allows a custom query (and any filters) to be applied.
     *  
     * @param id
     * @return ActivityApplication or null
     */
    public static ActivityApplication findById(Long id) {
    	TypedQuery<ActivityApplication> query = JPA.em().createNamedQuery("ActivityApplication.findById", ActivityApplication.class);
		
    	query.setParameter("id", id);
    	List<ActivityApplication> results = query.getResultList();
    	if (!results.isEmpty()) {
    		return results.get(0);
    	}
    	return null;
    }
    
	public static List<ActivityApplication> findAll() {
		TypedQuery<ActivityApplication> query = JPA.em().createNamedQuery("ActivityApplication.findAll", ActivityApplication.class);
		
		List<ActivityApplication> results = query.getResultList();
		return results;
	}
	
	public static List<ActivityApplication> findByStatus(String status,Organisation org) {
		TypedQuery<ActivityApplication> query = JPA.em().createNamedQuery("ActivityApplication.findApplicationsByStatus", ActivityApplication.class);
		List<String> statusVals = Arrays.asList(status.split(","));
		query.setParameter("status", statusVals);
		query.setParameter("orgid", org.getId());
		List<ActivityApplication> results = query.getResultList();
		
		return results;
	}
	
	public static List<ActivityApplication> findLateByRegulator(Regulator reg) {
		TypedQuery<ActivityApplication> query = JPA.em().createNamedQuery("ActivityApplication.findLateByRegulator", ActivityApplication.class);
		query.setParameter("reg", reg);
		List<ActivityApplication> results = query.getResultList();
		
		return results;
	}
	
	public static List<ActivityApplication> findLinked(ActivityApplication source) {
		List<ActivityApplication> results = new ArrayList<ActivityApplication>();
		TypedQuery<ActivityApplication> query = JPA.em().createNamedQuery("ActivityApplication.findLinkedByParent", ActivityApplication.class);
		if (source.getParent()!=null) {
			query.setParameter("parent", source.getParent());
			results.add(source.getParent());
		} else {
			query.setParameter("parent", source);
		}
		List<ActivityApplication> queryResults = query.getResultList();
		if (!queryResults.isEmpty()) {
			if (results.isEmpty()) {
				results.add(source);
			}
			results.addAll(queryResults);
		}
		return results;
	}
	
	public Long getActivitytype_id() {
		return activitytype_id;
	}
	public void setActivitytype_id(Long activitytype_id) {
		this.activitytype_id = activitytype_id;
	}

	public boolean isSavingAsClosed() {
		return (this.getClosing() || (this.getStatus()!=null && (this.getStatus().equals("Closed") || this.getStatus().equals("Interim Close-out"))));
	}
	
	public ActivityApplication createLinked() {
		ActivityApplication linked = new ActivityApplication();
		linked.setParent(this);
		linked.setNoiseproducer(this.getNoiseproducer());
		linked.setRegulator(this.getRegulator());
		linked.setDate_start(this.getDate_start());
		linked.setDate_end(this.getDate_end());
		linked.setDate_due(this.getDate_due());
		linked.setDuration(this.getDuration());
		linked.setNon_licensable(this.getNon_licensable());
		
		return linked;
	}
	
	/**
	 * Comparator classes for implementing custom sort requirements.
	 * 
	 * @author david.simpson
	 *
	 */
	public static class NoiseProducerComparator implements Comparator<ActivityApplication> {

		@Override
		public int compare(ActivityApplication aa, ActivityApplication aab) {
			int iOrg = aa.getNoiseproducer().getOrganisation().getOrganisation_name().compareTo(
					aab.getNoiseproducer().getOrganisation().getOrganisation_name());
			if (iOrg!=0)
				return iOrg;
			
			int iSt = aa.getDate_start().compareTo(aab.getDate_start());
			if (iSt!=0)
				return iSt;
			
			String sa = ActivityTypes.get(aa.getActivitytype_id());
			String sb = ActivityTypes.get(aab.getActivitytype_id());
			return sa.compareTo(sb);
		}
	}
	
	public static class DateDueComparator implements Comparator<ActivityApplication> {

		@Override
		public int compare(ActivityApplication o1, ActivityApplication o2) {
			// sort by date due, handling nulls first, then ascending by date due
			if (o1.getDate_due() == null) {
		        return (o2.getDate_due() == null) ? 0 : -1;
		    }
		    if (o2.getDate_due() == null) {
		        return 1;
		    }
		    return o1.getDate_due().compareTo(o2.getDate_due());
		}
		
	}
	
	public static class DateClosedComparator implements Comparator<ActivityApplication> {

		@Override
		public int compare(ActivityApplication o1, ActivityApplication o2) {
			// sort descending by date closed, handling nulls last
			if (o2.getDate_closed() == null) {
		        return (o1.getDate_closed() == null) ? 0 : -1;
		    }
		    if (o1.getDate_closed() == null) {
		        return 1;
		    }
		    return o2.getDate_closed().compareTo(o1.getDate_closed());
		}
		
	}

}