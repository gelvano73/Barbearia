package com.barbearia.saas.controller;

import com.barbearia.saas.dto.checkin.CheckinResponse;
import com.barbearia.saas.service.CheckinFacialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/** Endpoints de check-in presencial, inclusive por reconhecimento facial. */
@RestController
@RequestMapping("/api/checkin")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Check-in facial", description = "Cadastro de face e check-in do cliente")
public class CheckinController {

    private final CheckinFacialService checkinFacialService;

    /** Check-ins de hoje. */
    @GetMapping("/hoje")
    @Operation(summary = "Check-ins de hoje")
    public ResponseEntity<List<CheckinResponse>> hoje() {
        return ResponseEntity.ok(checkinFacialService.listarHoje());
    }

    /** Check-in manual por cliente. */
    @PostMapping("/manual/{clienteId}")
    @Operation(summary = "Check-in manual por cliente")
    public ResponseEntity<CheckinResponse> manual(@PathVariable Long clienteId) {
        return ResponseEntity.ok(checkinFacialService.checkinManual(clienteId));
    }

    /** Cadastra o perfil facial do cliente. */
    @PostMapping(value = "/face/{clienteId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Cadastrar face do cliente")
    public ResponseEntity<CheckinResponse> cadastrarFace(
            @PathVariable Long clienteId, @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(checkinFacialService.registrarFace(clienteId, file));
    }

    /** Check-in por reconhecimento facial. */
    @PostMapping(value = "/facial", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Check-in por reconhecimento facial")
    public ResponseEntity<CheckinResponse> facial(@RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(checkinFacialService.checkinFacial(file));
    }
}
