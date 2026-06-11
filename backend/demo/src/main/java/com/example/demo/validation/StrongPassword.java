package com.example.demo.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Password policy shared by sign-up and password reset:
 * at least 8 characters, one uppercase, one lowercase, one digit, and one symbol.
 */
@Documented
@Constraint(validatedBy = StrongPasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "password must be at least 8 characters and include an uppercase letter, a lowercase letter, a number, and a symbol";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
