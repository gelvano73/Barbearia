package com.barbearia.saas.controller;

import com.barbearia.saas.dto.agendamento.AgendamentoResponse;
import com.barbearia.saas.dto.barbeiro.BarbeiroResponse;
import com.barbearia.saas.dto.fidelidade.FidelidadeMeuPainelResponse;
import com.barbearia.saas.dto.ia.IaChatRequest;
import com.barbearia.saas.dto.ia.IaChatResponse;
import com.barbearia.saas.dto.portal.*;
import com.barbearia.saas.dto.servico.ServicoResponse;
import com.barbearia.saas.service.FidelidadeService;
import com.barbearia.saas.service.HorarioDisponivelService;
import com.barbearia.saas.service.IaAtendimentoService;
import com.barbearia.saas.service.PortalClienteService;
import com.barbearia.saas.security.SecurityUtils;
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

/** API do portal do cliente: perfil, agendamentos, avaliações e fidelidade. */
@RestController
@RequestMapping("/api/portal")
@RequiredArgsConstructor
@PreAuthorize("hasRole('CLIENTE')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Portal Cliente", description = "APIs do portal do cliente")
public class PortalClienteController {

    private final PortalClienteService portalClienteService;
    private final IaAtendimentoService iaAtendimentoService;
    private final HorarioDisponivelService horarioDisponivelService;
    private final FidelidadeService fidelidadeService;

    /** === Perfil === */

    /** Obter perfil do cliente. */
    @GetMapping("/perfil")
    @Operation(summary = "Obter perfil do cliente")
    public ResponseEntity<PerfilResponse> perfil() {
        return ResponseEntity.ok(portalClienteService.getPerfil());
    }

    /** Atualiza o perfil do usuário autenticado. */
    @PutMapping("/perfil")
    @Operation(summary = "Atualizar perfil")
    public ResponseEntity<PerfilResponse> atualizarPerfil(@Valid @RequestBody PerfilUpdateRequest request) {
        return ResponseEntity.ok(portalClienteService.atualizarPerfil(request));
    }

    /** Upload de foto do perfil. */
    @PostMapping(value = "/perfil/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload de foto do perfil")
    public ResponseEntity<PerfilResponse> uploadFoto(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(portalClienteService.uploadFoto(file));
    }

    /** === Agendamentos === */

    /** Listar meus agendamentos. */
    @GetMapping("/agendamentos")
    @Operation(summary = "Listar meus agendamentos")
    public ResponseEntity<List<AgendamentoResponse>> meusAgendamentos() {
        return ResponseEntity.ok(portalClienteService.listarMeusAgendamentos());
    }

    /** Lista o histórico de registros do módulo. */
    @GetMapping("/historico")
    @Operation(summary = "Histórico de serviços concluídos")
    public ResponseEntity<List<AgendamentoResponse>> historico() {
        return ResponseEntity.ok(portalClienteService.historico());
    }

    /** === Catálogo === */

    /** Listar barbeiros disponíveis. */
    @GetMapping("/barbeiros")
    @Operation(summary = "Listar barbeiros disponíveis")
    public ResponseEntity<List<BarbeiroResponse>> barbeiros() {
        return ResponseEntity.ok(portalClienteService.listarBarbeiros());
    }

    /** Listar serviços disponíveis. */
    @GetMapping("/servicos")
    @Operation(summary = "Listar serviços disponíveis")
    public ResponseEntity<List<ServicoResponse>> servicos() {
        return ResponseEntity.ok(portalClienteService.listarServicos());
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

    /** === Operações === */

    /** Agendar online. */
    @PostMapping("/agendamentos")
    @Operation(summary = "Agendar online")
    public ResponseEntity<AgendamentoResponse> agendar(@Valid @RequestBody PortalAgendamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalClienteService.agendar(request));
    }

    /** Reagendar atendimento. */
    @PatchMapping("/agendamentos/{id}/reagendar")
    @Operation(summary = "Reagendar atendimento")
    public ResponseEntity<AgendamentoResponse> reagendar(
            @PathVariable Long id, @Valid @RequestBody ReagendarRequest request) {
        return ResponseEntity.ok(portalClienteService.reagendar(id, request));
    }

    /** Cancelar agendamento. */
    @DeleteMapping("/agendamentos/{id}")
    @Operation(summary = "Cancelar agendamento")
    public ResponseEntity<Void> cancelar(@PathVariable Long id) {
        portalClienteService.cancelar(id);
        return ResponseEntity.noContent().build();
    }

    /** === Fidelidade e avaliações === */

    /** Meus pontos, resgates e histórico. */
    @GetMapping("/fidelidade")
    @Operation(summary = "Meus pontos, resgates e histórico")
    public ResponseEntity<FidelidadeMeuPainelResponse> fidelidade() {
        return ResponseEntity.ok(fidelidadeService.meuPainel(SecurityUtils.getClienteIdAtual()));
    }

    /** Avaliar barbeiro após atendimento. */
    @PostMapping("/avaliacoes")
    @Operation(summary = "Avaliar barbeiro após atendimento")
    public ResponseEntity<AvaliacaoResponse> avaliar(@Valid @RequestBody AvaliacaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(portalClienteService.avaliar(request));
    }

    /** === IA === */

    /** IA de atendimento: responde, sugere serviços e agenda. */
    @PostMapping("/ia/chat")
    @Operation(summary = "IA de atendimento: responde, sugere serviços e agenda")
    public ResponseEntity<IaChatResponse> iaChat(@Valid @RequestBody IaChatRequest request) {
        return ResponseEntity.ok(iaAtendimentoService.chat(request));
    }
}
