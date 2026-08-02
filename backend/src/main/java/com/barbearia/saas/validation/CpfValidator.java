package com.barbearia.saas.validation;

import com.barbearia.saas.util.CpfUtil;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Validador Bean Validation para CPF (Receita Federal). */
public class CpfValidator implements ConstraintValidator<Cpf, String> {

    private boolean optional;

    @Override
    public void initialize(Cpf annotation) {
        this.optional = annotation.optional();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return optional;
        }
        return CpfUtil.isValido(value);
    }
}
