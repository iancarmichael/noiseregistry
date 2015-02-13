package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.TypedQuery;

import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.Length;

import play.Logger;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;

import com.fasterxml.jackson.annotation.JsonIgnore;

@NamedQueries({
	@NamedQuery(name="Organisation.findAll", query="from Organisation order by organisation_name"),
	@NamedQuery(name="Organisation.findRegulators", query="from Organisation where regulator=true"),
	@NamedQuery(name="Organisation.findNonRegulators", query="from Organisation where regulator=false"),
	@NamedQuery(name="Organisation.findUsers", query="from OrgUser as ou where ou.org.id = :id order by ou.au.fullname"),
	@NamedQuery(name="Organisation.findUser", query="from OrgUser as ou where ou.id = :id"),
	@NamedQuery(name="Organisation.findAdmins", query="from OrgUser where org = :org and administrator = true"),
	@NamedQuery(name="Organisation.findRegulator", query="from Regulator as reg where reg.organisation=:org"),
	@NamedQuery(name="Organisation.findNoiseProducer", query="from NoiseProducer as np where np.organisation=:org")
})

@NamedNativeQueries({
	@NamedNativeQuery(name="Organisation.myAdminOrganisations",
			query="select * from organisation inner join orguser on orguser.organisation_id = organisation.id "
					+ "inner join appuser on appuser.id = orguser.appuser_id where appuser.email_address=:email and orguser.administrator "+
					"order by organisation_name",
			resultClass=Organisation.class),
			@NamedNativeQuery(name="Organisation.myOrganisations",
			query="select * from organisation inner join orguser on orguser.organisation_id = organisation.id "
					+ "inner join appuser on appuser.id = orguser.appuser_id where appuser.email_address=:email "
					+ "order by organisation_name",
			resultClass=Organisation.class),
})



@Entity
@Table(name="organisation")
public class Organisation
{
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "organisation_seq_gen")
    @SequenceGenerator(name = "organisation_seq_gen", sequenceName = "organisation_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;
    
    @Required
    @Column(length=50)
    @Length(max=50, message="validation.field_too_long")
    protected String organisation_name;
        
    @Required
    @Column(length=50)
    @Length(max=50, message="validation.field_too_long")
    protected String contact_name;
    
    @Required
    @Column(length=50)
    @Email
    @Length(max=50, message="validation.field_too_long")
    protected String contact_email;
    
    @Transient
    protected Boolean accepts_email;
    
    @Required
    @Column(length=20)
    @Length(max=20, message="validation.field_too_long")
    protected String contact_phone;
    
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
	 * Gets the name of the organisation
	 * @return
	 */
	public String getOrganisation_name() {
		return organisation_name;
	}

	/**
	 * Sets the name of the organisation
	 * @param organisation_name
	 */
	public void setOrganisation_name(String organisation_name) {
		this.organisation_name = organisation_name;
	}
	/**
	 * Gets the contact name
	 * @return
	 */
	public String getContact_name() {
		return contact_name;
	}

	/**
	 * Sets the contact name
	 * @param contact_name
	 */
	public void setContact_name(String contact_name) {
		this.contact_name = contact_name;
	}

	/**
	 * Gets the contact email
	 * @return
	 */
	public String getContact_email() {
		return contact_email;
	}

	/**
	 * Sets the contact email
	 * @param contact_email
	 */
	public void setContact_email(String contact_email) {
		this.contact_email = contact_email;
	}

	/**
	 * Gets the contact phone number
	 * @return
	 */
	public String getContact_phone() {
		return contact_phone;
	}

	/**
	 * Sets the contact phone number
	 * @param contact_phone
	 */
	public void setContact_phone(String contact_phone) {
		this.contact_phone = contact_phone;
	}

	/**
	 * Gets whether the organisation accepts email notifications
	 * @return
	 */
	public Boolean getAccepts_email() {
		return accepts_email;
	}

	/**
	 * Sets whether the organisation accepts email notifications
	 * @param accepts_email
	 */
	public void setAccepts_email(Boolean accepts_email) {
		this.accepts_email = accepts_email;
	}

	/**
	 * Gets a list of regulators
	 * @return
	 */
	public static List<Organisation> findRegulators() {
		TypedQuery<Organisation> query = JPA.em().createNamedQuery("Organisation.findRegulators", Organisation.class);
		
		List<Organisation> results = query.getResultList();
		return results;
	}
	
	/**
	 * Gets an organisation user given its id 
	 * @param id id of the organisation user
	 * @return The OrgUser
	 */
	public static OrgUser findUser(Long id) 
	{
		TypedQuery<OrgUser> query = JPA.em().createNamedQuery("Organisation.findUser", OrgUser.class).setParameter("id",id);
		List<OrgUser> results = query.getResultList();
		
		if (results.size()>0)
			return results.get(0);

		OrgUser ou = new OrgUser();
		
		return ou;
	}
    
	/**
	 * Gets those organisations that aren't listed as regulators
	 * @return
	 */
	public static List<Organisation> findNonRegulators() {
		TypedQuery<Organisation> query = JPA.em().createNamedQuery("Organisation.findNonRegulators", Organisation.class);
		
		List<Organisation> results = query.getResultList();
		return results;
	}

	/**
	 * Gets the list of administrators for this organisation 
	 * @return
	 */
	public List<OrgUser> findAdmins() {
		TypedQuery<OrgUser> query = JPA.em().createNamedQuery("Organisation.findAdmins", OrgUser.class);
		query.setParameter("org", this);
		List<OrgUser> results = query.getResultList();
		return results;
	}
	
	/**
	 * Gets the list of users for this organisation 
	 * @return
	 */
	public List<OrgUser> findUsers() {
		List<OrgUser> results = new ArrayList<OrgUser>();
				
		try {
			TypedQuery<OrgUser> query = JPA.em().createNamedQuery("Organisation.findUsers", OrgUser.class).setParameter("id",this.getId());
		
			results = query.getResultList();
		} catch(Exception e) {
			Logger.error("org-"+e.toString());
		}
		
		return results;
	}

	/**
	 * Saves the organisation to the database
	 */
	@Transactional
    public void save() 
	{
		JPA.em().merge(this);
		Regulator reg = this.getRegulator();
		if (reg != null) {
			if (this.getAccepts_email()==null) {
				reg.setAccepts_email(false);
			} else {
				reg.setAccepts_email(this.getAccepts_email());
			}
			JPA.em().merge(reg);
		}
	}
	
	/**
	 * Gets a List of users (0 or 1) that are administrators of the organisation with the given email 
	 * address  
	 * @param email the email address
	 * @return
	 */
	public static List<Organisation> getMyAdminOrganisations(String email)
	{
		List<Organisation> results = new ArrayList<Organisation>();
		
		try 
		{
			TypedQuery<Organisation> query = JPA.em().createNamedQuery("Organisation.myAdminOrganisations", Organisation.class);
			
			query.setParameter("email", email);

			results = query.getResultList();
		}
		catch (Exception e) {
			Logger.error(e.toString());
		}
		
		return results;
	}
	/**
	 * Gets the list of organisations to which the user with the email address belongs 
	 * @param email email address of user
	 * @return 
	 */
	public static List<Organisation> getMyOrganisations(String email)
	{
		List<Organisation> results = new ArrayList<Organisation>();
		
		try 
		{
			TypedQuery<Organisation> query = JPA.em().createNamedQuery("Organisation.myOrganisations", Organisation.class);
			
			query.setParameter("email", email);

			results = query.getResultList();
		}
		catch (Exception e) {
			Logger.error(e.toString());
		}
		
		return results;
	}

	/**
	 * Gets all organisations
	 * @return
	 */
	public static List<Organisation> findAll() {
		TypedQuery<Organisation> query = JPA.em().createNamedQuery("Organisation.findAll", Organisation.class);
		
		List<Organisation> results = query.getResultList();
		return results;
	}
	@JsonIgnore
	public boolean isRegulator() {
		TypedQuery<Regulator> query = JPA.em().createNamedQuery("Organisation.findRegulator", Regulator.class);
		query.setParameter("org", this);
		List<Regulator> results = query.getResultList();
		if (results.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}
	/**
	 * Try and get the regulator associated with this organisation (null if there isn't one)
	 * @return
	 */
	@JsonIgnore
	public Regulator getRegulator() {
		TypedQuery<Regulator> query = JPA.em().createNamedQuery("Organisation.findRegulator", Regulator.class);
		query.setParameter("org", this);
		List<Regulator> results = query.getResultList();
		if (results.isEmpty()) {
			return null;
		} else {
			return results.get(0);
		}
	}
	/**
	 * Gets whether this organisation is a NoiseProducer
	 * @return
	 */
	@JsonIgnore
	public boolean isNoiseProducer() {
		TypedQuery<NoiseProducer> query = JPA.em().createNamedQuery("Organisation.findNoiseProducer", NoiseProducer.class);
		query.setParameter("org", this);
		List<NoiseProducer> results = query.getResultList();
		if (results.isEmpty()) {
			return false;
		} else {
			return true;
		}
	}
	/**
	 * Try to get the NoiseProducer associated with this organisation
	 * @return
	 */
	@JsonIgnore
	public NoiseProducer getNoiseProducer() {
		TypedQuery<NoiseProducer> query = JPA.em().createNamedQuery("Organisation.findNoiseProducer", NoiseProducer.class);
		query.setParameter("org", this);
		List<NoiseProducer> results = query.getResultList();
		if (results.isEmpty()) {
			return null;
		} else {
			return results.get(0);
		}
	}
	/**
	 * Gets a list of activityapplications by the given status
	 * @param status
	 * @return
	 */
	public List<ActivityApplication> findApplicationsByStatus(String status) {
		List<ActivityApplication> results = ActivityApplication.findByStatus(status, this);
		
		return results;
	}
	/**
	 * Adds a new user to the organisation
	 * @param au the new user
	 */
	public void addUser(AppUser au)
	{
		OrgUser newOrgUser = new OrgUser();
		newOrgUser.setAu(au);
		newOrgUser.setOrg(this);
		newOrgUser.setStatus(OrgUser.UNVERIFIED);
		newOrgUser.save();
	}
}
