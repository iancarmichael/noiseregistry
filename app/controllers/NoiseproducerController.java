package controllers;


import models.AppUser;
import models.NoiseProducer;
import play.data.Form;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.*;


@Security.Authenticated(SecuredController.class)
public class NoiseproducerController extends Controller {

	static Form<NoiseProducer> appForm = Form.form(NoiseProducer.class);
	
	@Transactional(readOnly=true)
	public static Result add() 
	{
		return ok(
				noiseproducer.render(AppUser.findByEmail(session("email")), appForm, Messages.get("organisationform.title_new"))
			  );
	}
	
	@Transactional
	public static Result save()
	{
		Form<NoiseProducer>  filledForm = appForm.bindFromRequest();
		if(filledForm.hasErrors()) {
			return badRequest(
					noiseproducer.render(AppUser.findByEmail(session("email")), filledForm, Messages.get("organisationform.title_new"))
		    );
		} else {
			AppUser au = AppUser.findByEmail(session("email"));
			NoiseProducer np = filledForm.get();			
			if (np!=null)
			{
				np.save(au);
			}
	    	String activeTab="HOME";
	        return ok(finished.render(AppUser.findByEmail(session("email")), "organisation.confirmsave.title", "organisation.confirmsave.message", activeTab, routes.OrganisationController.adminorgs()));			
		}
	}

}
