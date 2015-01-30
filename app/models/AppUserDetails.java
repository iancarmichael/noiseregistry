package models;

import javax.persistence.Column;
import play.data.validation.Constraints.Email;
import play.data.validation.Constraints.MaxLength;
import play.data.validation.Constraints.Required;

public class AppUserDetails {

	@Column(length = 50)
	@Required
	@MaxLength(50)
	protected String fullname;

	@Required
	@Column(length = 50)
	@MaxLength(50)
	@Email
	protected String email_address;

	@Column(length = 20)
	@Required
	@MaxLength(20)
	protected String phone;
	

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

	public void update(Long id) {
		//set the real user details to be persisted
		AppUser.updateDetails(this, id);
	}
	
}
