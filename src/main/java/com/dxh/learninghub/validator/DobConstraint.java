package com.dxh.learninghub.validator;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
//có thể validateBy nhiều class
@Constraint(validatedBy = { DobValidator.class })
public @interface DobConstraint {
    String message() default "Invalid date of birth";

    //truền vào
    int min();

    Class<?>[] groups() default { };

    Class<? extends Payload>[] payload() default { };
}
