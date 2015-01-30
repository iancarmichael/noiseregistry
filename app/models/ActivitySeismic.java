package models;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonBackReference;

import play.data.validation.ValidationError;
import play.i18n.Messages;

@Entity
@Table(name="activityseismic")
public class ActivitySeismic
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "activityseismic_seq_gen")
    @SequenceGenerator(name = "activityseismic_seq_gen", sequenceName = "activityseismic_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;   
    
    @JsonBackReference("activity-activityseismic")
	@OneToOne(optional=false)
    @JoinColumn(name="activityapplication_id",referencedColumnName="id")
    protected ActivityApplication aa;
    
	@Column(length=10)
	@NotBlank(message="validation.required")
	protected String survey_type;
	
	@Column(length=2)
	@NotBlank(message="validation.required")
	protected String data_type;
	
	@Column(length=50)
	protected String other_survey_type;

	@Min(value=1, message="validation.min")
	@NotNull(message="validation.required")
	protected Integer max_airgun_volume;
	
	@Min(value=1, message="validation.min")
	@Max(value=500, message="validation.max")
	@NotNull(message="validation.required")
	protected Integer sound_pressure_level;
	
	@Min(value=1, message="validation.min")
	@Max(value=500, message="validation.max")
	@NotNull(message="validation.required")
	protected Integer sound_exposure_level;
	
	public Long getId() {
		return id;
	}
	public ActivityApplication getAa() {
		return aa;
	}
	public void setAa(ActivityApplication aa) {
		this.aa = aa;
	}
	public String getSurvey_type() {
		return survey_type;
	}
	public void setSurvey_type(String survey_type) {
		this.survey_type = survey_type;
	}
	public String getData_type() {
		return data_type;
	}
	public void setData_type(String data_type) {
		this.data_type = data_type;
	}
	public String getOther_survey_type() {
		return other_survey_type;
	}
	public void setOther_survey_type(String other_survey_type) {
		this.other_survey_type = other_survey_type;
	}
	public Integer getMax_airgun_volume() {
		return max_airgun_volume;
	}
	public void setMax_airgun_volume(Integer max_airgun_volume) {
		this.max_airgun_volume = max_airgun_volume;
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
	public void setId(Long id) {
		this.id = id;
	}
	public ActivitySeismic() 
	{
		
	}
	public List<ValidationError> validate() 
	{
		List<ValidationError> errors = new ArrayList<ValidationError>();
		
		if (survey_type.compareToIgnoreCase("other")==0 && other_survey_type.trim().compareTo("")==0)
		{
			errors.add(new ValidationError("other_survey_type",  Messages.get("error.no.survey.type")));
		}
			
		
		return errors.isEmpty() ? null : errors;
    }		
	
	public ActivitySeismic(ActivityApplication aa_p, Map map) 
	{
		this.setAa(aa_p);
		this.setData_type((String)map.get("seismic_datatype.id"));
		this.setMax_airgun_volume(new Integer((String)map.get("seismic_max_airgun")));
		this.setOther_survey_type((String)map.get("seismic_othersurvey_type"));		
		this.setSound_exposure_level(new Integer((String)map.get("seismic_exposure_level")));
		this.setSound_pressure_level(new Integer((String)map.get("seismic_sound_pressure")));
		this.setSurvey_type((String)map.get("seismic_surveytype.id"));
	}
}