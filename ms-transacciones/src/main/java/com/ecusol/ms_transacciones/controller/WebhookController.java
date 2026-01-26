package com.ecusol.ms_transacciones.controller;

import com.ecusol.ms_transacciones.dto.iso.IsoMensajeDTO;
import com.ecusol.ms_transacciones.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/core/transacciones")
@RequiredArgsConstructor
@Slf4j
public class WebhookController {

    private final RabbitTemplate rabbitTemplate;
    private final com.ecusol.ms_transacciones.client.CuentaClient cuentaClient;

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
            log.info("📤 Encolando en RabbitMQ para procesamiento asincrónico...");

            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.EXCHANGE_ECUSOL,
                    RabbitMQConfig.ROUTING_KEY_RETURNS,
                    mensaje);

            log.info("Mensaje encolado exitosamente en RabbitMQ para ID: {}", instructionId);

            respuesta.put("status", "RECEIVED");
            respuesta.put("messageId", messageId);
            respuesta.put("instructionId", instructionId);
            respuesta.put("message", "Transferencia encolada para procesamiento asincrónico");

            return ResponseEntity.ok(respuesta);

        } catch (Exception e) {
            log.error("Error encolando transferencia en RabbitMQ: {}", e.getMessage(), e);

            respuesta.put("status", "ERROR");
            respuesta.put("messageId", messageId);
            respuesta.put("instructionId", instructionId);
            respuesta.put("message", "Error al procesar: " + e.getMessage());

            // Retornar 500 para que Switch reintente
            return ResponseEntity.status(500).body(respuesta);
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "ms-transacciones",
                "banco", "ECUSOLBK",
                "rabbitmq", "CONNECTED"));
    }
}
