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

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name="activityexplosives")
public class ActivityExplosives
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "activityexplosives_seq_gen")
    @SequenceGenerator(name = "activityexplosives_seq_gen", sequenceName = "activityexplosives_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;   
    
    //@JsonBackReference("activity-activityexplosives")
    @JsonIgnore
	@OneToOne(optional=false)
    @JoinColumn(name="activityapplication_id",referencedColumnName="id")
    protected ActivityApplication aa;
    
    @Min(value=1, message="validation.min")
    @Max(value=500, message="validation.max")
    @NotNull(message="validation.required")
	protected Double tnt_equivalent;

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
	 * Gets the associated application
	 * @return
	 */
	public ActivityApplication getAa() {
		return aa;
	}

	/**
	 * Sets the associated application
	 * @param aa
	 */
	public void setAa(ActivityApplication aa) {
		this.aa = aa;
	}

	/**
	 * Gets the TNT equivalent
	 * @return
	 */
	public Double getTnt_equivalent() {
		return tnt_equivalent;
	}

	/**
	 * Sets the TNT equivalent
	 * @param tnt_equivalent
	 */
	public void setTnt_equivalent(Double tnt_equivalent) {
		this.tnt_equivalent = tnt_equivalent;
	}

	/**
	 * Constructor
	 */
	public ActivityExplosives() {
		super();
	}
	
}