package models;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

public class CheckActivityApplicationValidator implements ConstraintValidator<CheckActivityApplication, ActivityApplication> {

	@Override
	public void initialize(CheckActivityApplication arg0) {
	}

	@Override
	public boolean isValid(ActivityApplication aa,
			ConstraintValidatorContext context) {
		aa.unsetNotNeeded();
		return true;
	}

}
