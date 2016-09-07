package controllers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiImplicitParam;
import com.wordnik.swagger.annotations.ApiImplicitParams;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

import play.Logger;
import play.data.Form;
import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.*;
import play.twirl.api.Html;
import play.libs.Json;
import utils.AppConfigSettings;
import utils.MailSettings;
import views.html.*;
import models.*;
import play.data.validation.*;

@Security.Authenticated(SecuredController.class)
@Api(value = "/aas", description = "Operations on Activity Applications (aas)")
public class ActivityApplicationController extends Controller {
	
	static Form<ActivityApplication> appForm = Form.form(ActivityApplication.class);
	static Form<ActivityApplicationCloseOut> appcloseoutForm = Form.form(ActivityApplicationCloseOut.class);
	
	/**
	 * List of applications
	 * @return page with list of applications
	 */
	@Transactional(readOnly=true)
	public static Result index()
	{
		AppUser au = AppUser.getSystemUser(request().username());
	    
		String status="Proposed,Interim Close-out";
		List<String> statusVals = Arrays.asList(status.split(","));
		
		Comparator<ActivityApplication> comparator;
		if (au.isRegulatorMember()) {
			comparator = new ActivityApplication.DateDueComparator();
		} else {
			comparator = new ActivityApplication.NoiseProducerComparator();
		}
		
		if (request().accepts("text/html")) {
			//The default list is for noise producer users...  to handle regulators we need a different sort order...
			return ok(activities.render(au, au.findApplicationsByStatus(status, comparator), statusVals));
		} else if (request().accepts("application/json")) {
			return ok(Json.toJson(au.findApplicationsByStatus(status, comparator)));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
	/**
	 * List of drafts
	 * @return page with list of drafts
	 */
	@Transactional(readOnly=true)
	public static Result findDraft()
	{
		AppUser au = AppUser.getSystemUser(request().username());
		if (au.isRegulatorMember()) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		}
		String status="Draft";
		List<String> statusVals = Arrays.asList(status.split(","));
		
		Comparator<ActivityApplication> comparator;
		comparator = new ActivityApplication.NoiseProducerComparator();
		if (request().accepts("text/html")) {
			return ok(activities.render(au, au.findApplicationsByStatus(status, comparator), statusVals));
		} else if (request().accepts("application/json")) {
			return ok(Json.toJson(au.findApplicationsByStatus(status, comparator)));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
	/**
	 * List of completed applications
	 * @return page with list of completed applications
	 */
	@Transactional(readOnly=true)
	public static Result findCompleted()
	{
		AppUser au = AppUser.getSystemUser(request().username());
		
		String status="Closed,Cancelled";
		List<String> statusVals = Arrays.asList(status.split(","));
		Comparator<ActivityApplication> comparator;
		if (au.isRegulatorMember()) {
			comparator = new ActivityApplication.DateClosedComparator();
		} else {
			comparator = new ActivityApplication.NoiseProducerComparator();
		}
		if (request().accepts("text/html")) {
			return ok(activities.render(au, au.findApplicationsByStatus(status, comparator), statusVals));
		} else if (request().accepts("application/json")) {
			return ok(Json.toJson(au.findApplicationsByStatus(status, comparator)));
		} else {
			return badRequest("Unsupported content type");
		}
	}

	/**
	 * Fins all applications with the given status that are allowed to be seen by the user
	 * @param status
	 * @return List of applications with the given status
	 */
	@Transactional(readOnly=true)
	@ApiOperation(value = "Finds Activity Applications by Status",
            notes = "Returns all Activity Applications with a Status value matching the parameter.  Multiple comma-separated status can be used.",
            nickname = "findByStatus",
            response = ActivityApplication.class,
            responseContainer = "List", httpMethod = "GET")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid status value")})
	public static Result findByStatus(
			@ApiParam(value = "Status value for filter", 
				required = true, 
				defaultValue = "Proposed", 
				allowableValues = "Proposed,Cancelled,Interim Close-out,Closed", allowMultiple = true) 
			@QueryParam("status") String status)
	{
		AppUser au = AppUser.getSystemUser(request().username());

		List<String> statusVals = Arrays.asList(status.split(","));
		
		if (request().accepts("text/html")) {
			return ok(activities.render(au, au.findApplicationsByStatus(status, null), statusVals));
		} else if (request().accepts("application/json")) {
			return ok(Json.toJson(au.findApplicationsByStatus(status, null)));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
	/**
	 * Shows the UI for entering a new application
	 * @return Page allowing entry of new application
	 */
	@Transactional(readOnly=true)
	public static Result add()
	{
		AppUser au = AppUser.getSystemUser(request().username());
		
		if (request().accepts("text/html")) {
			//Prevent caching the form (doesn't work for the current version of Chrome v40.0)
			response().setHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate");
			response().setHeader(PRAGMA, "no-cache, no-store");
			response().setHeader(EXPIRES, "0");
			
			return ok(activityform.render(au, appForm, Messages.get("activityform.title_new"), null, null));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
	/**
	 * Show the UI for editing an existing application
	 * @param id the id of the application to be edited 
	 * @return page with the application
	 */
	@Transactional(readOnly=true)
	public static Result edit(Long id)
	{
		AppUser au = AppUser.getSystemUser(request().username());
		
		ActivityApplication aa = ActivityApplication.findById(id);
		if (aa==null || au.isRegulatorMember()) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		}
		Form<ActivityApplication> filledForm = appForm.fill(aa);
		
		if (request().accepts("text/html")) {
			//Prevent caching the form (doesn't work for the current version of Chrome v40.0)
			response().setHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate");
			response().setHeader(PRAGMA, "no-cache, no-store");
			response().setHeader(EXPIRES, "0");
			
			return ok(activityform.render(au, filledForm, Messages.get("activityform.title"), id, null));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
	/**
	 * Shows the UI for creating a linked application
	 * @param id the id of the application to be linked to
	 * @return the page allowing creation of the linked application
	 */
	@Transactional(readOnly=true)
	public static Result createLinked(Long id)
	{
		AppUser au = AppUser.getSystemUser(request().username());
		
		ActivityApplication aa = ActivityApplication.findById(id);
		
		if (aa==null || au.isRegulatorMember()) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		}
		ActivityApplication linked = aa.createLinked();
		
		Form<ActivityApplication> filledForm = appForm.fill(linked);
		
		if (request().accepts("text/html")) {
			return ok(activityform.render(au, filledForm, Messages.get("activityform.title"), null, aa));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
	/**
	 * Shows the UI asking users to confirm the deletion of the application
	 * @param id the application about to be deleted
	 * @return the page asking for confirmation
	 */
	@Transactional(readOnly=true)
	public static Result preDelete(Long id)
	{
		AppUser au = AppUser.getSystemUser(request().username());
		ActivityApplication aa = ActivityApplication.findById(id);
		if (aa==null) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		}
		if (request().accepts("text/html")) {
			return ok(activityformdelete.render(au, aa));
		} else {
			return badRequest("Unsupported content type"); 
		}
	}
	
	/**
	 * Delete an application
	 * @param id the id of the application to be deleted
	 * @return appropriate result
	 */
	@Transactional
	public static Result delete(Long id)
	{
		AppUser au = AppUser.getSystemUser(request().username());
		ActivityApplication aa = ActivityApplication.findById(id);
		if (aa==null) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		}
		try {
			aa.delete();
			if (request().accepts("text/html")) {
				return redirect(routes.ActivityApplicationController.findDraft());
			} else if (request().accepts("application/json")) {
				return status(204);
			} else {
				return badRequest("Unsupported content type");
			}
		} catch (Exception e) {
			return badRequest(e.getMessage());
		}
			
	}
	/**
	 * Shows the confirmation page that an application has been added
	 * @param id the application id added
	 * @return the page with the confirmation details
	 */
	@Transactional(readOnly=true)
	public static Result confirmadd(String id)
	{
		AppUser au = AppUser.getSystemUser(request().username());
		
		ActivityApplication aa = ActivityApplication.findById(Long.parseLong(id));
		if (aa==null) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		}
		return ok(
		    activityformread.render(au, aa, "confirmadd")
		);
	}
	
	/**
	 * Gets an activity application
	 * @param id the id of the application to be returned
	 * @return appropriate data
	 */
	@Transactional(readOnly=true)
	@ApiOperation(value = "Get Activity Application by ID",
	            notes = "Returns an Activity Application",
	            nickname = "getById",
	            response = ActivityApplication.class, httpMethod = "GET")
	@ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid ID supplied"),
	            @ApiResponse(code = 404, message = "Activity Application not found")})
	public static Result read(
			@ApiParam(value = "Id value that identifies the Activity Application", 
			required = true, 
			allowMultiple = false) 
		@PathParam("id") Long id)
	{
		AppUser au = AppUser.getSystemUser(request().username());
	    
		ActivityApplication aa = ActivityApplication.findById(id);
		
		if (aa==null) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		}
		if (request().accepts("text/html")) {
			return ok(activityformread.render(au, aa , "read"));
		} else if (request().accepts("application/json")) {
			return ok(Json.toJson(aa));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
	/**
	 * Shows the UI allowing a user to cancel an application
	 * @param id the application to be cancelled
	 * @return the page allowing the user to cancel
	 */
	@Transactional
	public static Result cancel(String id)
	{
		AppUser au = AppUser.getSystemUser(request().username());
		ActivityApplication aa = ActivityApplication.findById(Long.parseLong(id));
		if (aa==null) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		}
		return ok(activityformcancel.render(au, aa));
	}
	
	/**
	 * Gets an application to close
	 * @param id the application to be closed
	 * @return appropriate data
	 */
	@Transactional(readOnly = true)
	@ApiOperation(value = "Get Activity Application for close out by ID",
    	notes = "Returns an Activity Application for close out",
    	nickname = "getById",
    	response = ActivityApplicationCloseOut.class, httpMethod = "GET")
	@ApiResponses(value = {@ApiResponse(code = 400, message = "Invalid ID supplied"),
    @ApiResponse(code = 404, message = "Activity Application not found")})
	public static Result closeOut(
			@ApiParam(value = "Id value that identifies the Activity Application", 
				required = true, 
				allowMultiple = false) 
		@PathParam("id") Long id) 
	{
		AppUser au = AppUser.getSystemUser(request().username());
		ActivityApplication aa = ActivityApplication.findById(id);
		if (aa==null) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		}
		
		if (aa.getStatus().equals("Proposed") || aa.getStatus().equals("Interim Close-out")) {
			//populate the close out partial model
			ActivityApplicationCloseOut aaco = ActivityApplicationCloseOut.findById(aa.getId());
			aaco.setActivitylocations(aa.getActivitylocations());
			aaco.populateActivityDefaults();

			Form<ActivityApplicationCloseOut> filledForm = appcloseoutForm.fill(aaco);
			
			if (request().accepts("text/html")) {
				return ok(activityformcloseout.render(au, aa, filledForm, id));
			} else if (request().accepts("application/json")) {
				return ok(Json.toJson(aaco));
			} else {
				return badRequest("Unsupported content type");
			}
		} else {
			return notFound();
		}
	}
	
	protected static void actualsValidate(Form<ActivityApplicationCloseOut> filledForm, Form<ActivityApplication> frm)
	{
		Map<String, List<ValidationError>> mve = frm.errors();
		for (Entry<String, List<ValidationError>> entry : mve.entrySet()) 
		{
			Logger.error(entry.getKey());
			if (entry.getKey().contains("_actual"))
			{
				List<ValidationError> lve = entry.getValue();
				Iterator<ValidationError> it = lve.iterator();
				while (it.hasNext())
				{
					ValidationError ve = it.next();
					filledForm.reject(ve);
				}
			}
		}
	}
	/**
	 * Closes the application
	 * @param id the application to be closed
	 * @return appropriate data
	 */
	@Transactional
	@ApiOperation(value="Close out Activity Application",
	notes = "Closes out an Activity Application using the data supplied (if valid). The parameter 'interimcloseout' must be non-null if the update is an interim close out, otherwise the ActivityApplication status will be set to closed.",
	nickname = "closeoutAA",
	response = ActivityApplicationCloseOut.class, httpMethod = "POST")
	@ApiImplicitParams({
		@ApiImplicitParam(name = "body", value = "Activity Application details", required = true, dataType="models.ActivityApplicationCloseOut", paramType = "body"),
	})
	@ApiResponses(value = {@ApiResponse(code = 405, message = "Invalid input")})
	public static Result closeOutApplication(@ApiParam(value = "Id value that identifies the Activity Application", 
			required = true, 
			allowMultiple = false) 
	@PathParam("id") Long id)
	{
		AppUser au = AppUser.getSystemUser(request().username());
		ActivityApplication aa = ActivityApplication.findById(id);
		if (aa==null) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		}
		
		if (aa.getStatus().equals("Proposed") || aa.getStatus().equals("Interim Close-out")) {
			//set the closing indicator to allow validation checks based on status during the bindFromRequest call...
			aa.setClosing(true);
			Form<ActivityApplicationCloseOut> filledForm = appcloseoutForm.bindFromRequest();
			
			try {
				if (filledForm.get().getActivitylocations() == null || filledForm.get().getActivitylocations().isEmpty()) {
					filledForm.reject(new ValidationError("", Messages.get("validation.closeout.locations.empty")));
				}
			} catch(Exception ex) {
				filledForm.reject(new ValidationError("", Messages.get("validation.closeout.locations.error")));
			}
			
			if (filledForm.hasErrors()) 
			{
				if (request().accepts("text/html")) {
					return badRequest(activityformcloseout.render(au, aa, filledForm, id));
				} else if (request().accepts("application/json")) {
	        		return badRequest(filledForm.errorsAsJson());
	        	} else {
	        		return badRequest("Unsupported content type");
	        	}
			} else {
				try {
					filledForm.get().closeOut(id, filledForm.data());

					JPA.em().flush();  //make sure any persistence errors are raised before emailing

					sendRegulatorNotification(aa, aa.getStatus().toLowerCase().replace(' ', '_'));
					if (request().accepts("text/html")) {
						return redirect(routes.ActivityApplicationController.confirmadd(Long.toString(id)));
					} else if (request().accepts("application/json")) {
		        		return redirect(routes.ActivityApplicationController.read(id));
		        	} else {
		        		return badRequest("Unsupported content type");
		        	}
				} catch (Exception e) {
					return badRequest(activityformcloseout.render(au, aa, filledForm, id));
				}
			}
		} else {
			String activeTab="HOME";
			return status(404,index.render(au, activeTab));
		}
		
	}
	
	/** 
	 * UI part of the application cancellation
	 * @return page following cancellation
	 */
	@Transactional
	public static Result cancelApplication()
	{
		Form<ActivityApplication>  filledForm = appForm.bindFromRequest();
		Map<String,String> map = filledForm.data();
		String sId = (String)map.get("id"); 
		Long id = Long.parseLong(sId);
		
		return cancelApplicationById(id);
	}
		
	/**
	 * cancels an application (used for both UI and REST)
	 * @param id the id of the application to cancel
	 * @return appropriate data
	 */
	@Transactional
	@ApiOperation(value = "Cancel Activity Application by ID",
	nickname = "cancel",
	response = ActivityApplicationCloseOut.class, httpMethod = "POST")
	@ApiResponses(value = {@ApiResponse(code = 404, message = "Activity Application not found")})
	public static Result cancelApplicationById(
			@ApiParam(value = "Id value that identifies the Activity Application", 
			required = true, 
			allowMultiple = false) 
	@PathParam("id") Long id) 
	{
		AppUser au = AppUser.getSystemUser(request().username());
		ActivityApplication aa = ActivityApplication.findById(id);
		if (aa==null) {
			String activeTab="HOME";
	        return status(403,index.render(au, activeTab));
		} else {
			aa.cancel();
		}
		JPA.em().flush();  //make sure any persistence errors are raised before emailing
		sendRegulatorNotification(aa, aa.getStatus().toLowerCase());
		if (request().accepts("text/html")) {
			return redirect(routes.ActivityApplicationController.index());
		} else if (request().accepts("application/json")) {
    		return redirect(routes.ActivityApplicationController.read(id));
    	} else {
    		return badRequest("Unsupported content type");
    	}
	}
	
	/**
	 * Saves an application
	 * @return appropriate data
	 */
	@Transactional
	@ApiOperation(value="Create Activity Application",
	notes = "Creates a new Activity Application using the data supplied (if valid).  The following fields are read-only and should be excluded from any POST: all id values, all aa values, status, date_due, date_closed.  To create a linked application, specify the id of the parent in an additional parameter 'parent.id' (not shown in the included schema).",
	nickname = "createAA",
	response = ActivityApplication.class, httpMethod = "POST")
		@ApiImplicitParams({
			@ApiImplicitParam(name = "body", value = "Activity Application details", required = true, dataType="models.ActivityApplication", paramType = "body"),
		})
		@ApiResponses(value = {@ApiResponse(code = 405, message = "Invalid input")})
	public static Result save()
	{
		AppUser au = AppUser.getSystemUser(request().username());		
		Form<ActivityApplication> filledForm = appForm.bindFromRequest();

		if(filledForm.hasErrors()) {
			if (request().accepts("text/html")) 
			{
				ActivityApplication aaParent = null;
				
				if (filledForm.data().get("parent.id") != null && !filledForm.data().get("parent.id").isEmpty())
				{
					Long lPar = Long.parseLong((String)filledForm.data().get("parent.id"));
					aaParent = ActivityApplication.findById(lPar);
				}

				return badRequest(
						activityform.render(au, filledForm, Messages.get("activityform.title_new"), null, aaParent)
			    );
			} else if (request().accepts("application/json")) {
        		return badRequest(filledForm.errorsAsJson());
        	} else {
        		return badRequest("Unsupported content type");
        	}
			
		} else {
			ActivityApplication aa = filledForm.get();
			
			if (aa.getParent() != null && aa.getParent().getId() == null) {
				aa.setParent(null);
			}
			
			if (aa!=null)
			{
				aa.save();
			}
			JPA.em().flush();  //make sure any persistence errors are raised before emailing
			//if this is a newly proposed item, then send a mail...
			if (aa.getStatus().equals("Proposed")) {
				sendRegulatorNotification(aa, aa.getStatus().toLowerCase());
			}
			if (request().accepts("text/html")) {
				return redirect(routes.ActivityApplicationController.confirmadd(Long.toString(aa.getId())));
			} else if (request().accepts("application/json")) {
				return redirect(routes.ActivityApplicationController.read(aa.getId()));
        	} else {
        		return badRequest("Unsupported content type");
        	}	
		}
	}
	
	/**
	 * Emails the regulator
	 * @param aa the ActivityApplication
	 * @param statusKey status of the application
	 */
	public static void sendRegulatorNotification(ActivityApplication aa, String statusKey)
	{
		try {
			Regulator reg = JPA.em().find(Regulator.class,  aa.getRegulator().getId());
			NoiseProducer np = JPA.em().find(NoiseProducer.class,  aa.getNoiseproducer().getId());
			
			if (reg.getAccepts_email().booleanValue()) {
				if (Messages.get("regulator.activity." + statusKey + ".mail.subject").equals("regulator.activity." + statusKey + ".mail.subject")) {
					//no message translation - don't try to send mail out in this case...
					Logger.error("No mail settings found for application status value: " + statusKey);
					return;
				}
			
				Session session = MailSettings.getSession();
				String regulatorEmail = reg.getOrganisation().getContact_email();
						
				String mailFrom = AppConfigSettings.getConfigString("sendMailFrom", "email.sendFrom");
				String overrideAddress = AppConfigSettings.getConfigString("regulatorOverrideAddress", "email.regulator_override_address");
				InternetAddress[] addresses = new InternetAddress[1];
				
				if (overrideAddress.equals("")) {
					addresses[0] = new InternetAddress(regulatorEmail);
				} else {
					Logger.error("Regulator email override is in effect - email will not be sent to the regulator address");
					addresses[0] = new InternetAddress(overrideAddress);
				}
				Html mailBody = views.html.email.regulatoractivity.render(aa, request().host(), statusKey, overrideAddress, reg, np);
				
		        MimeMessage message = new MimeMessage(session);
		        message.setSubject(Messages.get("regulator.activity." + statusKey + ".mail.subject"));
		        message.setContent(mailBody.body(), "text/html");
		        message.setRecipients(Message.RecipientType.TO, addresses);
		        message.setFrom(new InternetAddress(mailFrom));
		        // set the message content here
		        Transport t = session.getTransport("smtp");
		        t.connect();
		        t.sendMessage(message, message.getAllRecipients());
		        t.close();
			 
			} else {
				Logger.error("Regulator is not set to receive email - not sending activity notification for status " + statusKey);
			}
		} catch (MessagingException me) {
			 me.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e.getMessage());
		}
	}
	
		
	/**
	 * Returns the activity types
	 * @return activity types
	 */
	@Transactional(readOnly=true)
	@ApiOperation(value = "Find list options for Activity Types",
		notes = "Returns a list of Activity Type options which are valid in activity application submissions.",
		nickname = "options",
		response = LinkedHashMap.class,
		httpMethod = "GET")
	@ApiResponses(value = {})
	public static Result activityTypeOptions()
	{
		if (request().accepts("text/html")) {
			return badRequest("Unsupported content type");
		} else if (request().accepts("application/json")) {
			return ok(Json.toJson(ActivityTypes.getOptions()));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
	/**
	 * Retrieves the list of Noise Produces of which the user is a verified member
	 * @return List of Noise Producers
	 */
	@Transactional(readOnly=true)
	@ApiOperation(value = "Find list options for Noise Producers of which User is a verified member",
		notes = "Returns a list of Noise Producer options which are valid in activity application submissions.",
		nickname = "options",
		response = LinkedHashMap.class,
		httpMethod = "GET")
	@ApiResponses(value = {
			@ApiResponse(code = 400, message = "Invalid user")
		})
	public static Result noiseProducerOptions()
	{
		AppUser au = AppUser.getSystemUser(request().username());
		
		if (request().accepts("text/html")) {
			return badRequest("Unsupported content type");
		} else if (request().accepts("application/json")) {
			return ok(Json.toJson(NoiseProducer.getOptions(au)));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
	@Transactional
	public static Result update(Long id) {
		AppUser au = AppUser.getSystemUser(request().username());
		Form<ActivityApplication>  filledForm = appForm.bindFromRequest();
		
		if(filledForm.hasErrors()) {
			if (request().accepts("text/html")) {
				return badRequest(activityform.render(au, filledForm, Messages.get("activityform.title"), id, null));
			} else if (request().accepts("application/json")) {
        		return badRequest(filledForm.errorsAsJson());
        	} else {
        		return badRequest("Unsupported content type");
        	}
			
		} else {
			ActivityApplication aa = filledForm.get();
			try {
				aa.update(id);
				JPA.em().flush();  //make sure any persistence errors are raised before emailing
			
				//if this is a newly proposed item, then send a mail...
				if (aa.getStatus().equals("Proposed")) {
					sendRegulatorNotification(aa, aa.getStatus().toLowerCase());
				}
			} catch (Exception e) {
				filledForm.reject(e.getMessage());
				return badRequest(activityform.render(au, filledForm, Messages.get("activityform.title"), id, null));
			}
			if (request().accepts("text/html")) {
				return redirect(routes.ActivityApplicationController.confirmadd(Long.toString(aa.getId())));
			} else if (request().accepts("application/json")) {
				return redirect(routes.ActivityApplicationController.read(aa.getId()));
        	} else {
        		return badRequest("Unsupported content type");
        	}	
		}
	}
	
	/**
	 * Returns a list of Regulator options which are valid in activity application submissions
	 * @return List of regulators
	 */
	@Transactional(readOnly=true)
	@ApiOperation(value = "Find list options for Regulators",
		notes = "Returns a list of Regulator options which are valid in activity application submissions.",
		nickname = "options",
		response = LinkedHashMap.class,
		httpMethod = "GET")
	@ApiResponses(value = {})
	public static Result regulatorOptions()
	{
		if (request().accepts("text/html")) {
			return badRequest("Unsupported content type");
		} else if (request().accepts("application/json")) {
			return ok(Json.toJson(Regulator.getOptions()));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
}
