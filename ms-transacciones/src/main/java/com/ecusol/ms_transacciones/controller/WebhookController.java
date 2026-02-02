package com.ecusol.ms_transacciones.controller;

import com.ecusol.ms_transacciones.dto.iso.IsoMensajeDTO;
import com.ecusol.ms_transacciones.model.Transaccion;
import com.ecusol.ms_transacciones.repository.TransaccionRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/core/transacciones")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final TransaccionRepository repository;
    private final com.ecusol.ms_transacciones.client.CuentaClient cuentaClient;
    private final com.ecusol.ms_transacciones.service.TransaccionService transaccionService;
    private final com.ecusol.ms_transacciones.client.ClienteClient clienteClient;

    @PostMapping("/webhook")
    public ResponseEntity<Map<String, Object>> recibirTransferencia(@RequestBody IsoMensajeDTO mensaje) {
        String messageId = mensaje.getHeader().getMessageId();
        String instructionId = mensaje.getBody().getInstructionId();
        String bancoOrigen = mensaje.getHeader().getOriginatingBankId();
        String cuentaDestino = mensaje.getBody().getCreditor().getAccountId();

        log.info("📥 Webhook: Recibida transferencia ISO 20022");
        log.info("   ├─ Message ID: {}", messageId);
        log.info("   ├─ Instruction ID: {}", instructionId);
        log.info("   ├─ Banco Origen: {}", bancoOrigen);
        log.info("   ├─ Cuenta Creditor: {}", cuentaDestino);
        log.info("   └─ Monto: {} {}",
                mensaje.getBody().getAmount().getValue(),
                mensaje.getBody().getAmount().getCurrency());

        // --- ACCOUNT LOOKUP (Validación de Nombre) ---
        if ("acmt.023.001.02".equals(mensaje.getHeader().getMessageNamespace())) {
            log.info("🔍 Lookup Request: Validando cuenta {}", cuentaDestino);
            Map<String, Object> data = new HashMap<>();

            try {
                // 1. Buscar Cuenta
                var cuentaInfo = cuentaClient.obtenerCuentaPorNumero(cuentaDestino);

                if (cuentaInfo != null) {
                    // 2. Buscar Cliente (Dueño)
                    var clienteInfo = clienteClient.obtenerCliente(cuentaInfo.getClienteId());
                    String nombreCompleto = (clienteInfo != null)
                            ? clienteInfo.getNombres() + " " + clienteInfo.getApellidos()
                            : "NOMBRE NO DISPONIBLE";

                    data.put("exists", true);
                    data.put("ownerName", nombreCompleto);
                    data.put("currency", "USD");
                    data.put("status", cuentaInfo.getEstado());

                    return ResponseEntity.ok(Map.of("status", "SUCCESS", "data", data));
                } else {
                    data.put("exists", false);
                    data.put("mensaje", "Cuenta no encontrada");
                    return ResponseEntity.ok(Map.of("status", "FAILED", "data", data));
                }
            } catch (Exception e) {
                log.error("Error en Lookup: {}", e.getMessage());
                data.put("exists", false);
                data.put("mensaje", "Error interno");
                return ResponseEntity.ok(Map.of("status", "FAILED", "data", data));
            }
        }

        // --- VALIDACIÓN SÍNCRONA (Anti-Ghost Money) ---
        try {
            String estadoCuenta = cuentaClient.validarCuenta(cuentaDestino);

            if (estadoCuenta == null) {
                log.warn("⚠️ Rechazo Inmediato: Cuenta Destino NO EXISTE: {}", cuentaDestino);
                return ResponseEntity.status(404).body(Map.of(
                        "codigo", "AC01",
                        "mensaje", "Cuenta inexistente"));
            }

            if ("BLOQUEADA".equalsIgnoreCase(estadoCuenta)) {
                log.warn("⚠️ Rechazo Inmediato: Cuenta BLOQUEADA: {}", cuentaDestino);
                return ResponseEntity.status(422).body(Map.of(
                        "codigo", "AG01",
                        "mensaje", "Cuenta bloqueada"));
            }

            if ("INACTIVA".equalsIgnoreCase(estadoCuenta) || "CERRADA".equalsIgnoreCase(estadoCuenta)) {
                log.warn("⚠️ Rechazo Inmediato: Cuenta CERRADA/INACTIVA: {}", cuentaDestino);
                return ResponseEntity.status(422).body(Map.of(
                        "codigo", "AC04",
                        "mensaje", "Cuenta cerrada"));
            }

        } catch (Exception e) {
            log.error("Error validando cuenta {}: {}", cuentaDestino, e.getMessage());
            return ResponseEntity.status(503).body(Map.of(
                    "codigo", "MS03",
                    "mensaje", "Error técnico validando cuenta destino"));
        }
        // ----------------------------------------------

        Map<String, Object> respuesta = new HashMap<>();

        try {
            log.info("Processing synchronous transfer...");

            // 1. Idempotency Check
            if (repository.existsByInstructionId(instructionId)) {
                log.warn("⚠️ Duplicado detectado: {}", instructionId);
                respuesta.put("status", "DUPLICATE");
                respuesta.put("messageId", messageId);
                respuesta.put("instructionId", instructionId);
                respuesta.put("message", "Transaccion duplicada");
                return ResponseEntity.ok(respuesta);
            }

            // 2. Credit Account
            java.math.BigDecimal monto = mensaje.getBody().getAmount().getValue();
            cuentaClient.acreditar(cuentaDestino, monto);

            // 3. Save Transaction
            Transaccion tx = new Transaccion();
            tx.setInstructionId(instructionId);
            tx.setReferencia(mensaje.getBody().getEndToEndId());
            tx.setCuentaOrigen(mensaje.getBody().getDebtor().getAccountId());
            tx.setCuentaDestino(cuentaDestino);
            tx.setMonto(monto);
            tx.setDescripcion(
                    mensaje.getBody().getRemittanceInformation() != null ? mensaje.getBody().getRemittanceInformation()
                            : "Transferencia Entrante");
            tx.setEstado("COMPLETED");
            tx.setRolTransaccion("CREDITO");
            tx.setFechaEjecucion(LocalDateTime.now());

            repository.save(tx);

            log.info("✅ Mensaje procesado exitosamente (Sync) para ID: {}", instructionId);

            respuesta.put("status", "PROCESSED");
            respuesta.put("messageId", messageId);
            respuesta.put("instructionId", instructionId);
            respuesta.put("message", "Transferencia procesada exitosamente");

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error processing transaction: {}", e.getMessage(), e);

            respuesta.put("status", "ERROR");
            respuesta.put("messageId", messageId);
            respuesta.put("instructionId", instructionId);
            respuesta.put("message", "Error al procesar: " + e.getMessage());

            // Return 500
            return ResponseEntity.status(500).body(respuesta);
        }
    }

    // --- NUEVO ENDPOINT PARA DEVOLUCIONES (RETURNS) ---
    // Mapeamos ambas rutas posibles (estándar y con sufijo que agrega el switch)
    @PostMapping({ "/webhook/return", "/webhook/api/incoming/return" })
    public ResponseEntity<Map<String, Object>> recibirDevolucion(
            @RequestBody com.ecusol.ms_transacciones.dto.ReturnRequestDTO dto) {
        log.info("📥 Webhook: Recibida solicitud de DEVOLUCIÓN (Return)");
        log.info("   ├─ Original Instruction ID: {}", dto.getBody().getOriginalInstructionId());
        log.info("   ├─ Return Reason: {}", dto.getBody().getReturnReason());

        try {
            // Procesar Síncronamente (Actualizar estado y acreditar)
            transaccionService.procesarDevolucionEntrante(dto);

            return ResponseEntity.ok(Map.of(
                    "status", "PROCESSED",
                    "originalInstructionId", dto.getBody().getOriginalInstructionId(),
                    "message", "Devolución procesada correctamente"));

        } catch (Exception e) {
            log.error("Error procesando devolución entrante: {}", e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "status", "ERROR",
                    "message", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ms-transacciones",
                "banco", "ECUSOLBK"));
    }
}
