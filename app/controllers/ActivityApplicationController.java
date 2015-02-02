package controllers;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

@Security.Authenticated(SecuredController.class)
@Api(value = "/aas", description = "Operations on Activity Applications (aas)")
public class ActivityApplicationController extends Controller {
	
	static Form<ActivityApplication> appForm = Form.form(ActivityApplication.class);
	static Form<ActivityApplicationCloseOut> appcloseoutForm = Form.form(ActivityApplicationCloseOut.class);
	
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
		//Logger.error("Found drafts: " + Integer.toString(apps.size()));
		if (request().accepts("text/html")) {
			return ok(activities.render(au, au.findApplicationsByStatus(status, comparator), statusVals));
		} else if (request().accepts("application/json")) {
			return ok(Json.toJson(au.findApplicationsByStatus(status, comparator)));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
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
	
	@Transactional(readOnly=true)
	public static Result add()
	{
		AppUser au = AppUser.getSystemUser(request().username());
		
		if (request().accepts("text/html")) {
			//Prevent caching the form (doesn't work for the current version of Chrome v40.0)
			response().setHeader(CACHE_CONTROL, "no-cache, no-store, must-revalidate");
			response().setHeader(PRAGMA, "no-cache, no-store");
			response().setHeader(EXPIRES, "0");
			
			return ok(activityform.render(au, appForm, Messages.get("activityform.title_new"), null));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
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
			
			return ok(activityform.render(au, filledForm, Messages.get("activityform.title"), id));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
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
			return ok(activityform.render(au, filledForm, Messages.get("activityform.title"), null));
		} else {
			return badRequest("Unsupported content type");
		}
	}
	
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
	
	@Transactional
	@ApiOperation(value = "Get Activity Application for close out by ID",
    	notes = "Returns an Activity Application for close out",
    	nickname = "getById",
    	response = ActivityApplication.class, httpMethod = "GET")
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
			Form<ActivityApplication> filledForm = appForm.fill(aa);

			if (request().accepts("text/html")) {
				return ok(activityformcloseout.render(au, aa, filledForm, id));
			} else if (request().accepts("application/json")) {
				return ok(Json.toJson(aa));
			} else {
				return badRequest("Unsupported content type");
			}
		} else {
			return notFound();
		}
	}
	
	@Transactional
	@ApiOperation(value="Close out Activity Application",
	notes = "Closes out an Activity Application using the data supplied (if valid).",
	nickname = "closeoutAA",
	response = ActivityApplication.class, httpMethod = "POST")
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
			
			if (filledForm.hasErrors()) {
				if (request().accepts("text/html")) {
					return badRequest(activityformcloseout.render(au, aa, filledForm, id));
				} else if (request().accepts("application/json")) {
	        		return badRequest(filledForm.errorsAsJson());
	        	} else {
	        		return badRequest("Unsupported content type");
	        	}
			} else {
				try {
					filledForm.get().closeOut(id);
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
					return badRequest(e.getMessage());
				}
			}
		} else {
			String activeTab="HOME";
			return status(404,index.render(au, activeTab));
		}
		
	}
	
	@Transactional
	public static Result cancelApplication()
	{
		Form<ActivityApplication>  filledForm = appForm.bindFromRequest();
		Map<String,String> map = filledForm.data();
		String sId = (String)map.get("id"); 
		Long id = Long.parseLong(sId);
		
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
		return redirect(routes.ActivityApplicationController.index());
	}
	
	@Transactional
	@ApiOperation(value="Create Activity Application",
	notes = "Creates a new Activity Application using the data supplied (if valid).  The following fields are read-only and should be excluded from any POST: all id values, all aa values, status, date_due, date_closed",
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
			if (request().accepts("text/html")) {
				return badRequest(
						activityform.render(au, filledForm, Messages.get("activityform.title_new"), null)
			    );
			} else if (request().accepts("application/json")) {
        		return badRequest(filledForm.errorsAsJson());
        	} else {
        		return badRequest("Unsupported content type");
        	}
			
		} else {
			ActivityApplication aa = filledForm.get();

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
	
	public static void sendRegulatorNotification(ActivityApplication aa, String statusKey)
	{
		try {
			//Regulator reg = aa.getRegulator();
			Regulator reg = JPA.em().find(Regulator.class,  aa.getRegulator().getId());
			NoiseProducer np = JPA.em().find(NoiseProducer.class,  aa.getNoiseproducer().getId());
			
			Logger.debug(reg.getAccepts_email().toString());
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
				return badRequest(activityform.render(au, filledForm, Messages.get("activityform.title"), id));
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
				return badRequest(activityform.render(au, filledForm, Messages.get("activityform.title"), id));
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
