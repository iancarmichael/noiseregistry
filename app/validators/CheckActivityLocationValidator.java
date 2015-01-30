package validators;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

import models.ActivityLocation;
import play.Logger;

public class CheckActivityLocationValidator implements ConstraintValidator<CheckActivityLocation, ActivityLocation> {

	@Override
	public void initialize(CheckActivityLocation arg0) {
		
	}

	@Override
	public boolean isValid(ActivityLocation al,
			ConstraintValidatorContext context) {
		
		boolean bHasErrors = false;
		Logger.error("ActivityLocationValidator");
		return !bHasErrors;
		/*
		try {
		    ActivityApplication aa = al.getAa();
		    if (aa != null) {
		    	if (aa.getId() != null && aa.getStatus()==null) {
		    		aa = JPA.em().find(ActivityApplication.class, aa.getId());
		    	}
		    	
		    }
			
			if (al.getEntered_ogb_code()!=null && !al.getEntered_ogb_code().equals("")) {
	    		List<OilAndGasBlock> blocks = OilAndGasBlock.findByCode(al.getEntered_ogb_code());
	    		if (blocks.isEmpty()) {
	    			context.disableDefaultConstraintViolation();
	    			context.buildConstraintViolationWithTemplate("validation.entered_ogb_code.invalid")
				  	          .addPropertyNode( "entered_ogb_code" )
				  	          .addConstraintViolation();
	    			bHasErrors = true;
	    		}
			}
			Logger.error(al.getCreation_type());
			
			if (aa!=null && aa.getStatus() != null) {
				Logger.error("Got aa with id: " + Long.toString(aa.getId()) + " status: " + aa.getStatus());
			} else {
				Logger.error("Null aa or aa status value?");
				return !bHasErrors;
			}
			if (aa!=null && (aa.getStatus()!=null && aa.getStatus().equals("Closed"))) {
				//for closing, there must be dates or this item must be marked as no activity...
				if (al.getCreation_type().equals("Proposed") && (al.getNo_activity() == null)) {
					context.disableDefaultConstraintViolation();
					context.buildConstraintViolationWithTemplate("error.required")
		  	          .addPropertyNode( "no_activity" )
		  	          .addConstraintViolation();
					bHasErrors = true;
				}
				if (al.getNo_activity() != null && !al.getNo_activity()) {
					if (al.getActivitydates() == null) {
						context.disableDefaultConstraintViolation();
						context.buildConstraintViolationWithTemplate("error.required")
			  	          .addPropertyNode( "activitydate" )
			  	          .addConstraintViolation();
						bHasErrors = true;
					}
				}
				if (al.getCreation_type().equals("Additional") && (al.getActivitydates() == null)) {
					context.disableDefaultConstraintViolation();
					context.buildConstraintViolationWithTemplate("error.required")
		  	          .addPropertyNode( "activitydate" )
		  	          .addConstraintViolation();
					bHasErrors = true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error(e.getMessage());
		}
		Logger.error("ActivityLocationValidator returning: " + Boolean.toString(!bHasErrors));
		return !bHasErrors;
		*/
	}

}
