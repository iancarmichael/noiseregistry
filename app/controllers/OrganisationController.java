package controllers;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import javax.mail.*;
import javax.mail.internet.*;

import models.AppUser;
import models.NoiseProducer;
import models.OrgUser;
import models.Organisation;
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

@Security.Authenticated(SecuredController.class)
public class OrganisationController extends Controller {

	static Form<Organisation> appForm = Form.form(Organisation.class);
	static Form<OrgUser> joinForm = Form.form(OrgUser.class);
	
	/**
	 * Shows the UI allowing creation of a new organisation
	 * @return page for new organisation
	 */
	public static Result add() 
	{
		Organisation org = new Organisation();
		return ok(
				organisation.render(AppUser.findByEmail(session("email")), appForm, org)
			  );
	}
	/**
	 * Allows editing of an organisation
	 * @param id the organisation to be edited
	 * @return the page containing the data to be edited
	 */
	@Transactional(readOnly=true)
	public static Result edit(String id) 
	{
		Organisation org = JPA.em().find(Organisation.class, Long.parseLong(id));
		if (userHasAdminAccessToOrganisation(org.getId().longValue()))
		{
			if (org.isRegulator()) {
				org.setAccepts_email(org.getRegulator().getAccepts_email());
			}
			Form<Organisation> f = appForm.fill(org);
			
			return ok(organisation.render(AppUser.findByEmail(session("email")), f , org));
		}
		String activeTab="HOME";
        return status(403,index.render(AppUser.findByEmail(session("email")), activeTab));
	}
	
	/**
	 * UI showing the list of organisations to which the user is the administrator
	 * @return page showing the organisations
	 */
	@Transactional(readOnly=true)
	public static Result adminorgs()
	{
		String sEmail = session("email");			

		return ok(adminorganisations.render(AppUser.findByEmail(session("email")), Organisation.getMyAdminOrganisations(sEmail)));
	}
	
	/**
	 * Shows the UI for organisation user
	 * @param id the organisation user id
	 * @return page allowing the user to be edited
	 */
	@Transactional(readOnly=true)
	public static Result getuser(String id) 
	{
		OrgUser ou = Organisation.findUser(Long.parseLong(id));

		if (userHasAdminAccessToOrganisation(ou.getOrg().getId()))
		{
			Form<OrgUser> f = Form.form(OrgUser.class).fill(ou);
			return ok(organisationuser.render(AppUser.findByEmail(session("email")), ou , ou.getOrg().getId(), f));
		}
		String activeTab="HOME";
        return status(403,index.render(AppUser.findByEmail(session("email")), activeTab)); 	// user must have admin rights to organisation
	}
	
	/**
	 * Checks whether the user is an administrator of an organisation
	 * @param lid the id of the organisation
	 * @return
	 */
	@Transactional(readOnly=true)
	private static boolean userHasAdminAccessToOrganisation(long lid)
	{
		String sEmail = request().username();

		List<Organisation> liorg = Organisation.getMyAdminOrganisations(sEmail);
		ListIterator<Organisation> it = liorg.listIterator();
		while (it.hasNext())
		{
			Organisation org = it.next();
			if (org.getId()==lid)
				return true;
		}
		return false;
	}
	
	/**
	 * Shows the UI for the organisation details
	 * @param id the id of the organisation
	 * @return page showing the organisation
	 */
	@Transactional(readOnly=true)
	public static Result read(String id)
	{
		long lid = Long.parseLong(id);
		
		if (userHasAdminAccessToOrganisation(lid))
		{
			Organisation org = JPA.em().find(Organisation.class, lid);
		
			return ok(organisationread.render(AppUser.findByEmail(session("email")), org));
		}
		String activeTab="HOME";
        return status(403,index.render(AppUser.findByEmail(session("email")), activeTab));	// user must have admin rights to organisation
	}
	/**
	 * Page showing list of noise producers
	 * @return Page showing list of noise producers
	 */
	@Transactional(readOnly=true)
	public static Result list()
	{
		AppUser au = AppUser.findByEmail(session("email"));
		List<NoiseProducer> nps = new ArrayList<NoiseProducer>();
		
		if (!au.isRegulatorMember()) {
			nps = NoiseProducer.findAll();
		}
		
		return ok(organisationselect.render(au, nps));
	}
	/**
	 * UI allowing the user to request join access
	 * @param orgId The organisation which the user is attempting to join
	 * @return page showing the request
	 */
	@Transactional(readOnly=true)
	public static Result join(Long orgId) 
	{
		AppUser au = AppUser.findByEmail(session("email"));
		Organisation org = JPA.em().find(Organisation.class, orgId);
		return ok(organisationjoin.render(au, org));
	}
	/**
	 * Request to join organisation
	 * @param orgId the id of the organisation to add the user to
	 * @return confirmation of the request
	 */
	@Transactional
	public static Result addUser(Long orgId)
	{
		String activeTab="HOME";

		Organisation org = JPA.em().find(Organisation.class, orgId);
		AppUser au = AppUser.findByEmail(session("email"));

		try {
			org.addUser(au);
			JPA.em().flush();
			
			sendAdminJoinNotification(org, au);
			
		} catch (Exception e) {
			//User may already be a member of the selected organisation?
			return badRequest(finished.render(au, "organisation.joinrequest.title", "organisation.joinrequest.errormessage", activeTab, routes.RegistrationController.read()));
		}
		
		return ok(finished.render(au, "organisation.joinrequest.title", "organisation.joinrequest.confirmmessage", activeTab, routes.RegistrationController.read()));			
		
	}
	/** 
	 * Saves the organisation details
	 * @return confirmation
	 */
	@Transactional
	public static Result save() 
	{
		Form<Organisation>  filledForm = appForm.bindFromRequest();
		if(filledForm.hasErrors()) {
			Logger.error(filledForm.errors().toString());
			Organisation org = filledForm.get();	
			return badRequest(
					organisation.render(AppUser.findByEmail(session("email")), filledForm, org)
		    );
		} else {
			Organisation org = filledForm.get();	
			//Logger.error(filledForm.toString());
			if (org!=null)
			{
				org.save();
			}
	    	String activeTab="HOME";
	        return ok(finished.render(AppUser.findByEmail(session("email")), "organisation.confirmsave.title", "organisation.confirmsave.message", activeTab, routes.OrganisationController.adminorgs()));			
		}
    }
	/**
	 * Removes a user from an organisation
	 * @return organsiation details page
	 */
	@Transactional
	public static Result deleteuser() 
	{
		Form<OrgUser> filledForm = Form.form(OrgUser.class).bindFromRequest();
		
		Map<String, String> map = filledForm.data();
		OrgUser ou = JPA.em().find(OrgUser.class, Long.parseLong((String)map.get("id")));
		
		Long lorgid = ou.getOrg().getId();
		
		sendUserRemovalMail(ou);
		
		ou.delete();
		return redirect(routes.OrganisationController.read(lorgid.toString()));
	}

	/**
	 * Saves changes to an organisation user
	 * May send email to user
	 * @return appropriate data
	 */
	@Transactional
	public static Result saveuser() 
	{
		Form<OrgUser> filledForm = Form.form(OrgUser.class).bindFromRequest();

		OrgUser ou=null;
		
		Map<String, String> map = filledForm.data();

		ou = JPA.em().find(OrgUser.class, Long.parseLong((String)map.get("id")));

		if (map.containsKey("action") && ((String)map.get("action")).compareTo("delete")==0)
		{
			return ok(organisationconfirmdelete.render(AppUser.findByEmail(session("email")), ou));				
		}

		if(filledForm.hasErrors()) {
			Logger.error(filledForm.errors().toString());
			if (userHasAdminAccessToOrganisation(ou.getOrg().getId()))
				return badRequest(organisationuser.render(AppUser.findByEmail(session("email")), ou , ou.getOrg().getId(), filledForm));
			else 
			{
				String activeTab="HOME";
		        return status(403,index.render(AppUser.findByEmail(session("email")), activeTab));			
		    }
		} else {
			if (ou!=null)
			{
				if (map.get("administrator")!=null && ((String)map.get("administrator")).compareToIgnoreCase("true")==0)
					ou.setAdministrator(true);
				else
					ou.setAdministrator(false);
				
				if (map.get("status")!=null)
				{
					if (((String)map.get("status")).compareToIgnoreCase("reject")==0)
					{
						sendRejectionToUser(ou,map);
						ou.delete();
					}
					else
					{
						if (ou.getStatus().compareToIgnoreCase((String)map.get("status"))!=0)
						{
							sendAcceptToUser(ou,map);
						}
						ou.setStatus((String)map.get("status"));
						ou.save();
					}
				}					
			}
		}
		Long lorgid = ou.getOrg().getId();
    	return redirect(routes.OrganisationController.read(lorgid.toString()));		
    }

	/**
	 * Sends rejection email to user
	 * @param ou the organisation user
	 * @param map additional data
	 */
	private static void sendRejectionToUser(OrgUser ou, Map<String, String> map) {
		AppUser au = ou.getAu();
		
		Logger.error(map.toString());
		
		Html mailBody = views.html.email.userreject.render(ou, request().host(),((String)map.get("reject_reason")));
		
		try {
			Session session = MailSettings.getSession();
			InternetAddress[] addresses = {new InternetAddress(au.getEmail_address())};
			String mailFrom = AppConfigSettings.getConfigString("sendMailFrom", "email.sendFrom");    
	        MimeMessage message = new MimeMessage(session);
	        message.setSubject(Messages.get("userreject.mail.subject"));
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

	/**
	 * Sends acceptance email to user
	 * @param ou the organisation user
	 * @param map additional data
	 */
	private static void sendAcceptToUser(OrgUser ou, Map<String, String> map) {
		AppUser au = ou.getAu();
		
		Logger.error(map.toString());
		
		Html mailBody = views.html.email.useraccept.render(ou, request().host());
		
		try {
			Session session = MailSettings.getSession();
			InternetAddress[] addresses = {new InternetAddress(au.getEmail_address())};
			String mailFrom = AppConfigSettings.getConfigString("sendMailFrom", "email.sendFrom");    
	        MimeMessage message = new MimeMessage(session);
	        message.setSubject(Messages.get("useraccept.mail.subject"));
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
	
	/**
	 * Sends the organisation administrator join confirmation
	 * @param org the organisation
	 * @param au the user
	 */
	private static void sendAdminJoinNotification(Organisation org, AppUser au) {
		List<OrgUser> admins = org.findAdmins();
		if (!admins.isEmpty()) {
				
			Html mailBody = views.html.email.userjoinrequest.render(org, au, request().host());
			
			try {       			
				Session session = MailSettings.getSession();
				String mailFrom = AppConfigSettings.getConfigString("sendMailFrom", "email.sendFrom");    
		        MimeMessage message = new MimeMessage(session);
		        message.setSubject(Messages.get("userjoinrequest.mail.subject"));
		        message.setContent(mailBody.body(), "text/html");
		        message.setFrom(new InternetAddress(mailFrom));
		        
		        //Add each admin user to the email "to" field...
		        Iterator<OrgUser> it = admins.iterator();
		        while (it.hasNext()) {      	
		        	InternetAddress address = new InternetAddress(it.next().getAu().getEmail_address());
		        	message.addRecipient(Message.RecipientType.TO, address);
		        }
		        	        
		        // set the message content here
		        Transport t = session.getTransport("smtp");
		        t.connect();
		        t.sendMessage(message, message.getAllRecipients());
		        t.close();
			 } catch (MessagingException me) {
			        me.printStackTrace();
			 }
		}
	}
	/**
	 * Sends the user a removal confirmation email
	 * @param ou the organisation user
	 */
	public static void sendUserRemovalMail(OrgUser ou) 
	{
		AppUser au = ou.getAu();
		Html mailBody = views.html.email.userremovalmail.render(ou, request().host());
		Html mailBodyAdmin = views.html.email.userremovalmailadmin.render(ou, request().host());
		
		try {
			Session session = MailSettings.getSession();
			InternetAddress[] addresses = {new InternetAddress(au.getEmail_address())};
			String mailFrom = AppConfigSettings.getConfigString("sendMailFrom", "email.sendFrom");  
	        
	        MimeMessage message = new MimeMessage(session);
	        message.setSubject(Messages.get("userremoval.mail.subject"));
	        message.setContent(mailBody.body(), "text/html");
	        message.setRecipients(Message.RecipientType.TO, addresses);				        
	        message.setFrom(new InternetAddress(mailFrom));
	        
	        MimeMessage messageAdmin = new MimeMessage(session);
	        messageAdmin.setSubject(Messages.get("userremovaladmin.mail.subject"));
	        messageAdmin.setContent(mailBodyAdmin.body(), "text/html");
	        List<OrgUser> lau = au.getOrgAdmins(ou);
	        InternetAddress[] adminaddresses = new InternetAddress[lau.size()];
	        ListIterator<OrgUser> it = lau.listIterator();
	        int i = 0;
	        while (it.hasNext())
	        {
	        	OrgUser ouA = it.next();
	        	adminaddresses[i++]=new InternetAddress(ouA.getAu().getEmail_address());
	        }
	        messageAdmin.setRecipients(Message.RecipientType.TO, adminaddresses);				        
	        messageAdmin.setFrom(new InternetAddress(mailFrom));
	        
	        // set the message content here
	        Transport t = session.getTransport("smtp");
	        t.connect();
	        t.sendMessage(message, message.getAllRecipients());
	        t.sendMessage(messageAdmin, messageAdmin.getAllRecipients());
	        t.close();
		 } catch (MessagingException me) {
		        me.printStackTrace();
		 }
	}
}
