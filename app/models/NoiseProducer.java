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
import javax.validation.Valid;

import com.fasterxml.jackson.annotation.JsonIgnore;

import play.db.jpa.JPA;

@NamedQueries({
	@NamedQuery(name="NoiseProducer.findAll", query="from NoiseProducer order by organisation.organisation_name"),
})
@Entity
@Table(name="noiseproducer")
public class NoiseProducer {
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO, generator = "noiseproducer_seq_gen")
	@SequenceGenerator(name = "noiseproducer_seq_gen", sequenceName = "noiseproducer_id_seq")
	@Column(columnDefinition = "serial")
	protected Long id; 
	
	@OneToOne
	@JsonIgnore
    @JoinColumn(name="organisation_id",referencedColumnName="id")
    @Valid
	protected Organisation organisation;

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

	public static List<NoiseProducer> findAll() {
		TypedQuery<NoiseProducer> query = JPA.em().createNamedQuery("NoiseProducer.findAll", NoiseProducer.class);
		
		List<NoiseProducer> results = query.getResultList();
		return results;
	}
	
	public static LinkedHashMap<String, String> getOptions(AppUser au) {
		
		List<NoiseProducer> nps = au.findNoiseProducers();
		LinkedHashMap<String, String> options = new LinkedHashMap<String, String>();
		if (!nps.isEmpty()) {
			for(NoiseProducer np: nps) {
	            options.put(Long.toString(np.id), np.getOrganisation().getOrganisation_name());
	        }
		}
		return options;
	}
	
	private void setOrgAdmin(AppUser au) {
		//having saved the new organisation, set the current user to be the first admin user...
		OrgUser orguser = new OrgUser();
		orguser.setOrg(this.getOrganisation());
		orguser.setStatus("verified");
		orguser.setAu(au);
		orguser.setAdministrator(true);
		JPA.em().persist(orguser);
	}
	
	public void save(AppUser au) {
		JPA.em().persist(this);
		JPA.em().persist(this.getOrganisation());
		this.setOrgAdmin(au);
	}

	/**
	 * AppUser should be passed to the save method so that the first
	 * administrator gets set correctly at creation.  This method remains for test cases.
	 * 
	 */
	public void saveNoAdmin() {
		JPA.em().persist(this);
		JPA.em().persist(this.getOrganisation());
	}
	
    public void update(Long id) {
        this.id = id;
        JPA.em().merge(this);
    }
}
