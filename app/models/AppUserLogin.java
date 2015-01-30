package models;

import com.fasterxml.jackson.annotation.JsonIgnore;

import play.Logger;
import play.i18n.Messages;


public class AppUserLogin {

	protected String email;
	protected String password;
	@JsonIgnore
	protected String redirectTo;
	
    protected AppUser au;

    public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	
	public String getRedirectTo() {
		return redirectTo;
	}

	public void setRedirectTo(String redirectTo) {
		this.redirectTo = redirectTo;
	}

	@JsonIgnore
	public AppUser getAu() {
		return au;
	}

	protected void setAu(AppUser au) {
		this.au = au;
	}

	public String validate() {
		this.au = AppUser.authenticate(this.email, this.password);
        if (this.au == null) {
        	//Log the failed authentication attempt...
        	Logger.error("Invalid authentication attempt for user: " + this.email);
            return Messages.get("loginform.error.invalid");
        }
        return null;
    }

}
