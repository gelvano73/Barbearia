package com.barbearia.saas.controller;

import com.barbearia.saas.dto.agendamento.AgendamentoResponse;
import com.barbearia.saas.dto.agendamento.AtualizarStatusRequest;
import com.barbearia.saas.dto.portal.AvaliacaoResponse;
import com.barbearia.saas.dto.portalbarbeiro.*;
import com.barbearia.saas.service.PortalBarbeiroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

/** API do portal do barbeiro: agenda, comissões, férias, horários e meta. */
@RestController
@RequestMapping("/api/barbeiro")
@RequiredArgsConstructor
@PreAuthorize("hasRole('BARBEIRO')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Portal Barbeiro", description = "Agenda, horários, férias, comissões e dashboard")
public class PortalBarbeiroController {

    private final PortalBarbeiroService portalBarbeiroService;

    /** === Perfil e dashboard === */

    /** Perfil do barbeiro logado. */
    @GetMapping("/perfil")
    @Operation(summary = "Perfil do barbeiro logado")
    public ResponseEntity<BarbeiroPerfilResponse> perfil() {
        return ResponseEntity.ok(portalBarbeiroService.perfil());
    }

    /** Upload de foto do próprio perfil. */
    @PostMapping(value = "/perfil/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de foto do próprio perfil")
    public ResponseEntity<BarbeiroPerfilResponse> uploadFoto(@RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(portalBarbeiroService.uploadFoto(arquivo));
    }

    /** Dashboard pessoal do barbeiro. */
    @GetMapping("/dashboard")
    @Operation(summary = "Dashboard pessoal do barbeiro")
    public ResponseEntity<BarbeiroDashboardResponse> dashboard() {
        return ResponseEntity.ok(portalBarbeiroService.dashboard());
    }

    /** === Agenda === */

    /** Lista a agenda de atendimentos do barbeiro. */
    @GetMapping("/agenda")
    @Operation(summary = "Agenda própria por data")
    public ResponseEntity<List<AgendamentoResponse>> agenda(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return ResponseEntity.ok(portalBarbeiroService.agenda(data));
    }

    /** Lista o histórico de registros do módulo. */
    @GetMapping("/historico")
    @Operation(summary = "Histórico de atendimentos concluídos")
    public ResponseEntity<List<AgendamentoResponse>> historico() {
        return ResponseEntity.ok(portalBarbeiroService.historico());
    }

    /** Atualizar status do próprio agendamento. */
    @PatchMapping("/agendamentos/{id}/status")
    @Operation(summary = "Atualizar status do próprio agendamento")
    public ResponseEntity<AgendamentoResponse> atualizarStatus(
            @PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest request) {
        return ResponseEntity.ok(portalBarbeiroService.atualizarStatus(id, request.getStatus()));
    }

    /** === Horários e férias === */

    /** Listar horários de trabalho. */
    @GetMapping("/horarios")
    @Operation(summary = "Listar horários de trabalho")
    public ResponseEntity<List<HorarioResponse>> horarios() {
        return ResponseEntity.ok(portalBarbeiroService.listarHorarios());
    }

    /** Salvar gestão de horários semanais. */
    @PutMapping("/horarios")
    @Operation(summary = "Salvar gestão de horários semanais")
    public ResponseEntity<List<HorarioResponse>> salvarHorarios(@Valid @RequestBody HorariosBatchRequest request) {
        return ResponseEntity.ok(portalBarbeiroService.salvarHorarios(request));
    }

    /** Listar férias/folgas. */
    @GetMapping("/ferias")
    @Operation(summary = "Listar férias/folgas")
    public ResponseEntity<List<FeriasResponse>> ferias() {
        return ResponseEntity.ok(portalBarbeiroService.listarFerias());
    }

    /** Solicitar férias. */
    @PostMapping("/ferias")
    @Operation(summary = "Solicitar férias")
    public ResponseEntity<FeriasResponse> solicitarFerias(@Valid @RequestBody FeriasRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalBarbeiroService.solicitarFerias(request));
    }

    /** Cancelar solicitação de férias. */
    @DeleteMapping("/ferias/{id}")
    @Operation(summary = "Cancelar solicitação de férias")
    public ResponseEntity<Void> cancelarFerias(@PathVariable Long id) {
        portalBarbeiroService.cancelarFerias(id);
        return ResponseEntity.noContent().build();
    }

    /** === Comissões e metas === */

    /** Comissões do período. */
    @GetMapping("/comissoes")
    @Operation(summary = "Comissões do período")
    public ResponseEntity<List<ComissaoResponse>> comissoes(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(portalBarbeiroService.listarComissoes(ano, mes));
    }

    /** Avaliações recebidas. */
    @GetMapping("/avaliacoes")
    @Operation(summary = "Avaliações recebidas")
    public ResponseEntity<List<AvaliacaoResponse>> avaliacoes() {
        return ResponseEntity.ok(portalBarbeiroService.listarAvaliacoes());
    }

    /** Progresso da meta mensal. */
    @GetMapping("/meta")
    @Operation(summary = "Progresso da meta mensal")
    public ResponseEntity<MetaProgressoResponse> meta(
            @RequestParam(required = false) Integer ano,
            @RequestParam(required = false) Integer mes) {
        return ResponseEntity.ok(portalBarbeiroService.metaProgresso(ano, mes));
    }
}
