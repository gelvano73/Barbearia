package com.barbearia.saas.controller;

import com.barbearia.saas.dto.agendamento.AgendamentoRequest;
import com.barbearia.saas.dto.agendamento.AgendamentoResponse;
import com.barbearia.saas.dto.agendamento.AtualizarStatusRequest;
import com.barbearia.saas.dto.portal.HorarioDisponivelResponse;
import com.barbearia.saas.service.AgendamentoService;
import com.barbearia.saas.service.HorarioDisponivelService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/** Endpoints REST para CRUD e gestão de status de agendamentos. */
@RestController
@RequestMapping("/api/agendamentos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Agendamentos", description = "Gestão de agendamentos")
public class AgendamentoController {

    private final AgendamentoService agendamentoService;
    private final HorarioDisponivelService horarioDisponivelService;

    /** Listar agendamentos (filtros opcionais por data e barbeiro). */
    @GetMapping
    @Operation(summary = "Listar agendamentos (filtros opcionais por data e barbeiro)")
    public ResponseEntity<List<AgendamentoResponse>> listar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) Long barbeiroId) {
        return ResponseEntity.ok(agendamentoService.listar(data, barbeiroId));
    }

    /** Listar horários livres (agenda inteligente). */
    @GetMapping("/horarios-disponiveis")
    @Operation(summary = "Listar horários livres (agenda inteligente)")
    public ResponseEntity<List<HorarioDisponivelResponse>> horariosDisponiveis(
            @RequestParam(required = false) Long barbeiroId,
            @RequestParam(required = false) Long servicoId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(defaultValue = "40") int limite) {
        return ResponseEntity.ok(horarioDisponivelService.listar(barbeiroId, servicoId, data, limite));
    }

    /** Buscar agendamento por ID. */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar agendamento por ID")
    public ResponseEntity<AgendamentoResponse> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(agendamentoService.buscarPorId(id));
    }

    /** Criar agendamento. */
    @PostMapping
    @Operation(summary = "Criar agendamento")
    public ResponseEntity<AgendamentoResponse> criar(@Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(agendamentoService.criar(request));
    }

    /** Atualizar agendamento. */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar agendamento")
    public ResponseEntity<AgendamentoResponse> atualizar(
            @PathVariable Long id, @Valid @RequestBody AgendamentoRequest request) {
        return ResponseEntity.ok(agendamentoService.atualizar(id, request));
    }

    /** Atualizar status do agendamento. */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status do agendamento")
    public ResponseEntity<AgendamentoResponse> atualizarStatus(
            @PathVariable Long id, @Valid @RequestBody AtualizarStatusRequest request) {
        return ResponseEntity.ok(agendamentoService.atualizarStatus(id, request.getStatus()));
    }

    /** Cancelar agendamento. */
    @DeleteMapping("/{id}")
    @Operation(summary = "Cancelar agendamento")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        agendamentoService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}
