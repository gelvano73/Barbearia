package com.barbearia.saas.service;

import com.barbearia.saas.config.StorageProperties;
import com.barbearia.saas.exception.NegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

/** Armazenamento e resolução de URLs de fotos de clientes/barbeiros (disco local ou S3/MinIO). */
@Service
@RequiredArgsConstructor
@Slf4j
public class FotoStorageService {

    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Set<String> TIPOS = Set.of("image/jpeg", "image/png", "image/webp");

    private final StorageProperties storageProperties;

    private volatile S3Client s3Client;

    /** Salva o arquivo no provedor configurado (local ou S3) e retorna a URL pública. */
    public String salvar(MultipartFile file, String pasta, String prefixo) {
        validar(file);
        String ext = resolverExtensao(file.getContentType());
        String nome = (prefixo == null || prefixo.isBlank() ? "foto" : prefixo)
                + "-" + UUID.randomUUID() + ext;

        return storageProperties.isS3()
                ? salvarNoS3(file, pasta, nome)
                : salvarLocal(file, pasta, nome);
    }

    private String salvarLocal(MultipartFile file, String pasta, String nome) {
        try {
            Path dir = Paths.get(storageProperties.getDir(), pasta).toAbsolutePath().normalize();
            Files.createDirectories(dir);
            Path dest = dir.resolve(nome);
            try (var in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            return "/uploads/" + pasta + "/" + nome;
        } catch (IOException e) {
            throw new NegocioException("Falha ao salvar foto: " + e.getMessage());
        }
    }

    private String salvarNoS3(MultipartFile file, String pasta, String nome) {
        String key = pasta + "/" + nome;
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(storageProperties.getS3Bucket())
                    .key(key)
                    .contentType(file.getContentType())
                    .build();
            getOrCreateS3Client().putObject(request,
                    software.amazon.awssdk.core.sync.RequestBody.fromInputStream(
                            file.getInputStream(), file.getSize()));
            return resolverUrlPublicaS3(key);
        } catch (IOException e) {
            throw new NegocioException("Falha ao salvar foto: " + e.getMessage());
        } catch (Exception e) {
            log.error("Falha ao enviar foto para S3: {}", e.getMessage(), e);
            throw new NegocioException("Falha ao salvar foto no armazenamento remoto");
        }
    }

    private String resolverUrlPublicaS3(String key) {
        String base = storageProperties.getS3PublicBaseUrl();
        if (base != null && !base.isBlank()) {
            return base.replaceAll("/+$", "") + "/" + key;
        }
        String endpoint = storageProperties.getS3Endpoint();
        if (endpoint != null && !endpoint.isBlank()) {
            return endpoint.replaceAll("/+$", "") + "/" + storageProperties.getS3Bucket() + "/" + key;
        }
        return "https://" + storageProperties.getS3Bucket() + ".s3." + storageProperties.getS3Region()
                + ".amazonaws.com/" + key;
    }

    private S3Client getOrCreateS3Client() {
        if (s3Client == null) {
            synchronized (this) {
                if (s3Client == null) {
                    var builder = S3Client.builder()
                            .region(Region.of(storageProperties.getS3Region()));

                    String endpoint = storageProperties.getS3Endpoint();
                    if (endpoint != null && !endpoint.isBlank()) {
                        builder = builder.endpointOverride(URI.create(endpoint)).forcePathStyle(true);
                    }

                    String accessKey = storageProperties.getS3AccessKey();
                    String secretKey = storageProperties.getS3SecretKey();
                    if (accessKey != null && !accessKey.isBlank() && secretKey != null && !secretKey.isBlank()) {
                        builder = builder.credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)));
                    }

                    s3Client = builder.build();
                }
            }
        }
        return s3Client;
    }

    private String resolverExtensao(String contentType) {
        return "image/png".equals(contentType) ? ".png"
                : "image/webp".equals(contentType) ? ".webp" : ".jpg";
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
