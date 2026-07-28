package com.barbearia.saas.service;

import com.barbearia.saas.config.BackupProperties;
import com.barbearia.saas.exception.NegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Executa e gerencia backups do banco de dados da aplicação. */
@Service
@RequiredArgsConstructor
@Slf4j
public class BackupService {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    private final BackupProperties properties;
    private final Environment environment;

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Value("${spring.datasource.url:}")
    private String datasourceUrl;

    /** Executa um backup do banco de dados. */
    public Map<String, Object> executar() {
        Path root = Paths.get(properties.getDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new NegocioException("Não foi possível criar pasta de backup: " + e.getMessage());
        }

        String stamp = LocalDateTime.now().format(TS);
        Path dest = root.resolve(stamp);
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("pasta", dest.toString());
        resultado.put("inicio", LocalDateTime.now().toString());

        try {
            Files.createDirectories(dest);

            boolean dbOk = backupBanco(dest);
            resultado.put("banco", dbOk ? "ok" : "pulado");

            boolean uploadsOk = backupUploads(dest);
            resultado.put("uploads", uploadsOk ? "ok" : "vazio_ou_ausente");

            Files.writeString(dest.resolve("OK"), stamp + "\n");
            limparAntigos(root);

            resultado.put("status", "ok");
            resultado.put("fim", LocalDateTime.now().toString());
            log.info("Backup concluído em {}", dest);
            return resultado;
        } catch (Exception e) {
            log.error("Falha no backup: {}", e.getMessage(), e);
            throw new NegocioException("Falha no backup: " + e.getMessage());
        }
    }

    private boolean backupBanco(Path dest) throws IOException, InterruptedException {
        if (datasourceUrl != null && datasourceUrl.contains("postgresql")) {
            return backupPostgres(dest);
        }
        if (datasourceUrl != null && datasourceUrl.contains("h2:")) {
            return backupH2(dest);
        }
        log.warn("Datasource não suportado para dump automático: {}", datasourceUrl);
        return false;
    }

    private boolean backupPostgres(Path dest) throws IOException, InterruptedException {
        Path dump = dest.resolve("db.dump");
        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-Fc",
                "-f", dump.toString()
        );
        pb.environment().putIfAbsent("PGHOST", envOr("PGHOST", hostFromJdbc()));
        pb.environment().putIfAbsent("PGPORT", envOr("PGPORT", "5432"));
        pb.environment().putIfAbsent("PGUSER", envOr("PGUSER", environment.getProperty("spring.datasource.username", "barbearia")));
        pb.environment().putIfAbsent("PGDATABASE", envOr("PGDATABASE", "barbearia_saas"));
        String password = environment.getProperty("spring.datasource.password");
        if (password != null) {
            pb.environment().put("PGPASSWORD", password);
        }
        pb.redirectErrorStream(true);

        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            log.warn("pg_dump indisponível neste ambiente (use o sidecar Docker). {}", e.getMessage());
            Files.writeString(dest.resolve("db-SKIPPED.txt"),
                    "pg_dump não encontrado. Em Docker use o serviço barbearia-backup.\n");
            return false;
        }

        String output = new String(process.getInputStream().readAllBytes());
        boolean finished = process.waitFor(10, TimeUnit.MINUTES);
        if (!finished || process.exitValue() != 0) {
            log.warn("pg_dump falhou: {}", output);
            Files.writeString(dest.resolve("db-ERROR.txt"), output);
            return false;
        }
        return Files.exists(dump);
    }

    private boolean backupH2(Path dest) throws IOException {
        Path dataDir = Paths.get("data").toAbsolutePath().normalize();
        if (!Files.isDirectory(dataDir)) {
            Files.writeString(dest.resolve("h2-SKIPPED.txt"), "Pasta data/ não encontrada\n");
            return false;
        }
        Path zipPath = dest.resolve("h2-data.zip");
        zipDirectory(dataDir, zipPath);
        return Files.exists(zipPath);
    }

    private boolean backupUploads(Path dest) throws IOException {
        Path uploads = Paths.get(uploadDir).toAbsolutePath().normalize();
        if (!Files.isDirectory(uploads)) {
            return false;
        }
        try (Stream<Path> walk = Files.walk(uploads)) {
            boolean hasFiles = walk.anyMatch(p -> !p.equals(uploads) && Files.isRegularFile(p));
            if (!hasFiles) {
                return false;
            }
        }
        zipDirectory(uploads, dest.resolve("uploads.zip"));
        return true;
    }

    private void limparAntigos(Path root) throws IOException {
        int dias = Math.max(1, properties.getRetentionDays());
        long cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(dias);
        try (Stream<Path> dirs = Files.list(root)) {
            dirs.filter(Files::isDirectory)
                    .filter(p -> {
                        try {
                            BasicFileAttributes attrs = Files.readAttributes(p, BasicFileAttributes.class);
                            return attrs.creationTime().toMillis() < cutoff;
                        } catch (IOException e) {
                            return false;
                        }
                    })
                    .sorted(Comparator.comparing(Path::getFileName))
                    .forEach(p -> {
                        try {
                            deleteRecursive(p);
                            log.info("Backup antigo removido: {}", p);
                        } catch (IOException e) {
                            log.warn("Não removeu {}: {}", p, e.getMessage());
                        }
                    });
        }
    }

    private void zipDirectory(Path sourceDir, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath));
             Stream<Path> walk = Files.walk(sourceDir)) {
            walk.filter(Files::isRegularFile).forEach(file -> {
                try {
                    String entryName = sourceDir.relativize(file).toString().replace('\\', '/');
                    zos.putNextEntry(new ZipEntry(entryName));
                    try (InputStream in = Files.newInputStream(file)) {
                        in.transferTo(zos);
                    }
                    zos.closeEntry();
                } catch (IOException e) {
                    log.warn("Arquivo ignorado no backup (em uso?): {} — {}", file, e.getMessage());
                }
            });
        }
    }

    private void deleteRecursive(Path path) throws IOException {
        if (!Files.exists(path)) return;
        try (Stream<Path> walk = Files.walk(path)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.deleteIfExists(p);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    private String hostFromJdbc() {
        // jdbc:postgresql://host:5432/db
        try {
            String withoutPrefix = datasourceUrl.substring(datasourceUrl.indexOf("://") + 3);
            String hostPort = withoutPrefix.split("/")[0];
            return hostPort.split(":")[0];
        } catch (Exception e) {
            return "localhost";
        }
    }

    private String envOr(String key, String fallback) {
        String v = System.getenv(key);
        return v != null && !v.isBlank() ? v : fallback;
    }
}
