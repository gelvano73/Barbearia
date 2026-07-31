package com.barbearia.saas.service;

import com.barbearia.saas.config.BackupProperties;
import com.barbearia.saas.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Testes unitários do serviço de backup. */
@ExtendWith(MockitoExtension.class)
class BackupServiceTest {

    @Mock
    private BackupProperties properties;

    @Mock
    private StorageProperties storageProperties;

    @Mock
    private Environment environment;

    @InjectMocks
    private BackupService backupService;

    @TempDir
    Path temp;

    @Test
    void deveGerarBackupComPastaOk() throws Exception {
        Path backupRoot = temp.resolve("backups");
        Path uploads = temp.resolve("uploads");
        Files.createDirectories(uploads);
        Files.writeString(uploads.resolve("foto.txt"), "x");

        when(properties.getDir()).thenReturn(backupRoot.toString());
        when(properties.getRetentionDays()).thenReturn(7);
        when(storageProperties.isS3()).thenReturn(false);
        ReflectionTestUtils.setField(backupService, "uploadDir", uploads.toString());
        ReflectionTestUtils.setField(backupService, "datasourceUrl", "jdbc:postgresql://localhost:5432/barbearia_saas");

        Map<String, Object> result = backupService.executar();

        assertThat(result.get("status")).isEqualTo("ok");
        assertThat(result.get("uploads")).isEqualTo("ok");
        assertThat(result.get("s3")).isEqualTo("desabilitado");
        Path pasta = Path.of(result.get("pasta").toString());
        assertThat(Files.exists(pasta.resolve("OK"))).isTrue();
        assertThat(Files.exists(pasta.resolve("uploads.zip"))).isTrue();
        assertThat(result.get("banco")).isIn("ok", "pulado");
    }
}
