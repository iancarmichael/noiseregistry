package models;

import java.util.LinkedHashMap;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.validation.constraints.Min;

import com.fasterxml.jackson.annotation.JsonIgnore;

import play.db.jpa.JPA;

@NamedQueries({
	@NamedQuery(name="Regulator.findAll", query="from Regulator order by organisation.organisation_name")
})

@Entity
@Table(name="regulator")
public class Regulator {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "regulator_seq_gen")
	@SequenceGenerator(name = "regulator_seq_gen", sequenceName = "regulator_id_seq")
	@Column(columnDefinition = "serial")
	protected Long id; 
	
	@OneToOne
    @JoinColumn(name="organisation_id",referencedColumnName="id")
	@JsonIgnore
	protected Organisation organisation;
	
	@JsonIgnore
	@Min(value=1, message="validation.min")
	protected Integer closeoutdays;

	@JsonIgnore
	protected Boolean accepts_email;
	
	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Organisation getOrganisation() {
		return organisation;
	}

	public void setOrganisation(Organisation organisation) {
		this.organisation = organisation;
	}

	public Integer getCloseoutdays() {
		return closeoutdays;
	}

	public void setCloseoutdays(Integer closeoutdays) {
		this.closeoutdays = closeoutdays;
	}
	
	public Boolean getAccepts_email() {
		return accepts_email;
	}

	public void setAccepts_email(Boolean accepts_email) {
		this.accepts_email = accepts_email;
	}

	public static List<Regulator> findAll() {
		TypedQuery<Regulator> query = JPA.em().createNamedQuery("Regulator.findAll", Regulator.class);
		
		List<Regulator> results = query.getResultList();
		return results;
	}
	public void save() {
		JPA.em().persist(this);
		JPA.em().persist(this.getOrganisation());
	}	
	
	public static LinkedHashMap<String, String> getOptions() {
		List<Regulator> regs = findAll();
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		if (!regs.isEmpty()) {
			for(Regulator reg: regs) {
            	options.put(Long.toString(reg.id), reg.getOrganisation().getOrganisation_name());
        	}
		}
		return options;
	}	
}