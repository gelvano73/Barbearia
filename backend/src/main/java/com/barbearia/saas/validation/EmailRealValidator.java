package com.barbearia.saas.validation;

import com.barbearia.saas.util.EmailUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Valida formato e bloqueia e-mails descartáveis/de exemplo.
 * A existência do domínio (DNS MX) é checada no serviço de domínio.
 */
public class EmailRealValidator implements ConstraintValidator<EmailReal, String> {

    private boolean optional;

    @Override
    public void initialize(EmailReal annotation) {
        this.optional = annotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return optional;
        }
        return EmailUtil.isValidoParaCadastro(value);
    }
}
