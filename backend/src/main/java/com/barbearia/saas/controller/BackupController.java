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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        if (SecurityUtils.getUsuarioAtual().getRole() != Role.ADMIN) {
            throw new NegocioException("Apenas administradores podem executar backup");
        }
        return ResponseEntity.ok(backupService.executar());
    }
}
