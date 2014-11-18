package controllers;

import play.db.jpa.JPA;
import play.db.jpa.Transactional;
import play.mvc.*;
import views.html.*;
import models.Xuser;

public class Application extends Controller {
	//@Transactional
    public static Result index() {
		
		String message = "";
		/*
		if (JPA.em().find(Xuser.class, "david.simpson@tronsystems.co.uk")==null) {
			final Xuser Xuser = new Xuser();
	         Xuser.email = "david.simpson@tronsystems.co.uk";
	         Xuser.fullname = "David Simpson";

	         //JPA.em().getTransaction().begin();
	         JPA.em().persist(Xuser);
	         //JPA.em().getTransaction().commit();
	         message = "User created!";
		} else {
			message = "User already exists!";
		}
    	*/
    	
        return ok(index.render(message));
    }

}
