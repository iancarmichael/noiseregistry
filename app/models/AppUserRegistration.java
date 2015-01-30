package models;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.Transient;

import play.Logger;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Pattern;
import play.data.validation.ValidationError;
import play.data.validation.Constraints.Required;
import play.i18n.Messages;

/**
 * @author david.simpson
 *
 */
/**
 * @author david.simpson
 *
 */

public class AppUserRegistration
{
	@Column(length = 50)
	@MaxLength(50)
	@Required
	protected String fullname;

	@Required
	@MaxLength(50)
	@Email
	@Column(length = 50)
	protected String email_address;

	@Column(length = 20)
	@MaxLength(20)
	@Required
	protected String phone;
	
	@Transient
	@Required
	@MinLength(8)
	@Pattern(value="((?=.*\\d)(?=.*[A-Z])(.)*)", message="error.passwords_strength")
    protected String password_entry;
    
    @Transient
    @Required
    protected String password_confirm;
    
    
    public String getFullname() {
		return fullname;
	}
	public void setFullname(String fullname) {
		this.fullname = fullname;
	}
	public String getEmail_address() {
		return email_address;
	}
	public void setEmail_address(String email_address) {
		this.email_address = email_address;
	}
	public String getPhone() {
		return phone;
	}
	public void setPhone(String phone) {
		this.phone = phone;
	}
	public String getPassword_entry() {
		return password_entry;
	}
	public void setPassword_entry(String password_entry) {
		this.password_entry = password_entry;
	}
	public String getPassword_confirm() {
		return password_confirm;
	}
	public void setPassword_confirm(String password_confirm) {
		this.password_confirm = password_confirm;
	}
	
	/**
	 * validate
	 * 
	 * Custom validation for the AppUser entity.
	 * 
	 */
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		try
		{	
			if (!this.getPassword_entry().equals(this.getPassword_confirm())) {
				errors.add(new ValidationError("password_confirm", "error.passwords_must_match"));
			}
		}
		catch (Exception e)
		{
			Logger.error(e.getMessage());
			errors.add(new ValidationError("",  Messages.get("error.something.went.wrong")));
		}
		
		return errors.isEmpty() ? null : errors;
    }
	
	public void save() {
		AppUser.saveRegistration(this);
	}
}
