package com.barbearia.saas.controller;

import com.barbearia.saas.domain.enums.Role;
import com.barbearia.saas.exception.NegocioException;
import com.barbearia.saas.security.SecurityUtils;
import com.barbearia.saas.service.BackupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** Endpoints administrativos para disparar e consultar backups. */
@RestController
@RequestMapping("/api/backup")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Backup", description = "Backup manual (admin)")
public class BackupController {

    private final BackupService backupService;

    /** Executar backup agora (somente ADMIN). */
    @PostMapping("/executar")
    @Operation(summary = "Executar backup agora (somente ADMIN)")
    public ResponseEntity<Map<String, Object>> executar() {
        exigirAdmin();
        return ResponseEntity.ok(backupService.executar());
    }

    /** Lista backups locais recentes (somente ADMIN). */
    @GetMapping
    @Operation(summary = "Listar backups locais recentes")
    public ResponseEntity<List<Map<String, Object>>> listar() {
        exigirAdmin();
        return ResponseEntity.ok(backupService.listar());
    }

    private void exigirAdmin() {
        if (SecurityUtils.getUsuarioAtual().getRole() != Role.ADMIN) {
            throw new NegocioException("Apenas administradores podem gerenciar backup");
        }
    }
}
