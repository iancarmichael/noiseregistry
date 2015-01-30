package controllers;

import models.AppUserRegistration;
import models.AppUserDetails;
import models.AppUser;
import play.Logger;
import play.data.Form;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.*;
import play.twirl.api.Html;
import utils.AppConfigSettings;
import utils.MailSettings;
import views.html.*;

import javax.mail.*;
import javax.mail.internet.*;

public class RegistrationController extends Controller {

	static Form<AppUserRegistration> userForm = Form.form(AppUserRegistration.class);
	static Form<AppUserDetails> detailsForm = Form.form(AppUserDetails.class);
	
	@Transactional
	public static Result add()
	{
		//Get the form object...
		return ok(
			    userform.render(AppUser.findByEmail(session("email")), userForm, Messages.get("userform.title_new"))
			  );
	}
	
	@Transactional
	public static Result save()
	{
		Form<AppUserRegistration>  filledForm = userForm.bindFromRequest();
		if(filledForm.hasErrors()) {
			return badRequest(
				userform.render(AppUser.findByEmail(session("email")), filledForm, Messages.get("userform.title_new"))
		    );
		} else {
			try {
				String sEmail = filledForm.get().getEmail_address();
				if (sEmail!=null)
				{
					AppUser au = AppUser.findByEmail(sEmail);
					if (au!=null)
					{
						filledForm.reject(Messages.get("userform.save.failed"));
						return badRequest(
								userform.render(AppUser.findByEmail(session("email")), filledForm, Messages.get("userform.title_new"))
					    );						
					}
				}
				//storeDetails(filledForm);
				AppUserRegistration aur = filledForm.get();
				aur.save();
				//flush the transaction so that any exceptions from the db layer are raised...
				JPA.em().flush();
				
				//Send the user a verification email...
				AppUser au = AppUser.findByEmail(aur.getEmail_address());
				sendVerificationMail(au);			
				return redirect(routes.RegistrationController.confirmadd(Long.toString(au.getId())));
				
			} catch (Exception e) {
				Logger.error(e.getMessage());

				filledForm.reject(Messages.get("userform.save.failed"));
				return badRequest(
						userform.render(AppUser.findByEmail(session("email")), filledForm, Messages.get("userform.title_new"))
			    );
			}
			
		}
	}
	
	public static void sendVerificationMail(AppUser au) 
	{
		Html mailBody = views.html.email.verificationmail.render(au, request().host());
		try {
			Session session = MailSettings.getSession();
			InternetAddress[] addresses = {new InternetAddress(au.getEmail_address())};
			String mailFrom = AppConfigSettings.getConfigString("sendMailFrom", "email.sendFrom");
	        MimeMessage message = new MimeMessage(session);
	        message.setSubject(Messages.get("registration.mail.subject"));
	        message.setContent(mailBody.body(), "text/html");
	        message.setRecipients(Message.RecipientType.TO, addresses);
	        message.setFrom(new InternetAddress(mailFrom));
	        // set the message content here
	        Transport t = session.getTransport("smtp");
	        t.connect();
	        t.sendMessage(message, message.getAllRecipients());
	        t.close();
		 } catch (MessagingException me) {
		        me.printStackTrace();
		 }
	}
	
	@Transactional
	public static Result verify(String ver_token)
	{
		//Look for the verification token in the unverified user records
		boolean res = AppUser.verify(ver_token);
		if (res) {
	    	String activeTab="HOME";
	        return ok(views.html.finished.render(AppUser.findByEmail(session("email")), "verification.successpage_title", "verification.success", activeTab, routes.ApplicationController.home()));
		} else {
	    	String activeTab="HOME";
	        return ok(views.html.finished.render(AppUser.findByEmail(session("email")), "verification.errorpage_title", "verification.error", activeTab, routes.ApplicationController.home()));

		}
	}
	
	@Transactional(readOnly=true)
	public static Result confirmadd(String id)
	{
    	String activeTab="HOME";
        return ok(views.html.finished.render(AppUser.findByEmail(session("email")), "registration.confirmation_title", "registration.success", activeTab, routes.ApplicationController.home()));
        
	}
	
	@Transactional
	@Security.Authenticated(SecuredController.class)
	public static Result edit()
	{
		AppUser au = AppUser.findByEmail(request().username());
		Form<AppUserDetails> filledForm = detailsForm.fill(au.toAppUserDetails());
	    return ok(
	            mydetails.render(AppUser.findByEmail(session("email")), filledForm, Messages.get("userform.title_mydetails"), au.getId())
	        );
	}
	
	@Transactional(readOnly=true)
	@Security.Authenticated(SecuredController.class)
	public static Result read()
	{
		AppUser au = AppUser.findByEmail(request().username());
		return ok(userread.render(au));
	}
	
	@Transactional
	public static Result update(Long id)
	{
		Form<AppUserDetails>  filledForm = detailsForm.bindFromRequest();
		if(filledForm.hasErrors()) {
			Logger.error("Submitted form contained errors.");
			return badRequest(
					mydetails.render(AppUser.findByEmail(session("email")), filledForm, Messages.get("userform.title_mydetails"), id)
		    );
		} else {
			filledForm.get().update(id);
			session("email", filledForm.get().getEmail_address());
			return redirect(routes.RegistrationController.confirmupdate());
		}	
	}
	
	@Transactional(readOnly=true)
	public static Result confirmupdate()
	{
    	String activeTab="HOME";
        return ok(views.html.finished.render(AppUser.findByEmail(session("email")), "changeuserdetails.confirmation_title", "changeuserdetails.success", activeTab, routes.RegistrationController.read()));

	}
}
