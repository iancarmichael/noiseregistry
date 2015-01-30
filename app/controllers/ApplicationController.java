package controllers;

//import play.db.jpa.JPA;
//import play.db.jpa.Transactional;
import models.AppUser;
import play.db.jpa.Transactional;
import play.mvc.*;
import views.html.*;
//import models.Xuser;

public class ApplicationController extends Controller {
	@Transactional(readOnly=true)
    public static Result index() {
	   	String activeTab="HOME";
        return ok(index.render(AppUser.findByEmail(session("email")), activeTab));
    }
	
	@Transactional(readOnly=true)
    public static Result home() {
    	AppUser au = AppUser.findByEmail(session("email"));
    	String activeTab="HOME";
        
    	if ((au!=null) && ((au.getUserOrgType()=="NOISEPRODUCER") || (au.getUserOrgType()=="REGULATOR"))){
    		return ok(home.render(AppUser.findByEmail(session("email")), activeTab));
        }
        return redirect(routes.RegistrationController.read());
    }
	
	@Transactional(readOnly=true)
    public static Result feedback() {
    	String activeTab="FEEDBACK";
        return ok(infopage.render(AppUser.findByEmail(session("email")), "feedback.message", activeTab));
    }
	
	@Transactional(readOnly=true)
    public static Result help() {
    	String activeTab="HOME";
        return ok(infopage.render(AppUser.findByEmail(session("email")), "helppage.message", activeTab));
    }
	
	@Transactional(readOnly=true)
    public static Result cookies() {
    	String activeTab="HOME";
        return ok(infopage.render(AppUser.findByEmail(session("email")), "cookies.message", activeTab));
    }
	
	@Transactional(readOnly=true)
    public static Result contact() {
    	String activeTab="HOME";
        return ok(infopage.render(AppUser.findByEmail(session("email")), "contact.message", activeTab));
    }
	
	@Transactional(readOnly=true)
    public static Result terms() {
    	String activeTab="HOME";
        return ok(infopage.render(AppUser.findByEmail(session("email")), "termsandconditions.message", activeTab));
    }
   
}
