package com.dxh.learninghub.validator;

import com.dxh.learninghub.enums.Gender;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target({METHOD, FIELD, PARAMETER, RECORD_COMPONENT})
@Retention(RUNTIME)
@Constraint(validatedBy = GenderSubSetValidator.class)
public @interface GenderSubset {
    Gender[] anyOf();
    String message() default "INVALID_GENDER";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
