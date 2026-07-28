package com.barbearia.saas.controller;

import com.barbearia.saas.dto.auth.*;
import com.barbearia.saas.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Endpoints de autenticação, registro, OAuth e recuperação de senha. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Registro, login JWT, recuperação de senha e OAuth")
public class AuthController {

    private final AuthService authService;

    /** Registra uma nova barbearia com usuário administrador. */
    @PostMapping("/registro")
    @Operation(summary = "Registrar barbearia e usuário admin")
    public ResponseEntity<AuthResponse> registrar(@Valid @RequestBody RegistroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    /** Autentica o usuário e retorna o token JWT. */
    @PostMapping("/login")
    @Operation(summary = "Autenticar staff e obter token JWT")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /** Registra um novo cliente no portal. */
    @PostMapping("/cliente/registro")
    @Operation(summary = "Registrar cliente no portal")
    public ResponseEntity<AuthResponse> registrarCliente(@Valid @RequestBody RegistroClienteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrarCliente(request));
    }

    /** Autentica cliente do portal e retorna o token JWT. */
    @PostMapping("/cliente/login")
    @Operation(summary = "Login do cliente no portal")
    public ResponseEntity<AuthResponse> loginCliente(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginCliente(request));
    }

    /** Autentica barbeiro e retorna o token JWT. */
    @PostMapping("/barbeiro/login")
    @Operation(summary = "Login do barbeiro no portal")
    public ResponseEntity<AuthResponse> loginBarbeiro(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginBarbeiro(request));
    }

    /** Autentica usuário da recepção e retorna o token JWT. */
    @PostMapping("/recepcao/login")
    @Operation(summary = "Login da recepcionista")
    public ResponseEntity<AuthResponse> loginRecepcao(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.loginRecepcao(request));
    }

    /** Cria um usuário com perfil de atendente. */
    @PostMapping("/recepcao/atendente")
    @Operation(summary = "Criar conta de recepcionista (admin)")
    @org.springframework.security.access.prepost.PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AuthResponse> criarAtendente(@Valid @RequestBody CriarAtendenteRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.criarAtendente(request));
    }

    /** Inicia o fluxo de recuperação de senha. */
    @PostMapping("/recuperar-senha")
    @Operation(summary = "Solicitar token de recuperação de senha")
    public ResponseEntity<RecuperarSenhaResponse> recuperarSenha(@Valid @RequestBody RecuperarSenhaRequest request) {
        return ResponseEntity.ok(authService.recuperarSenha(request));
    }

    /** Redefine a senha usando o token recebido. */
    @PostMapping("/redefinir-senha")
    @Operation(summary = "Redefinir senha com token")
    public ResponseEntity<Void> redefinirSenha(@Valid @RequestBody RedefinirSenhaRequest request) {
        authService.redefinirSenha(request);
        return ResponseEntity.noContent().build();
    }

    /** Login social (modo desenvolvimento). */
    @PostMapping("/oauth/{provider}")
    @Operation(summary = "Login social (modo desenvolvimento)")
    public ResponseEntity<AuthResponse> oauth(
            @PathVariable String provider,
            @Valid @RequestBody OAuthLoginRequest request) {
        return ResponseEntity.ok(authService.oauthLogin(provider, request));
    }

    /** Listar barbearias ativas para cadastro no portal. */
    @GetMapping("/barbearias")
    @Operation(summary = "Listar barbearias ativas para cadastro no portal")
    public ResponseEntity<java.util.List<BarbeariaResumoResponse>> listarBarbearias() {
        return ResponseEntity.ok(authService.listarBarbeariasAtivas());
    }
}
