package com.ecusol.ms_transacciones.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ecusol.ms_transacciones.dto.MovimientoDTO;
import com.ecusol.ms_transacciones.dto.RespuestaTransferenciaDTO;
import com.ecusol.ms_transacciones.dto.SolicitudTransferenciaDTO;
import com.ecusol.ms_transacciones.service.TransaccionService;

@RestController
@RequestMapping("/api/v1/transacciones")
@RequiredArgsConstructor
@Tag(name = "Cliente Móvil")
public class TransaccionClienteController {

    private final TransaccionService service;

    @Operation(summary = "Iniciar Proceso de Transferencia (SAGA)", description = "Punto de entrada para la banca móvil. Realiza el débito local, comunica al Switch y maneja la compensación si hay fallos.", responses = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Procesado correctamente (Éxito o Fallo controlado)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflicto: Saldo insuficiente en la cuenta origen"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Error interno del servidor")
    })
    @PostMapping("/transferir")
    public ResponseEntity<RespuestaTransferenciaDTO> transferir(@Valid @RequestBody SolicitudTransferenciaDTO dto) {
        RespuestaTransferenciaDTO respuesta = service.realizarTransferencia(dto);

        // Si la transacción falló, devolver HTTP 422 para que el frontend lo detecte
        if ("FAILED".equals(respuesta.getEstado())) {
            return ResponseEntity.status(422).body(respuesta);
        }
        return ResponseEntity.ok(respuesta);
    }

    @GetMapping("/cuenta/{numeroCuenta}")
    @Operation(summary = "Obtener movimientos por número de cuenta")
    public ResponseEntity<List<MovimientoDTO>> obtenerMovimientos(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(service.obtenerMovimientosPorCuenta(numeroCuenta));
    }

    @PostMapping("/devoluciones")
    @Operation(summary = "Solicitar Devolución de Transferencia Recibida")
    public ResponseEntity<java.util.Map<String, String>> solicitarDevolucion(
            @RequestBody java.util.Map<String, String> payload) {
        String originalId = payload.get("originalInstructionId");
        String motivo = payload.get("motivo");
        String numeroCuenta = payload.get("numeroCuentaPropietaria");

        // Simple validation
        if (originalId == null || numeroCuenta == null) {
            return ResponseEntity.badRequest().body(java.util.Map.of("message", "Faltan datos requeridos"));
        }

        try {
            service.solicitarDevolucion(originalId, motivo, numeroCuenta);
            return ResponseEntity.ok(java.util.Map.of("message", "Solicitud de devolución enviada correctamente"));
        } catch (Exception e) {
            return ResponseEntity.status(400).body(java.util.Map.of("message", e.getMessage()));
        }
    }
}