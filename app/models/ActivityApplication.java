package models;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
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
import javax.persistence.OrderBy;
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

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.wordnik.swagger.annotations.ApiModelProperty;

import play.Logger;
import play.data.format.*;
import play.data.validation.*;
import play.db.jpa.*;
import play.i18n.Messages;
import validators.CheckNoiseProducer;
import validators.CheckRegulator;

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
	@NamedQuery(name = "ActivityApplication.findApplicationsByStatus", query = "from ActivityApplication where status in (:status) and (noiseproducer.organisation.id = :orgid or regulator.organisation.id = :orgid) order by date_start desc"),
	@NamedQuery(name = "ActivityApplication.findLateByNoiseProducer", query = "from ActivityApplication where noiseproducer=:np and status in ('Proposed','Interim Close-out') and date_due < now()")
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
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIdentityInfo(generator=ObjectIdGenerators.PropertyGenerator.class, property="id")
@JsonPropertyOrder({"id", "status", "noiseproducer", "regulator", "non_licensable", 
					"date_start", "date_end",  "date_due", "date_closed", 
					"duration", "activitytype_id", 
					"activityAcousticDD", "activityExplosives", "activitySubBottomProfilers", "activityMoD",
					"activityMultibeamES", "activityPiling", "activitySeismic", "activitylocations"})
public class ActivityApplication
{
	@Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "activityapplication_seq_gen")
    @SequenceGenerator(name = "activityapplication_seq_gen", sequenceName = "activityapplication_id_seq")
    @Column(columnDefinition = "serial")
	@ApiModelProperty(position=0)
    protected Long id;
    
	@ManyToOne(optional=true)
    @JoinColumn(name="parent_id")
	@ApiModelProperty(position=19)
	protected ActivityApplication parent;
		
    @ManyToOne(optional=false)
    @JoinColumn(name="noiseproducer_id")
    @Valid 
    @CheckNoiseProducer
    @NotNull(message="validation.required")
    @ApiModelProperty(position=2, required=true)
    protected NoiseProducer noiseproducer;
    
    
    @NotNull(message="validation.required")
    @ApiModelProperty(position=10, required=true)
    protected Long activitytype_id;
    
    @ManyToOne(optional=false)
    @JoinColumn(name="regulator_id")    
    @Valid
    @CheckRegulator
    @NotNull(message="validation.required")
    @ApiModelProperty(position=3, required=true)
    protected Regulator regulator;
    
    @Column(length=10)
    @ApiModelProperty(position=1)
    protected String status;
    
    @NotNull(message="validation.required")
    @ApiModelProperty(position=4, required=true)
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
	@ApiModelProperty(position=5, required=true)
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
	@ApiModelProperty(position=6, required=true)
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
    @ApiModelProperty(position=7)
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
    @ApiModelProperty(position=8)
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
    
    /**
     * Updates the timestamp of when changed
     */
    @PreUpdate
    @PrePersist
    public void updateTimeStamps() {
    	date_updated = new Timestamp(new java.util.Date().getTime());
    }
    
    @NotNull(message="validation.required")
    @Min(value=1, message="validation.min")
    @ApiModelProperty(position=9, required=true)
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
    @ApiModelProperty(position=17)
    protected ActivitySeismic activitySeismic;
    

    /**
     * If there is an activity type then it returns it
     * @return the related ActivitySeismic
     */
	public ActivitySeismic getActivitySeismic() {
		return activitySeismic;
	}
	/**
	 * Sets an ActivitySeismic to be related
	 * @param activitySeismic the activity type to relate
	 */
	public void setActivitySeismic(ActivitySeismic activitySeismic) {
		this.activitySeismic = activitySeismic;
	}

	@JsonManagedReference("activity-activitysubbottomprofilers")
    @OneToOne(mappedBy="aa",
    			targetEntity=ActivitySubBottomProfilers.class,
    			fetch=FetchType.LAZY,
    			optional=true,
    			orphanRemoval=true,
    			cascade = {CascadeType.ALL})
	@Valid
	@ApiModelProperty(position=13)
    protected ActivitySubBottomProfilers activitySubBottomProfilers;
    

	/**
	 * If there is an activity type then it returns it
	 * @return the ActivitySubBottomProfilers
	 */
	public ActivitySubBottomProfilers getActivitySubBottomProfilers() {
		return activitySubBottomProfilers;
	}
	/**
	 * Sets an associated ActivitySubBottomProfilers
	 * @param activitySubBottomProfilers the activity type to relate
	 */
	public void setActivitySubBottomProfilers(ActivitySubBottomProfilers activitySubBottomProfilers) {
		this.activitySubBottomProfilers = activitySubBottomProfilers;
	}

	@JsonManagedReference("activity-activityacousticdd")
    @OneToOne(mappedBy="aa",
    			targetEntity=ActivityAcousticDD.class,
    			fetch=FetchType.LAZY,
    			optional=true,
    			orphanRemoval=true,
    			cascade = {CascadeType.ALL})
	@Valid
	@ApiModelProperty(position=11)
    protected ActivityAcousticDD activityAcousticDD;

	/**
	 * If there is an activity type then it returns it
	 * @return the related ActivityAcousticDD 
	 */
	public ActivityAcousticDD getActivityAcousticDD() {
		return activityAcousticDD;
	}
	/**
	 * Sets an activity type to relate 
	 * @param activityAcousticDD the activity type to relate
	 */
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
	@ApiModelProperty(position=12)
    protected ActivityExplosives activityExplosives;    
	
	/**
	 * If there is an activity type then it returns it
	 * @return the associated ActivityExplosives 
	 */
	public ActivityExplosives getActivityExplosives() {
		return activityExplosives;
	}
	/**
	 * Sets a related ActivityExplosives
	 * @param activityExplosives the activity type to relate
	 */
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
	@ApiModelProperty(position=15)
    protected ActivityMultibeamES activityMultibeamES;

	/**
	 * If there is an activity type then it returns it
	 * @return the related ActivityMultibeamES 
	 */
	public ActivityMultibeamES getActivityMultibeamES() {
		return activityMultibeamES;
	}
	/**
	 * Sets an associated ActivityMultibeamES
	 * @param activityMultibeamES the related activity type
	 */
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
	@ApiModelProperty(position=16)
    protected ActivityPiling activityPiling;    

	/**
	 * If there is an activity type then it returns it
	 * @return the related ActivityPiling 
	 */
	public ActivityPiling getActivityPiling() {
		return activityPiling;
	}
	/**
	 * Sets the associated ActivityPiling
	 * @param activityPiling the related activity type
	 */
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
	@ApiModelProperty(position=14)
    protected ActivityMoD activityMoD;    

	/**
	 * If there is an activity type then it returns it
	 * @return the related ActivityMoD 
	 */
	public ActivityMoD getActivityMoD() {
		return activityMoD;
	}
	/**
	 * Sets the associated ActivityMoD
	 * @param activityMoD the activity type to relate
	 */
	public void setActivityMoD(ActivityMoD activityMoD) {
		this.activityMoD = activityMoD;
	}	
	
	@JsonManagedReference("activity-activitylocation")
	@OneToMany(mappedBy="aa",
				targetEntity=ActivityLocation.class,
				fetch=FetchType.EAGER,
				cascade = {CascadeType.ALL})
	@OrderBy
	@Valid
	@ApiModelProperty(position=18)
    protected List<ActivityLocation> activitylocations; 
    
	/**
	 * Gets the id of the application
	 * @return the id of the application
	 */
    public Long getId() {
		return id;
	}
    /**
     * Sets the id of the application
     * @param id
     */
	public void setId(Long id) {
		this.id = id;
	}
 
	/**
	 * 
	 * @return the start date of the application
	 */
    public Integer getDate_start_day() {
    	return this.date_start_day;
	}
    /**
     * Set the start date of the application
     * @param date_start_day
     */
	public void setDate_start_day(Integer date_start_day) {
		this.date_start_day = date_start_day;
		getDate_start();
	}
	/**
	 * 
	 * @return the start month of the applicaton
	 */
	public Integer getDate_start_month() {
		return this.date_start_month;
	}
	/**
	 * Set  the start month of the application
	 * @param date_start_month
	 */
	public void setDate_start_month(Integer date_start_month) {
		this.date_start_month = date_start_month;
		getDate_start();
	}
	/**
	 * 
	 * @return the start year of the application
	 */
	public Integer getDate_start_year() {
		return this.date_start_year;
	}
	/**
	 * Sets the start year of the application
	 * @param date_year
	 */
	public void setDate_start_year(Integer date_year) {
		this.date_start_year = date_year;
		getDate_start();
	}
	
	/**
	 * Gets the end date of the application
	 * @return
	 */
	public Integer getDate_end_day() {
	   	return this.date_end_day;
	}
	/**
	 * Sets the end date of the application
	 * @param date_day
	 */
	public void setDate_end_day(Integer date_day) {
		this.date_end_day = date_day;
		getDate_end();
	}
	/**
	 * Gets the end month of the application
	 * @return
	 */
	public Integer getDate_end_month() {
		return this.date_end_month;
	}
	/**
	 * Sets the end month of the application
	 * @param date_month
	 */
	public void setDate_end_month(Integer date_month) {
		this.date_end_month = date_month;
		getDate_end();
	}
	/**
	 * Gets the end year of the application
	 * @return
	 */
	public Integer getDate_end_year() {
		return this.date_end_year;
	}
	/**
	 * Sets the end year of the application
	 * @param date_year
	 */
	public void setDate_end_year(Integer date_year) {
		this.date_end_year = date_year;
		getDate_end();
	}
		
	/**
	 * Gets the date the application is due
	 * @return
	 */
	public Integer getDate_due_day() {
		return date_due_day;
	}
	/**
	 * Sets the due date for the application
	 * @param date_due_day
	 */
	public void setDate_due_day(Integer date_due_day) {
		this.date_due_day = date_due_day;
		getDate_due();
	}
	/**
	 * Gets the month of the due date
	 * @return
	 */
	public Integer getDate_due_month() {
		return date_due_month;
	}
	/**
	 * Sets the month of the due date
	 * @param date_due_month
	 */
	public void setDate_due_month(Integer date_due_month) {
		this.date_due_month = date_due_month;
		getDate_due();
	}
	/**
	 * Gets the year of the due date
	 * @return
	 */
	public Integer getDate_due_year() {
		return date_due_year;
	}
	/**
	 * Sets the year of the due date
	 * @param date_due_year
	 */
	public void setDate_due_year(Integer date_due_year) {
		this.date_due_year = date_due_year;
		getDate_due();
	}
	/**
	 * Gets the date closed
	 * @return
	 */
	public Integer getDate_closed_day() {
		return date_closed_day;
	}
	/**
	 * Sets the date closed
	 * @param date_closed_day
	 */
	public void setDate_closed_day(Integer date_closed_day) {
		this.date_closed_day = date_closed_day;
		getDate_closed();
	}
	/**
	 * Gets the month of the date closed
	 * @return
	 */
	public Integer getDate_closed_month() {
		return date_closed_month;
	}
	/**
	 * Sets the month of the date closed
	 * @param date_closed_month
	 */
	public void setDate_closed_month(Integer date_closed_month) {
		this.date_closed_month = date_closed_month;
		getDate_closed();
	}
	/**
	 * Gets the year of the date closed
	 * @return
	 */
	public Integer getDate_closed_year() {
		return date_closed_year;
	}
	/**
	 * Sets the year of the date closed
	 * @param date_closed_year
	 */
	public void setDate_closed_year(Integer date_closed_year) {
		this.date_closed_year = date_closed_year;
		getDate_closed();
	}
	/**
	 * Gets the associated NoiseProducer
	 * @return
	 */
	public NoiseProducer getNoiseproducer() {
		return noiseproducer;
	}
	/**
	 * Sets the noiseproducer
	 * @param noiseproducer
	 */
	public void setNoiseproducer(NoiseProducer noiseproducer) {
		this.noiseproducer = noiseproducer;
	}
	
	/**
	 * Gets the associated regulator
	 * @return
	 */
	public Regulator getRegulator() {
		return regulator;
	}
	/**
	 * Sets the associated regulator
	 * @param regulator
	 */
	public void setRegulator(Regulator regulator) {
		this.regulator = regulator;
	}
	/**
	 * Gets the status
	 * @return
	 */
	public String getStatus() {
		return status;
	}
	/**
	 * Sets the status
	 * @param status
	 */
	public void setStatus(String status) {
		this.status = status;
	}
	/**
	 * Gets the start date as a date.  If null then uses todays date
	 * @return
	 */
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
	
	/**
	 * Sets the start date
	 * @param date_start
	 */
	public void setDate_start(Date date_start) {
		this.date_start = date_start;
	}
	
	/**
	 * Gets teh end date as a date and todays date if not available
	 * @return
	 */
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
	/**
	 * Sets the end date
	 * @param date_end
	 */
	public void setDate_end(Date date_end) {
		this.date_end = date_end;
	}
	/**
	 * Gets the date due as a date
	 * @return
	 */
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
	/**
	 * Sets the due date
	 * @param date_due
	 */
	public void setDate_due(Date date_due) {
		this.date_due = date_due;
	}
	/**
	 * Gets the date closed as a date
	 * @return
	 */
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
	/**
	 * Sets the date closed
	 * @param date_closed
	 */
	public void setDate_closed(Date date_closed) {
		this.date_closed = date_closed;
	}
	/**
	 * Gets the time updated
	 * @return
	 */
	public Timestamp getDate_updated() {
		return date_updated;
	}
	/**
	 * Sets the time updated
	 * @param date_updated
	 */
	public void setDate_updated(Timestamp date_updated) {
		this.date_updated = date_updated;
	}
	/**
	 * Gets the duration (days)
	 * @return
	 */
	public Integer getDuration() {
		return duration;
	}
	/**
	 * Sets the duration
	 * @param duration
	 */
	public void setDuration(Integer duration) {
		this.duration = duration;
	}
	
	/**
	 * Gets all of the ActivityLocations
	 * @return
	 */
	public List<ActivityLocation> getActivitylocations() {
		return activitylocations;
	}
	/**
	 * Sets the ActivityLocations
	 * @param activitylocations
	 */
	public void setActivitylocations(List<ActivityLocation> activitylocations) {
		this.activitylocations = activitylocations;
	}
	/**
	 * Gets whether this is saved as a draft
	 * @return
	 */
	public String getSaveasdraft() {
		return saveasdraft;
	}
	/**
	 * Sets whether this is saved as a draft
	 * @param saveasdraft
	 */
	public void setSaveasdraft(String saveasdraft) {
		this.saveasdraft = saveasdraft;
	}
		
	/**
	 * Gets the closing flag
	 * @return
	 */
	public boolean getClosing() {
		return closing;
	}
	/**
	 * Sets the closing flag
	 * @param closing
	 */
	public void setClosing(boolean closing) {
		this.closing = closing;
	}
	/**
	 * Gets the parent ActivityApplication
	 * @return
	 */
	public ActivityApplication getParent() {
		return parent;
	}
	public void setParent(ActivityApplication parent) {
		this.parent = parent;
	}
	
	private Calendar dateWithoutTime(Date dateVal) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateVal);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		return cal;
	}
	/**
	 * validate
	 * 
	 * Custom validation for the ActivityApplication entity form binding.
	 * @return a list of validation errors
	 */
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		try
		{
			if (this.getDate_start() != null && this.getDate_end() != null)
			{
				if (this.getDate_end().before(this.getDate_start()))
					errors.add(new ValidationError("date_start",  Messages.get("validation.startafterend")));
				else if (this.getDate_start() != null && this.getDate_end() != null)
				{
					//Avoid issues with time components being set - force them to zero.
					Calendar cal = dateWithoutTime(date_start);
					long start_time = cal.getTimeInMillis();
					
					cal = dateWithoutTime(date_end);
					long end_time = cal.getTimeInMillis();
					
					long diff = end_time - start_time;
					long daysDiff = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
								
					if ((long) duration > (daysDiff+1)) {
						errors.add(new ValidationError("duration",  Messages.get("validation.duration.too_long")));
					}
				}
			}
			//Ensure that the NoiseProducer and Regulator values exist in the database... 
			if (JPA.em().find(NoiseProducer.class, this.getNoiseproducer().getId())==null) {
				errors.add(new ValidationError("noiseproducer",  Messages.get("validation.noiseproducer.invalid")));
			}
			
			if (JPA.em().find(Regulator.class, this.getRegulator().getId())==null) 
			{
				errors.add(new ValidationError("regulator",  Messages.get("validation.regulator.invalid")));
			}
			
			
			if (this.getActivitylocations()!=null) {
				Iterator<ActivityLocation> it = this.getActivitylocations().iterator();
				while (it.hasNext()) {
					ActivityLocation al = it.next();
					List<ValidationError> location_errors = al.validate();
					if (location_errors!=null) {
						Iterator<ValidationError> itErr = location_errors.iterator();
						while (itErr.hasNext()) {
							ValidationError err=itErr.next();
							errors.add(new ValidationError("activitylocations[" + this.getActivitylocations().indexOf(al) + "]." + err.key(), err.message()));
						}
						
					}
				}
			}
			
			if (activitytype_id==ActivityTypes.Seismic_Survey.toLong()) {
				if (getActivitySeismic().survey_type.compareToIgnoreCase("other")==0 && getActivitySeismic().other_survey_type.trim().compareTo("")==0)
				{
					errors.add(new ValidationError("activitySeismic.other_survey_type",  Messages.get("validation.required")));
				}
			}	
			
		}
		catch (Exception e)
		{
			errors.add(new ValidationError("",  Messages.get("error.something.went.wrong")));
		}
		return errors.isEmpty() ? null : errors;
		
    }
	/**
	 * Marks the application as cancelled in the database
	 */
	@Transactional
	public void cancel()
	{
		this.setStatus("Cancelled");
		JPA.em().persist(this);
	}
	/**
	 * Calculates the due date using the information for the regulators in the database
	 */
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
	public void addActuals(Map<String,String> m)
	{
		if (getActivitytype_id()==ActivityTypes.Seismic_Survey.toLong())
        {       	
        	this.getActivitySeismic().mergeActuals(m);        
        }
        else if (getActivitytype_id()==ActivityTypes.Sub_Bottom_Profilers.toLong())
        {
        	this.getActivitySubBottomProfilers().mergeActuals(m);
        }
        else if (getActivitytype_id()==ActivityTypes.Piling.toLong()) 
        {
        	this.getActivityPiling().mergeActuals(m);
        }
        else if (getActivitytype_id()==ActivityTypes.Explosives.toLong())
        {
        	this.getActivityExplosives().mergeActuals(m);
        }
        else if (getActivitytype_id()==ActivityTypes.Acoustic_Deterrent_Device.toLong()) 
        {
        	this.getActivityAcousticDD().mergeActuals(m);
        }
        else if (getActivitytype_id()==ActivityTypes.Multibeam_Echosounders.toLong()) 
        {
        	this.getActivityMultibeamES().mergeActuals(m);
        }
        else if (getActivitytype_id()==ActivityTypes.MoD.toLong()) 
        {
        	this.getActivityMoD().mergeActuals(m);
        }		
	}
	
	/**
	 * Closes out the application
	 * @param aaco additional data submitted at close out
	 * @param id the application to be closed
	 * @param interim flag to indicate whether this is an interim close out
	 * @throws Exception
	 */
	public static void closeOut(ActivityApplicationCloseOut aaco, Long id, boolean interim, Map<String, String> m) throws Exception {
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

		aa.addActuals(m);
		
        JPA.em().merge(aa);

		//Persist any proposed activity location details
        if (aaco.getActivitylocations() != null) {
	        Iterator<ActivityLocation> it = aaco.getActivitylocations().iterator();
	        while(it.hasNext()) {
	        	ActivityLocation al = it.next();
	        	al.setAa(aa);
	        	al.update();
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
	
	/**
	 * Because we may or may not have any of these activity types associated with an application
	 * we need to set and unset some of the data in the request
	 */
	private void setNeeded()
	{
		if (getActivitytype_id()==ActivityTypes.Seismic_Survey.toLong())
        {       	
        	this.getActivitySeismic().setAa(this);        
        }
        else if (getActivitytype_id()==ActivityTypes.Sub_Bottom_Profilers.toLong())
        {
        	this.getActivitySubBottomProfilers().setAa(this);
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
        if (this.getActivitylocations() != null) {
	        Iterator<ActivityLocation> it = this.getActivitylocations().iterator();
	        while(it.hasNext()) {
	        	ActivityLocation al = it.next();
	        	al.setAa(this);
	        }
        }
	}
	/**
	 * Because we may or may not have any of these activity types associated with an application
	 * we need to set and unset some of the data in the request
	 */
	public void unsetNotNeeded()
	{
		if (getActivitytype_id()!=ActivityTypes.Seismic_Survey.toLong()) {
			this.setActivitySeismic(null);
		}
		
		if (getActivitytype_id()!=ActivityTypes.Sub_Bottom_Profilers.toLong()) {
			this.setActivitySubBottomProfilers(null);
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
     * Allow the deletion of activity locations when the item is draft
     * 
     * @param aaDb the persisted activity application matching the current item
     */
	private void removeDeletedLocations(ActivityApplication aaDb) {
		//get the list of activity location id values from the current item
		List<Long> currentLocationIds = new ArrayList<Long>();
		if (this.getActivitylocations() != null) {
			Iterator<ActivityLocation> it = this.getActivitylocations().iterator();
			while (it.hasNext()) {
				ActivityLocation al = it.next();
				if (al.getId() != null) {
					currentLocationIds.add(al.getId());
				}
			}
		}
		//iterate over the stored locations, removing any that are not in the current list
		if (aaDb.getActivitylocations()!=null) {
			Iterator<ActivityLocation> itStored = aaDb.getActivitylocations().iterator();
			while (itStored.hasNext()) {
				ActivityLocation alStored = itStored.next();
				if (currentLocationIds.isEmpty() || !currentLocationIds.contains(alStored.getId())) {
					JPA.em().remove(alStored);
				}
			}
		}
	}
    /**
     * Update this activity application and related activity type and activity location details.
     * Throws an exception if updates are not allowed for the current item.
     * @param id the application
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
        this.removeDeletedLocations(aaDb);
        this.unsetNotNeeded();
    	this.setNeeded();
    	
    	if (this.parent != null && this.parent.getId() == null) {
    		this.parent = null;
    	}
    	
    	JPA.em().merge(this);
    }
    /**
     * deletes the application from the database
     * @throws Exception
     */
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
     * @param id the application to be found
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
    /**
     * Gets a list of applications
     * @return
     */
	public static List<ActivityApplication> findAll() {
		TypedQuery<ActivityApplication> query = JPA.em().createNamedQuery("ActivityApplication.findAll", ActivityApplication.class);
		
		List<ActivityApplication> results = query.getResultList();
		return results;
	}
	
	/**
	 * Gets a list of applications matching one or more statuses
	 * @param status comma separated list of statuses
	 * @param org the organisation in context (either a noise producer or regulator for the application)
	 * @return
	 */
	public static List<ActivityApplication> findByStatus(String status,Organisation org) {
		TypedQuery<ActivityApplication> query = JPA.em().createNamedQuery("ActivityApplication.findApplicationsByStatus", ActivityApplication.class);
		List<String> statusVals = Arrays.asList(status.split(","));
		query.setParameter("status", statusVals);
		query.setParameter("orgid", org.getId());
		List<ActivityApplication> results = query.getResultList();
		
		return results;
	}
	
	/**
	 * Gets applications for the given regulator that are late
	 * @param reg the regulator
	 * @return
	 */
	public static List<ActivityApplication> findLateByRegulator(Regulator reg) {
		TypedQuery<ActivityApplication> query = JPA.em().createNamedQuery("ActivityApplication.findLateByRegulator", ActivityApplication.class);
		query.setParameter("reg", reg);
		List<ActivityApplication> results = query.getResultList();
		
		return results;
	}
	
	/**
	 * Gets applications for the given noiseproducer that are late
	 * @param reg the noiseproducer
	 * @return
	 */
	public static List<ActivityApplication> findLateByNoiseProducer(NoiseProducer np) {
		TypedQuery<ActivityApplication> query = JPA.em().createNamedQuery("ActivityApplication.findLateByNoiseProducer", ActivityApplication.class);
		query.setParameter("np", np);
		List<ActivityApplication> results = query.getResultList();
		
		return results;
	}	
	
	/**
	 * Gets any linked applications
	 * @param source the application from which they may be linked
	 * @return
	 */
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
	
	/**
	 * Gets the activity type as a number
	 * @return
	 */
	public Long getActivitytype_id() {
		return activitytype_id;
	}
	/**
	 * Sets the activity type
	 * @param activitytype_id
	 */
	public void setActivitytype_id(Long activitytype_id) {
		this.activitytype_id = activitytype_id;
	}

	/**
	 * Gets whether the application is closed
	 * @return
	 */
	@JsonIgnore
	public boolean isSavingAsClosed() {
		return (this.getClosing() || (this.getStatus()!=null && (this.getStatus().equals("Closed") || this.getStatus().equals("Interim Close-out"))));
	}
	
	/**
	 * Create a linked application
	 * @return the newly linked application
	 */
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
	public void populateActivityDefaults()
	{
		if (getActivitytype_id()==ActivityTypes.Seismic_Survey.toLong())
        {       	
        	this.getActivitySeismic().populateDefaults();        
        }
        else if (getActivitytype_id()==ActivityTypes.Sub_Bottom_Profilers.toLong())
        {
        	this.getActivitySubBottomProfilers().populateDefaults();
        }
        else if (getActivitytype_id()==ActivityTypes.Piling.toLong()) 
        {
        	this.getActivityPiling().populateDefaults();
        }
        else if (getActivitytype_id()==ActivityTypes.Explosives.toLong())
        {
        	this.getActivityExplosives().populateDefaults();
        }
        else if (getActivitytype_id()==ActivityTypes.Acoustic_Deterrent_Device.toLong()) 
        {
        	this.getActivityAcousticDD().populateDefaults();
        }
        else if (getActivitytype_id()==ActivityTypes.Multibeam_Echosounders.toLong()) 
        {
        	this.getActivityMultibeamES().populateDefaults();
        }
        else if (getActivitytype_id()==ActivityTypes.MoD.toLong()) 
        {
        	this.getActivityMoD().populateDefaults();
        }
	}
}
