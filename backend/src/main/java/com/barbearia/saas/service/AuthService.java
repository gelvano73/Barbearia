package com.barbearia.saas.service;

import com.barbearia.saas.config.SecurityAppProperties;
import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.OAuthProvider;
import com.barbearia.saas.domain.enums.PlanoAssinatura;
import com.barbearia.saas.domain.enums.Role;
import com.barbearia.saas.domain.enums.StatusAssinatura;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.auth.*;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.JwtService;
import com.barbearia.saas.security.UsuarioPrincipal;
import com.barbearia.saas.util.CpfUtil;
import com.barbearia.saas.util.EmailUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/** Autenticação, registro de usuários/clientes, OAuth e reset de senha. */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final BarbeariaRepository barbeariaRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final BarbeiroRepository barbeiroRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final OAuthIdentityRepository oAuthIdentityRepository;
    private final UnidadeService unidadeService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final LoginAttemptService loginAttemptService;
    private final OtpService otpService;
    private final EmailDominioService emailDominioService;
    private final SecurityAppProperties securityAppProperties;

    @Value("${app.oauth.dev-mode:true}")
    private boolean oauthDevMode;

    @Value("${app.public-base-url:http://localhost:5173}")
    private String publicBaseUrl = "http://localhost:5173";

    @Value("${app.assinatura.trial-dias:14}")
    private int trialDias;

    /** Registra uma nova barbearia com usuário administrador. */
    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        emailDominioService.validarOuFalhar(request.getEmail());
        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new NegocioException("Email já cadastrado");
        }
        String cpf = CpfUtil.somenteDigitos(request.getCpf());
        if (!CpfUtil.isValidoParaNotaFiscal(cpf, true)) {
            throw new NegocioException(
                    "CPF inválido ou de demonstração. Informe o CPF real cadastrado na Receita Federal.");
        }
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new NegocioException("CPF já cadastrado");
        }
        if (request.getCnpj() != null && !request.getCnpj().isBlank()
                && barbeariaRepository.existsByCnpj(normalizarCnpj(request.getCnpj()))) {
            throw new NegocioException("CNPJ já cadastrado");
        }

        Barbearia barbearia = barbeariaRepository.save(Barbearia.builder()
                .nome(request.getNomeBarbearia().trim())
                .cnpj(normalizarCnpj(request.getCnpj()))
                .telefone(blankToNull(request.getTelefoneBarbearia()))
                .email(request.getEmail().toLowerCase().trim())
                .ativo(true)
                .plano(PlanoAssinatura.TRIAL)
                .assinaturaStatus(StatusAssinatura.ATIVA)
                .assinaturaVenceEm(LocalDateTime.now().plusDays(trialDias))
                .build());

        unidadeService.criarPadrao(barbearia);

        Usuario admin = usuarioRepository.save(Usuario.builder()
                .barbearia(barbearia)
                .nome(request.getNomeAdmin().trim())
                .email(request.getEmail().toLowerCase().trim())
                .cpf(cpf)
                .telefone(blankToNull(request.getTelefoneBarbearia()))
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .role(Role.ADMIN)
                .ativo(true)
                .aceitePrivacidadeEm(LocalDateTime.now())
                .build());

        return buildAuthResponse(admin, barbearia, null, null);
    }

    /** Registra um novo cliente no portal. */
    @Transactional
    public AuthResponse registrarCliente(RegistroClienteRequest request) {
        emailDominioService.validarOuFalhar(request.getEmail());
        String email = request.getEmail().toLowerCase().trim();
        if (usuarioRepository.existsByEmail(email)) {
            throw new NegocioException("Email já cadastrado");
        }
        String cpf = CpfUtil.somenteDigitos(request.getCpf());
        if (!CpfUtil.isValidoParaNotaFiscal(cpf, true)) {
            throw new NegocioException(
                    "CPF inválido ou de demonstração. Informe o CPF real cadastrado na Receita Federal.");
        }
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new NegocioException("CPF já cadastrado");
        }

        Barbearia barbearia = barbeariaRepository.findById(request.getBarbeariaId())
                .filter(b -> Boolean.TRUE.equals(b.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .barbearia(barbearia)
                .nome(request.getNome().trim())
                .email(email)
                .cpf(cpf)
                .telefone(request.getTelefone().trim())
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .role(Role.CLIENTE)
                .ativo(true)
                .aceitePrivacidadeEm(LocalDateTime.now())
                .build());

        Cliente cliente = clienteRepository.save(Cliente.builder()
                .barbearia(barbearia)
                .usuario(usuario)
                .nome(request.getNome().trim())
                .telefone(request.getTelefone().trim())
                .email(email)
                .cpf(cpf)
                .ativo(true)
                .aceitePrivacidadeEm(LocalDateTime.now())
                .build());

        return buildAuthResponse(usuario, barbearia, cliente.getId(), null);
    }

    /** Autentica o usuário e retorna o token JWT. */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String chave = request.getLogin() != null ? request.getLogin().trim() : "";
        loginAttemptService.verificarNaoBloqueado(chave);
        Usuario usuario = buscarPorLogin(chave);
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(usuario.getEmail(), request.getSenha()));
            loginAttemptService.registrarSucesso(chave);
        } catch (BadCredentialsException e) {
            loginAttemptService.registrarFalha(chave);
            throw new NegocioException("Credenciais inválidas");
        }

        return buildAuthResponse(usuario, usuario.getBarbearia(), resolveClienteId(usuario), resolveBarbeiroId(usuario));
    }

    /** Autentica barbeiro e retorna o token JWT. */
    @Transactional(readOnly = true)
    public AuthResponse loginBarbeiro(LoginRequest request) {
        AuthResponse response = login(request);
        if (response.getRole() != Role.BARBEIRO) {
            throw new NegocioException("Use o login correto para este tipo de conta");
        }
        return response;
    }

    /** Autentica cliente do portal e retorna o token JWT. */
    @Transactional(readOnly = true)
    public AuthResponse loginCliente(LoginRequest request) {
        AuthResponse response = login(request);
        if (response.getRole() != Role.CLIENTE) {
            throw new NegocioException("Use o login da barbearia para contas internas");
        }
        return response;
    }

    /** Autentica usuário da recepção e retorna o token JWT. */
    @Transactional(readOnly = true)
    public AuthResponse loginRecepcao(LoginRequest request) {
        AuthResponse response = login(request);
        if (response.getRole() != Role.ATENDENTE && response.getRole() != Role.ADMIN) {
            throw new NegocioException("Acesso restrito à recepção");
        }
        return response;
    }

    /** Cria um usuário com perfil de atendente. */
    @Transactional
    public AuthResponse criarAtendente(CriarAtendenteRequest request) {
        Long barbeariaId = com.barbearia.saas.security.SecurityUtils.getBarbeariaIdAtual();
        var role = com.barbearia.saas.security.SecurityUtils.getUsuarioAtual().getRole();
        if (role != Role.ADMIN) {
            throw new NegocioException("Apenas admin pode criar recepcionista");
        }

        emailDominioService.validarOuFalhar(request.getEmail());
        String email = EmailUtil.normalizar(request.getEmail());
        if (usuarioRepository.existsByEmail(email)) {
            throw new NegocioException("Email já cadastrado");
        }

        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Usuario atendente = usuarioRepository.save(Usuario.builder()
                .barbearia(barbearia)
                .nome(request.getNome().trim())
                .email(email)
                .telefone(blankToNull(request.getTelefone()))
                .cpf(request.getCpf() != null && !request.getCpf().isBlank()
                        ? CpfUtil.somenteDigitos(request.getCpf()) : null)
                .senhaHash(passwordEncoder.encode(request.getSenha()))
                .role(Role.ATENDENTE)
                .ativo(true)
                .build());

        return buildAuthResponse(atendente, barbearia, null, null);
    }

    /** Envia código OTP para o telefone do usuário (login sem senha). */
    @Transactional(readOnly = true)
    public Map<String, Object> enviarOtp(OtpEnviarRequest request) {
        String chave = request.getLogin().trim();
        loginAttemptService.verificarNaoBloqueado(chave);
        Usuario usuario = buscarPorLogin(chave);
        String telefone = resolverTelefone(usuario);
        String mascarado = otpService.gerarEEnviar(
                chave, telefone, usuario.getBarbearia().getId(), usuario.getNome());
        return Map.of(
                "mensagem", "Código enviado para o telefone cadastrado",
                "telefoneMascarado", mascarado);
    }

    /** Valida OTP e retorna JWT. */
    @Transactional(readOnly = true)
    public AuthResponse verificarOtp(OtpVerificarRequest request) {
        String chave = request.getLogin().trim();
        loginAttemptService.verificarNaoBloqueado(chave);
        try {
            otpService.validar(chave, request.getCodigo());
            loginAttemptService.registrarSucesso(chave);
        } catch (NegocioException e) {
            loginAttemptService.registrarFalha(chave);
            throw e;
        }
        Usuario usuario = buscarPorLogin(chave);
        return buildAuthResponse(usuario, usuario.getBarbearia(), resolveClienteId(usuario), resolveBarbeiroId(usuario));
    }

    private Usuario buscarPorLogin(String login) {
        if (login == null || login.isBlank()) {
            throw new NegocioException("Informe e-mail ou CPF");
        }
        String valor = login.trim();
        if (CpfUtil.pareceCpf(valor)) {
            String cpf = CpfUtil.somenteDigitos(valor);
            if (!CpfUtil.isValido(cpf)) {
                throw new NegocioException("CPF inválido");
            }
            return usuarioRepository.findByCpf(cpf)
                    .orElseThrow(() -> new NegocioException("Usuário não encontrado"));
        }
        return usuarioRepository.findByEmail(valor.toLowerCase())
                .orElseThrow(() -> new NegocioException("Usuário não encontrado"));
    }

    private String resolverTelefone(Usuario usuario) {
        if (usuario.getTelefone() != null && !usuario.getTelefone().isBlank()) {
            return usuario.getTelefone();
        }
        if (usuario.getRole() == Role.CLIENTE) {
            return clienteRepository.findByUsuarioId(usuario.getId())
                    .map(Cliente::getTelefone)
                    .filter(t -> t != null && !t.isBlank())
                    .orElseThrow(() -> new NegocioException("Usuário sem telefone cadastrado"));
        }
        if (usuario.getBarbearia() != null && usuario.getBarbearia().getTelefone() != null
                && !usuario.getBarbearia().getTelefone().isBlank()) {
            return usuario.getBarbearia().getTelefone();
        }
        throw new NegocioException("Usuário sem telefone cadastrado para receber o código");
    }

    private Long resolveClienteId(Usuario usuario) {
        if (usuario.getRole() != Role.CLIENTE) {
            return null;
        }
        return clienteRepository.findByUsuarioId(usuario.getId())
                .map(Cliente::getId)
                .orElse(null);
    }

    /** Inicia o fluxo de recuperação de senha. */
    @Transactional
    public RecuperarSenhaResponse recuperarSenha(RecuperarSenhaRequest request) {
        String email = EmailUtil.normalizar(request.getEmail());
        String mensagemPadrao = "Se o e-mail existir e estiver ativo, enviaremos instruções de recuperação";

        var usuarioOpt = usuarioRepository.findByEmail(email);
        if (usuarioOpt.isEmpty()) {
            return RecuperarSenhaResponse.builder().mensagem(mensagemPadrao).build();
        }

        Usuario usuario = usuarioOpt.get();
        String token = UUID.randomUUID().toString().replace("-", "");
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .usuario(usuario)
                .token(token)
                .expiraEm(LocalDateTime.now().plusHours(2))
                .usado(false)
                .build());

        log.info("Token de recuperação de senha gerado para usuário id={}", usuario.getId());

        String link = publicBaseUrl.replaceAll("/+$", "") + "/portal/recuperar-senha?token=" + token;
        emailService.send(
                usuario.getBarbearia() != null ? usuario.getBarbearia().getId() : null,
                email,
                "Recuperação de senha",
                "Olá, " + usuario.getNome() + "!\n\n"
                        + "Recebemos uma solicitação para redefinir sua senha. Clique no link abaixo para continuar:\n"
                        + link + "\n\n"
                        + "Se você não solicitou essa alteração, ignore este email.");

        RecuperarSenhaResponse.RecuperarSenhaResponseBuilder builder = RecuperarSenhaResponse.builder()
                .mensagem(mensagemPadrao);
        if (securityAppProperties.isExposeDevTokens()) {
            builder.tokenDev(token);
        }
        return builder.build();
    }

    /** Redefine a senha usando o token recebido. */
    @Transactional
    public void redefinirSenha(RedefinirSenhaRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenAndUsadoFalse(request.getToken())
                .orElseThrow(() -> new NegocioException("Token inválido ou já utilizado"));

        if (resetToken.getExpiraEm().isBefore(LocalDateTime.now())) {
            throw new NegocioException("Token expirado");
        }

        Usuario usuario = resetToken.getUsuario();
        usuario.setSenhaHash(passwordEncoder.encode(request.getNovaSenha()));
        usuarioRepository.save(usuario);

        resetToken.setUsado(true);
        passwordResetTokenRepository.save(resetToken);
    }

    /** Autentica via provedor OAuth e retorna o token JWT. */
    @Transactional
    public AuthResponse oauthLogin(String providerName, OAuthLoginRequest request) {
        if (!oauthDevMode) {
            throw new NegocioException("OAuth em produção ainda não configurado");
        }

        OAuthProvider provider;
        try {
            provider = OAuthProvider.valueOf(providerName.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new NegocioException("Provider OAuth inválido");
        }

        return oAuthIdentityRepository.findByProviderAndProviderUserId(provider, request.getProviderUserId())
                .map(identity -> {
                    Usuario usuario = identity.getUsuario();
                    return buildAuthResponse(usuario, usuario.getBarbearia(), resolveClienteId(usuario), resolveBarbeiroId(usuario));
                })
                .orElseGet(() -> criarClienteViaOAuth(provider, request));
    }

    private AuthResponse criarClienteViaOAuth(OAuthProvider provider, OAuthLoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();
        Barbearia barbearia = barbeariaRepository.findById(request.getBarbeariaId())
                .filter(b -> Boolean.TRUE.equals(b.getAtivo()))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));

        Usuario usuario = usuarioRepository.findByEmail(email).orElse(null);
        Cliente cliente;

        if (usuario == null) {
            usuario = usuarioRepository.save(Usuario.builder()
                    .barbearia(barbearia)
                    .nome(request.getNome().trim())
                    .email(email)
                    .senhaHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .role(Role.CLIENTE)
                    .ativo(true)
                    .build());

            cliente = clienteRepository.save(Cliente.builder()
                    .barbearia(barbearia)
                    .usuario(usuario)
                    .nome(request.getNome().trim())
                    .telefone(request.getTelefone() != null && !request.getTelefone().isBlank()
                            ? request.getTelefone().trim() : "00000000000")
                    .email(email)
                    .ativo(true)
                    .build());
        } else {
            if (usuario.getRole() != Role.CLIENTE) {
                throw new NegocioException("Email já vinculado a outra conta");
            }
            cliente = clienteRepository.findByUsuarioId(usuario.getId())
                    .orElseThrow(() -> new NegocioException("Cliente não vinculado"));
        }

        oAuthIdentityRepository.save(OAuthIdentity.builder()
                .usuario(usuario)
                .provider(provider)
                .providerUserId(request.getProviderUserId())
                .build());

        return buildAuthResponse(usuario, barbearia, cliente.getId(), null);
    }

    private Long resolveBarbeiroId(Usuario usuario) {
        if (usuario.getRole() != Role.BARBEIRO) {
            return null;
        }
        return barbeiroRepository.findByUsuarioId(usuario.getId())
                .map(Barbeiro::getId)
                .orElse(null);
    }

    private AuthResponse buildAuthResponse(Usuario usuario, Barbearia barbearia, Long clienteId, Long barbeiroId) {
        UsuarioPrincipal principal = new UsuarioPrincipal(usuario, clienteId, barbeiroId);
        String token = jwtService.generateToken(principal);
        return AuthResponse.builder()
                .token(token)
                .tipo("Bearer")
                .usuarioId(usuario.getId())
                .clienteId(clienteId)
                .barbeiroId(barbeiroId)
                .nome(usuario.getNome())
                .email(usuario.getEmail())
                .role(usuario.getRole())
                .barbeariaId(barbearia.getId())
                .nomeBarbearia(barbearia.getNome())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String normalizarCnpj(String cnpj) {
        if (cnpj == null || cnpj.isBlank()) {
            return null;
        }
        String digits = com.barbearia.saas.util.CnpjUtil.somenteDigitos(cnpj);
        if (digits.isBlank()) {
            return null;
        }
        if (!com.barbearia.saas.util.CnpjUtil.isValido(digits)) {
            throw new NegocioException("CNPJ inválido segundo dígitos verificadores da Receita Federal");
        }
        return digits;
    }

    /** Lista barbearias ativas. */
    @Transactional(readOnly = true)
    public java.util.List<BarbeariaResumoResponse> listarBarbeariasAtivas() {
        return barbeariaRepository.findAll().stream()
                .filter(b -> Boolean.TRUE.equals(b.getAtivo()))
                .map(b -> BarbeariaResumoResponse.builder().id(b.getId()).nome(b.getNome()).build())
                .toList();
    }
}
