package com.barbearia.saas.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Senha forte: mínimo 8 caracteres, com maiúscula, minúscula e número. */
@Documented
@Constraint(validatedBy = SenhaForteValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SenhaForte {
    String message() default "Senha deve ter no mínimo 8 caracteres, com letra maiúscula, minúscula e número";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
