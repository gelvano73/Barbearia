package com.barbearia.saas.exception;

import com.barbearia.saas.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

/** Handler global que traduz exceções de negócio e validação em respostas HTTP padronizadas. */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /** Trata recurso não encontrado e retorna HTTP 404. */
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Não encontrado", ex.getMessage(), request.getRequestURI(), null);
    }

    /** Trata violação de regra de negócio e retorna HTTP de erro. */
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorResponse> handleNegocio(NegocioException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Regra de negócio", ex.getMessage(), request.getRequestURI(), null);
    }

    /** Trata credenciais inválidas e retorna HTTP 401. */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Não autorizado", "Email ou senha inválidos", request.getRequestURI(), null);
    }

    /** Trata erros de validação Bean Validation. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> detalhes = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();
        if (detalhes.isEmpty()) {
            detalhes = ex.getBindingResult().getGlobalErrors().stream()
                    .map(err -> err.getDefaultMessage() == null ? err.getCode() : err.getDefaultMessage())
                    .toList();
        }
        String mensagem = detalhes.isEmpty() ? "Dados inválidos" : detalhes.get(0);
        return build(HttpStatus.BAD_REQUEST, "Validação", mensagem, request.getRequestURI(), detalhes);
    }

    /** Trata ConstraintViolation (ex.: AssertTrue fora de field errors). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest request) {
        List<String> detalhes = ex.getConstraintViolations().stream()
                .map(v -> v.getMessage())
                .toList();
        String mensagem = detalhes.isEmpty() ? "Dados inválidos" : detalhes.get(0);
        return build(HttpStatus.BAD_REQUEST, "Validação", mensagem, request.getRequestURI(), detalhes);
    }

    /** Trata violação de integridade no banco (unique, NOT NULL, etc.). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Integridade de dados em {}: {}", request.getRequestURI(), ex.getMostSpecificCause().getMessage());
        String msg = ex.getMostSpecificCause().getMessage();
        if (msg != null && msg.toLowerCase().contains("unique")) {
            return build(HttpStatus.CONFLICT, "Regra de negócio", "Registro já existe (email/CNPJ duplicado)", request.getRequestURI(), null);
        }
        return build(HttpStatus.CONFLICT, "Integridade", "Não foi possível salvar os dados. Verifique os campos e tente novamente.", request.getRequestURI(), null);
    }

    /** Trata exceções genéricas não mapeadas. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex, HttpServletRequest request) {
        log.error("Erro interno em {}", request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno",
                "Ocorreu um erro inesperado. Tente novamente ou contate o suporte.",
                request.getRequestURI(), null);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + ": " + error.getDefaultMessage();
    }

    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String erro, String mensagem, String path, List<String> detalhes) {
        return ResponseEntity.status(status).body(ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .erro(erro)
                .mensagem(mensagem)
                .path(path)
                .detalhes(detalhes)
                .build());
    }
}
