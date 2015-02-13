package controllers;


import models.AppUser;
import models.NoiseProducer;
import play.data.Form;
import play.db.jpa.Transactional;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.*;


@Security.Authenticated(SecuredController.class)
public class NoiseproducerController extends Controller {

	static Form<NoiseProducer> appForm = Form.form(NoiseProducer.class);
	
	/**
	 * UI page allowing new Noise Producers
	 * @return Add noise producer page
	 */
	@Transactional(readOnly=true)
	public static Result add() 
	{
		return ok(
				noiseproducer.render(AppUser.findByEmail(session("email")), appForm)
			  );
	}
	
	/**
	 * Allows the saving of a noise producer
	 * @return confirmation page 
	 */
	@Transactional
	public static Result save()
	{
		Form<NoiseProducer>  filledForm = appForm.bindFromRequest();
		if(filledForm.hasErrors()) {
			return badRequest(
					noiseproducer.render(AppUser.findByEmail(session("email")), filledForm)
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
