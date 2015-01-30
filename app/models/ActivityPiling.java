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
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name="activitypiling")
public class ActivityPiling
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "activitypiling_seq_gen")
    @SequenceGenerator(name = "activitypiling_seq_gen", sequenceName = "activitypiling_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;   
    
    @JsonBackReference("activity-activitypiling")
    @OneToOne(optional=false)
    @JoinColumn(name="activityapplication_id",referencedColumnName="id")
    protected ActivityApplication aa;
    
    @Min(value=1, message="validation.min")
    @NotNull(message="validation.required")
	protected Integer max_hammer_energy;

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

	public Integer getMax_hammer_energy() {
		return max_hammer_energy;
	}

	public void setMax_hammer_energy(Integer max_hammer_energy) {
		this.max_hammer_energy = max_hammer_energy;
	}

	public ActivityPiling() {
		super();
	}
}