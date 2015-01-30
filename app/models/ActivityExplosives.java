package models;

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

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name="activityexplosives")
public class ActivityExplosives
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "activityexplosives_seq_gen")
    @SequenceGenerator(name = "activityexplosives_seq_gen", sequenceName = "activityexplosives_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;   
    
    @JsonBackReference("activity-activityexplosives")
	@OneToOne(optional=false)
    @JoinColumn(name="activityapplication_id",referencedColumnName="id")
    protected ActivityApplication aa;
    
    @Min(value=1, message="validation.min")
    @Max(value=500, message="validation.max")
    @NotNull(message="validation.required")
	protected Double tnt_equivalent;

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


	public Double getTnt_equivalent() {
		return tnt_equivalent;
	}

	public void setTnt_equivalent(Double tnt_equivalent) {
		this.tnt_equivalent = tnt_equivalent;
	}

	public ActivityExplosives() {
		super();
	}
	
}