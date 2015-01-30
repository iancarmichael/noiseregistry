package models;

import java.util.ArrayList;
import java.util.List;

import play.Logger;
import play.data.validation.ValidationError;
import play.i18n.Messages;

public class AppUserResetPassword {

	protected String email;
	
	public String getEmail() {
		return email;
	}
	public void setEmail(String email) {
		this.email = email;
	}
	
	
	public String resetPassword() {
		String res = AppUser.resetPassword(this.getEmail());
		return res;
	}
	
	public List<ValidationError> validate() {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		try
		{	
			if (AppUser.findVerifiedByEmail(email) == null) {
				errors.add(new ValidationError("",  Messages.get("resetpasswordform.error.nouser")));
			}
			
		} catch (Exception e) {
			Logger.error(e.getMessage());
			errors.add(new ValidationError("",  Messages.get("error.something.went.wrong")));
		}
			
		return errors.isEmpty() ? null : errors;
	}
}
