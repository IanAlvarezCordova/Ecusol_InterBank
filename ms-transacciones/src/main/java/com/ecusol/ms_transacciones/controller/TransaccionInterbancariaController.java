package com.ecusol.ms_transacciones.controller;

import com.ecusol.ms_transacciones.client.CuentaClient;
import com.ecusol.ms_transacciones.client.SwitchClient;
import com.ecusol.ms_transacciones.dto.BancoDTO;
import com.ecusol.ms_transacciones.dto.SwitchWebhookPayload;
import com.ecusol.ms_transacciones.dto.SwitchWebhookResponse;
import com.ecusol.ms_transacciones.model.Transaccion;
import com.ecusol.ms_transacciones.repository.TransaccionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Controlador para integración con Switch DIGICONECU.
 * Maneja webhook de transferencias entrantes y consultas de bancos.
 */
@RestController
@RequestMapping("/api/transacciones")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Switch DIGICONECU", description = "Integración con Switch Interbancario")
public class TransaccionInterbancariaController {

    private final TransaccionRepository repository;
    private final CuentaClient cuentaClient;
    private final SwitchClient switchClient;

    /**
     * WEBHOOK: Recibir transferencias de otros bancos.
     * El Switch DIGICONECU llama a este endpoint cuando alguien nos envía dinero.
     */
    @Operation(summary = "Webhook para recibir transferencias entrantes del Switch")
    @PostMapping("/webhook")
    public ResponseEntity<SwitchWebhookResponse> recibirTransferencia(
            @RequestBody SwitchWebhookPayload payload) {

        log.info("📥 Webhook recibido: {} ({}) -> {} por ${}",
                payload.getBancoOrigen(),
                payload.getCuentaOrigen(),
                payload.getCuentaDestino(),
                payload.getMonto());

        try {
            // 1. Verificar idempotencia (no procesar duplicados)
            if (payload.getInstructionId() != null &&
                    repository.existsByInstructionId(payload.getInstructionId())) {
                log.info("⚠️ Transferencia ya procesada: {}", payload.getInstructionId());
                return ResponseEntity.ok(new SwitchWebhookResponse(
                        "ACK", "Transferencia ya procesada anteriormente", payload.getInstructionId()));
            }

            // 2. Acreditar el monto en la cuenta destino
            cuentaClient.acreditar(payload.getCuentaDestino(), payload.getMonto());

            // 3. Registrar la transacción
            Transaccion tx = new Transaccion();
            tx.setInstructionId(payload.getInstructionId());
            tx.setReferencia(payload.getInstructionId());
            tx.setCuentaOrigen(payload.getCuentaOrigen());
            tx.setCuentaDestino(payload.getCuentaDestino());
            tx.setMonto(payload.getMonto());
            tx.setDescripcion("Recibido de " + payload.getBancoOrigen() + ": " +
                    (payload.getConcepto() != null ? payload.getConcepto() : "Transferencia interbancaria"));
            tx.setEstado("COMPLETED");
            tx.setRolTransaccion("CREDITO");
            tx.setFechaEjecucion(LocalDateTime.now());
            repository.save(tx);

            log.info("✅ Transferencia acreditada exitosamente en cuenta {}", payload.getCuentaDestino());

            return ResponseEntity.ok(new SwitchWebhookResponse(
                    "ACK", "Transferencia recibida exitosamente", payload.getInstructionId()));

        } catch (Exception e) {
            log.error("❌ Error procesando webhook: {}", e.getMessage());
            return ResponseEntity.status(500).body(new SwitchWebhookResponse(
                    "ERROR", "Error: " + e.getMessage(), payload.getInstructionId()));
        }
    }

    /**
     * Lista de bancos disponibles en la red (para el frontend).
     */
    @Operation(summary = "Obtener lista de bancos disponibles en la red")
    @GetMapping("/bancos")
    public ResponseEntity<List<BancoDTO>> obtenerBancos() {
        List<BancoDTO> bancos = switchClient.obtenerBancos();
        return ResponseEntity.ok(bancos);
    }

    /**
     * VALIDAR CUENTA: Verificar si una cuenta existe en ECUSOL.
     * El Switch DIGICONECU llama a este endpoint antes de una transferencia.
     */
    @Operation(summary = "Validar existencia de una cuenta en ECUSOL")
    @GetMapping("/validar/{numeroCuenta}")
    public ResponseEntity<com.ecusol.ms_transacciones.dto.AccountValidationResponse> validarCuenta(
            @PathVariable String numeroCuenta) {

        log.info("🔍 Validación de cuenta solicitada: {}", numeroCuenta);

        Object cuenta = cuentaClient.buscarPorNumero(numeroCuenta);

        if (cuenta != null) {
            log.info("✅ Cuenta válida encontrada: {}", numeroCuenta);
            return ResponseEntity.ok(new com.ecusol.ms_transacciones.dto.AccountValidationResponse(
                    true, "Cuenta válida", "TITULAR COMPROBADO", true));
        } else {
            log.warn("❌ Cuenta no encontrada: {}", numeroCuenta);
            return ResponseEntity.ok(new com.ecusol.ms_transacciones.dto.AccountValidationResponse(
                    false, "Cuenta no existe en ECUSOL", null, false));
        }
    }

    /**
     * Health check del servicio.
     */
    @Operation(summary = "Verificar estado del servicio")
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "banco", switchClient.getBancoCodigo(),
                "timestamp", LocalDateTime.now().toString()));
    }
}
