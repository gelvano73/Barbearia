package com.barbearia.saas.validation;

import com.barbearia.saas.util.CnpjUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validador Bean Validation de CNPJ. */
public class CnpjValidator implements ConstraintValidator<Cnpj, String> {

    private boolean optional;

    @Override
    public void initialize(Cnpj annotation) {
        this.optional = annotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return optional;
        }
        return CnpjUtil.isValido(value);
    }
}
