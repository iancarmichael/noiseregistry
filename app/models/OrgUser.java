package models;

import java.util.ArrayList;
import java.util.List;

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
import javax.persistence.TypedQuery;
import javax.persistence.UniqueConstraint;

import org.hibernate.annotations.NamedNativeQueries;
import org.hibernate.annotations.NamedNativeQuery;

import play.Logger;
import play.data.validation.ValidationError;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.i18n.Messages;

@NamedNativeQueries({
	@NamedNativeQuery(name="OrgUser.findOU",
			query="select * from orguser where appuser_id = :appuser_id and organisation_id = :org_id",
			resultClass=OrgUser.class),
	@NamedNativeQuery(name="OrgUser.findOrgs",
	query="select * from orguser left join organisation on organisation.id = orguser.organisation_id where appuser_id = :appuser_id order by organisation.organisation_name",
	resultClass=OrgUser.class)
})

@Entity
@Table(name="orguser", uniqueConstraints=
	@UniqueConstraint(columnNames = {"appuser_id", "organisation_id"})
)
public class OrgUser
{
	public static final String VERIFIED = "verified";
	public static final String UNVERIFIED = "unverified";
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "orguser_seq_gen")
    @SequenceGenerator(name = "orguser_seq_gen", sequenceName = "orguser_id_seq")
    @Column(columnDefinition = "serial")
    protected Long id;   
        
    @OneToOne(optional=false)
    @JoinColumn(name = "organisation_id")
    protected Organisation org; 
    
	@OneToOne(optional=false)
    @JoinColumn(name="appuser_id",referencedColumnName="id")
    protected AppUser au;
	
	protected boolean administrator;

	@Column(length=20)
	protected String status;
	
	@Transient
	protected String reject_reason;
	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public boolean isAdministrator() {
		return administrator;
	}

	public void setAdministrator(boolean administrator) {
		this.administrator = administrator;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Organisation getOrg() {
		return org;
	}

	public void setOrg(Organisation org) {
		this.org = org;
	}

	public AppUser getAu() {
		return au;
	}

	public void setAu(AppUser au) {
		this.au = au;
	}
	
	public String getReject_reason() {
		return reject_reason;
	}

	public void setReject_reason(String reject_reason) {
		this.reject_reason = reject_reason;
	}

	@Transactional
    public void save() 
	{
		JPA.em().merge(this);
	}
	@Transactional
    public void delete() 
	{
		JPA.em().remove(this);
		// we're not deleting the attached users as this was deemed unwanted 
	}
	public static OrgUser findOU(Long appuser_id, Long org_id)
	{
		List<OrgUser> results = new ArrayList<OrgUser>();
		
		try 
		{
			TypedQuery<OrgUser> query = JPA.em().createNamedQuery("OrgUser.findOU", OrgUser.class);
			
			query.setParameter("appuser_id", appuser_id);
			query.setParameter("org_id", org_id);

			results = query.getResultList();
			return results.get(0);
		}
		catch(Exception e) 
		{
			// just return null
		}
		return null;
	}
	public static List<OrgUser> findOrgs(Long appuser_id)
	{
		List<OrgUser> results = new ArrayList<OrgUser>();
		
		try 
		{
			TypedQuery<OrgUser> query = JPA.em().createNamedQuery("OrgUser.findOrgs", OrgUser.class);
			
			query.setParameter("appuser_id", appuser_id);

			results = query.getResultList();
			
			return results;
		}
		catch(Exception e) 
		{
			// just return null
		}
		return null;
	}
	
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		try
		{
			//Saving a rejection must include a reason
			if (this.getStatus().equalsIgnoreCase("reject")) {
				if (this.getReject_reason()==null || this.getReject_reason().trim().equals("")) {
					errors.add(new ValidationError("reject_reason",  "validation.required"));
				}
			}
		}
		catch (Exception e)
		{
			Logger.error(e.getMessage());
			errors.add(new ValidationError("",  Messages.get("error.something.went.wrong")));
		}
			
		return errors.isEmpty() ? null : errors;
	}
}
