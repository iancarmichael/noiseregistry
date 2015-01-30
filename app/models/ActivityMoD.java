package models;

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
import javax.persistence.Transient;

import org.hibernate.validator.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;

import play.db.jpa.JPA;
import play.db.jpa.Transactional;

@Entity
@Table(name="activitymod")
public class ActivityMoD
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "activitymod_seq_gen")
    @SequenceGenerator(name = "activitymod_seq_gen", sequenceName = "activitymod_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;   
    
    @JsonBackReference("activity-activitymod")
	@OneToOne(optional=false)
    @JoinColumn(name="activityapplication_id",referencedColumnName="id")
    protected ActivityApplication aa;
    
	@Column(length=20)
	@NotBlank(message="validation.required")
	protected String source;
	
	@JsonIgnore
	@Transient
	protected String dummy;
	
	public String getDummy() {
		return dummy;
	}
	public void setDummy(String dummy) {
		this.dummy = dummy;
	}
	public Long getId() {
		return id;
	}
	public ActivityApplication getAa() {
		return aa;
	}
	public void setAa(ActivityApplication aa) {
		this.aa = aa;
	}
	public String getSource() {
		return source;
	}
	public void setSource(String source) {
		this.source = source;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public ActivityMoD() 
	{
		
	}
	public ActivityMoD(ActivityApplication aa_p, Map map) 
	{
		this.setAa(aa_p);
		this.setSource((String)map.get("mod_source.id"));
	}
	@Transactional
	public void save()
	{
		JPA.em().persist(this);
	}
}