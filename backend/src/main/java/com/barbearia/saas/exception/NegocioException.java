package com.barbearia.saas.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Exceção de regra de negócio mapeada tipicamente para HTTP 400/422. */
@ResponseStatus(HttpStatus.CONFLICT)
public class NegocioException extends RuntimeException {
    public NegocioException(String mensagem) {
        super(mensagem);
    }
}
