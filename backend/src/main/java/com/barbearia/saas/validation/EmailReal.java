package com.barbearia.saas.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/** E-mail real: formato, sem domínio descartável/exemplo; DNS opcional via serviço. */
@Documented
@Constraint(validatedBy = EmailRealValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface EmailReal {
    String message() default "E-mail inválido ou não real (use um endereço ativo, sem temporários)";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    boolean optional() default false;
}
