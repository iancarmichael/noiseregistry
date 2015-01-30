package models;

import java.util.ArrayList;
import java.util.List;

import play.Logger;
import play.data.validation.Constraints.MinLength;
import play.data.validation.Constraints.Pattern;
import play.data.validation.Constraints.Required;
import play.data.validation.ValidationError;
import play.i18n.Messages;

public class AppUserChangePassword {
	@Required
	protected String email_address;
	@Required
	@MinLength(8)
	@Pattern(value="((?=.*\\d)(?=.*[A-Z])(.)*)", message="error.passwords_strength")
	protected String password_entry;
	@Required
	protected String password_confirm;
	@Required
	protected String current_password;

	public String getEmail_address() {
		return email_address;
	}

	public void setEmail_address(String email_address) {
		this.email_address = email_address;
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

	public String getCurrent_password() {
		return current_password;
	}

	public void setCurrent_password(String current_password) {
		this.current_password = current_password;
	}
	
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		try
		{	
			if (email_address==null) {
				errors.add(new ValidationError("",  Messages.get("changepasswordform.error.notloggedin")));
			} else {
				//Validate that the current password is valid, then check for new password validation...
				if (AppUser.authenticate(email_address, current_password) == null) {
	            	errors.add(new ValidationError("",  Messages.get("changepasswordform.error.invalid")));
	            } else {
	            	if (!this.getPassword_entry().equals(this.getPassword_confirm())) {
	    				errors.add(new ValidationError("", "error.passwords_must_match"));
	    			}
	            }
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
			errors.add(new ValidationError("", e.getMessage()));
		}
		return errors.isEmpty() ? null : errors;
    }

	public void update() {
		//set the real password value to be persisted (entry and confirm values are transient)
		AppUser.updatePassword(this);
	}
}
