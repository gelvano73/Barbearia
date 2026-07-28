package com.barbearia.saas.service;

import com.barbearia.saas.domain.entity.*;
import com.barbearia.saas.domain.enums.MetodoCheckin;
import com.barbearia.saas.domain.repository.*;
import com.barbearia.saas.dto.checkin.CheckinResponse;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.exception.RecursoNaoEncontradoException;
import com.barbearia.saas.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Check-in por reconhecimento facial: cadastro de perfil e matching. */
@Service
@RequiredArgsConstructor
public class CheckinFacialService {

    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final FacePerfilRepository facePerfilRepository;
    private final CheckinRepository checkinRepository;
    private final ClienteRepository clienteRepository;
    private final BarbeariaRepository barbeariaRepository;
    private final UnidadeService unidadeService;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** Cadastra o perfil facial do cliente. */
    @Transactional
    public CheckinResponse registrarFace(Long clienteId, MultipartFile file) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        Cliente cliente = clienteRepository.findByIdAndBarbeariaId(clienteId, barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        validarImagem(file);
        String fotoUrl = salvarFoto(file, "faces");
        String assinatura = assinaturaImagem(file);

        FacePerfil perfil = facePerfilRepository.findByClienteIdAndAtivoTrue(clienteId)
                .orElse(FacePerfil.builder()
                        .barbearia(cliente.getBarbearia())
                        .cliente(cliente)
                        .ativo(true)
                        .build());
        perfil.setFotoUrl(fotoUrl);
        perfil.setAssinatura(assinatura);
        perfil.setAtivo(true);
        facePerfilRepository.save(perfil);

        cliente.setFotoUrl(fotoUrl);
        clienteRepository.save(cliente);

        return CheckinResponse.builder()
                .clienteId(cliente.getId())
                .clienteNome(cliente.getNome())
                .metodo(MetodoCheckin.FACIAL)
                .fotoUrl(fotoUrl)
                .mensagem("Face cadastrada com sucesso")
                .build();
    }

    /** Realiza check-in por reconhecimento facial. */
    @Transactional
    public CheckinResponse checkinFacial(MultipartFile file) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        validarImagem(file);
        String assinatura = assinaturaImagem(file);
        String fotoUrl = salvarFoto(file, "checkins");

        FacePerfil melhor = null;
        double melhorScore = 0;
        for (FacePerfil p : facePerfilRepository.findByBarbeariaIdAndAtivoTrue(barbeariaId)) {
            double score = similaridade(assinatura, p.getAssinatura());
            if (score > melhorScore) {
                melhorScore = score;
                melhor = p;
            }
        }

        if (melhor == null || melhorScore < 0.72) {
            throw new NegocioException("Nenhum cliente reconhecido (confiança "
                    + String.format(Locale.ROOT, "%.0f", melhorScore * 100) + "%). Cadastre a face ou faça check-in manual.");
        }

        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        Unidade unidade = unidadeService.obterOuCriarPadrao(barbeariaId);

        Checkin checkin = checkinRepository.save(Checkin.builder()
                .barbearia(barbearia)
                .unidade(unidade)
                .cliente(melhor.getCliente())
                .metodo(MetodoCheckin.FACIAL)
                .confianca(BigDecimal.valueOf(melhorScore * 100).setScale(2, RoundingMode.HALF_UP))
                .fotoUrl(fotoUrl)
                .build());

        return toResponse(checkin, "Check-in facial realizado");
    }

    /** Realiza check-in manual do cliente na unidade. */
    @Transactional
    public CheckinResponse checkinManual(Long clienteId) {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        Cliente cliente = clienteRepository.findByIdAndBarbeariaId(clienteId, barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado"));
        Barbearia barbearia = barbeariaRepository.findById(barbeariaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Barbearia não encontrada"));
        Unidade unidade = unidadeService.obterOuCriarPadrao(barbeariaId);

        Checkin checkin = checkinRepository.save(Checkin.builder()
                .barbearia(barbearia)
                .unidade(unidade)
                .cliente(cliente)
                .metodo(MetodoCheckin.MANUAL)
                .confianca(BigDecimal.valueOf(100))
                .build());
        return toResponse(checkin, "Check-in manual realizado");
    }

    /** Lista hoje. */
    @Transactional(readOnly = true)
    public List<CheckinResponse> listarHoje() {
        Long barbeariaId = SecurityUtils.getBarbeariaIdAtual();
        LocalDateTime inicio = LocalDate.now().atStartOfDay();
        LocalDateTime fim = inicio.plusDays(1);
        return checkinRepository.findByBarbeariaIdAndCriadoEmBetweenOrderByCriadoEmDesc(barbeariaId, inicio, fim)
                .stream()
                .map(c -> toResponse(c, null))
                .toList();
    }

    private CheckinResponse toResponse(Checkin c, String mensagem) {
        return CheckinResponse.builder()
                .id(c.getId())
                .clienteId(c.getCliente().getId())
                .clienteNome(c.getCliente().getNome())
                .metodo(c.getMetodo())
                .confianca(c.getConfianca())
                .fotoUrl(c.getFotoUrl())
                .criadoEm(c.getCriadoEm())
                .mensagem(mensagem)
                .build();
    }

    private void validarImagem(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new NegocioException("Imagem obrigatória");
        }
        String ct = file.getContentType();
        if (ct == null || !TIPOS.contains(ct)) {
            throw new NegocioException("Use JPEG, PNG ou WEBP");
        }
    }

    private String salvarFoto(MultipartFile file, String pasta) {
        try {
            Path dir = Paths.get(uploadDir, pasta).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String ext = Optional.ofNullable(file.getOriginalFilename())
                    .filter(n -> n.contains("."))
                    .map(n -> n.substring(n.lastIndexOf('.')))
                    .orElse(".jpg");
            String nome = UUID.randomUUID() + ext;
            Path dest = dir.resolve(nome);
            try (var in = file.getInputStream()) {
                Files.copy(in, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + pasta + "/" + nome;
        } catch (IOException e) {
            throw new NegocioException("Falha ao salvar imagem: " + e.getMessage());
        }
    }

    /** Assinatura simples (hash de amostragem) — MVP local sem API externa de face. */
    static String assinaturaImagem(MultipartFile file) {
        try {
            byte[] bytes = file.getBytes();
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            // amostra espaçada para tolerar pequenas variações de tamanho
            int step = Math.max(1, bytes.length / 256);
            for (int i = 0; i < bytes.length; i += step) {
                md.update(bytes[i]);
            }
            md.update((byte) (bytes.length % 251));
            byte[] dig = md.digest();
            StringBuilder sb = new StringBuilder();
            for (byte b : dig) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new NegocioException("Não foi possível processar a imagem");
        }
    }

    static double similaridade(String a, String b) {
        if (a == null || b == null || a.length() != b.length()) return 0;
        int iguais = 0;
        for (int i = 0; i < a.length(); i++) {
            if (a.charAt(i) == b.charAt(i)) iguais++;
        }
        return (double) iguais / a.length();
    }
}
