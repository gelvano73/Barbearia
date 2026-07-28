package com.barbearia.saas.service;

import com.barbearia.saas.exception.NegocioException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/** Armazenamento e resolução de URLs de fotos de clientes/barbeiros. */
@Service
public class FotoStorageService {

    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    /** Salva. */
    public String salvar(MultipartFile file, String pasta, String prefixo) {
        validar(file);
        try {
            Path dir = Paths.get(uploadDir, pasta).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            String contentType = file.getContentType();
            String ext = "image/png".equals(contentType) ? ".png"
                    : "image/webp".equals(contentType) ? ".webp" : ".jpg";
            String nome = (prefixo == null || prefixo.isBlank() ? "foto" : prefixo)
                    + "-" + UUID.randomUUID() + ext;
            Path dest = dir.resolve(nome);
            try (var in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + pasta + "/" + nome;
        } catch (IOException e) {
            throw new NegocioException("Falha ao salvar foto: " + e.getMessage());
        }
    }

    private void validar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new NegocioException("Arquivo de foto obrigatório");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new NegocioException("Foto deve ter no máximo 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !TIPOS.contains(contentType)) {
            throw new NegocioException("Formato inválido. Use JPEG, PNG ou WEBP");
        }
    }
}
