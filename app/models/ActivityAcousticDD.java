package models;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name="activityacousticdd")
public class ActivityAcousticDD
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "activityacousticdd_seq_gen")
    @SequenceGenerator(name = "activityacousticdd_seq_gen", sequenceName = "activityacousticdd_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;   
    
    @JsonBackReference("activity-activityacousticdd")
	@ManyToOne(optional=true)
    @JoinColumn(name="activityapplication_id",referencedColumnName="id")
    protected ActivityApplication aa;
    
    @Min(value=1, message="validation.min")
	protected Integer frequency;
    
    @Min(value=1, message="validation.min")
    @Max(value=500, message="validation.max")
	protected Integer sound_pressure_level;
    
    @Min(value=1, message="validation.min")
    @Max(value=500, message="validation.max")    
	protected Integer sound_exposure_level;
	
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
	public Integer getFrequency() {
		return frequency;
	}
	public void setFrequency(Integer frequency) {
		this.frequency = frequency;
	}
	public Integer getSound_pressure_level() {
		return sound_pressure_level;
	}
	public void setSound_pressure_level(Integer sound_pressure_level) {
		this.sound_pressure_level = sound_pressure_level;
	}
	public Integer getSound_exposure_level() {
		return sound_exposure_level;
	}
	public void setSound_exposure_level(Integer sound_exposure_level) {
		this.sound_exposure_level = sound_exposure_level;
	}
	public ActivityAcousticDD() {
		super();
	}
	
}