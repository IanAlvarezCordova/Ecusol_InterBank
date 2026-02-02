package com.ecusol.ms_transacciones.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.ecusol.ms_transacciones.client.CuentaClient;
import com.ecusol.ms_transacciones.infrastructure.outbound.SwitchClient;

import com.ecusol.ms_transacciones.dto.BancoDTO;
import com.ecusol.ms_transacciones.dto.SwitchWebhookResponse;
import com.ecusol.ms_transacciones.model.Transaccion;
import com.ecusol.ms_transacciones.repository.TransaccionRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transacciones")
@RequiredArgsConstructor
@Tag(name = "Switch DIGICONECU", description = "Endpoints para comunicación con el Switch Interbancario")
@Slf4j
public class TransaccionInterbancariaController {

        private final TransaccionRepository repository;
        private final CuentaClient cuentaClient;
        private final SwitchClient switchClient;

        /**
         * Webhook que recibe transferencias entrantes desde el Switch DIGICONECU.
         * Formato esperado por el Switch.
         */
        @Operation(summary = "Recibir transferencia entrante desde otro banco via Switch")
        @PostMapping("/webhook")
        public ResponseEntity<SwitchWebhookResponse> recibirTransferenciaEntrante(
                        @RequestBody com.ecusol.ms_transacciones.dto.iso.IsoMensajeDTO msg) {

                String bancoOrigen = msg.getHeader() != null ? msg.getHeader().getOriginatingBankId() : "DESCONOCIDO";
                String cuentaOrigen = msg.getBody() != null && msg.getBody().getDebtor() != null
                                ? msg.getBody().getDebtor().getAccountId()
                                : "DESCONOCIDO";
                String cuentaDestino = msg.getBody() != null && msg.getBody().getCreditor() != null
                                ? msg.getBody().getCreditor().getAccountId()
                                : "DESCONOCIDO";
                java.math.BigDecimal monto = msg.getBody() != null && msg.getBody().getAmount() != null
                                ? msg.getBody().getAmount().getValue()
                                : java.math.BigDecimal.ZERO;
                String referencia = msg.getBody() != null ? msg.getBody().getInstructionId() : "REF-UNKNOWN";
                String concepto = msg.getBody() != null ? msg.getBody().getRemittanceInformation()
                                : "Transferencia Entrante";

                log.info("📥 Webhook recibido desde {}: {} -> {} por ${}",
                                bancoOrigen,
                                cuentaOrigen,
                                cuentaDestino,
                                monto);

                try {
                        // 1. Verificar idempotencia (no procesar duplicados)
                        if (referencia != null &&
                                        repository.existsByInstructionId(referencia)) {
                                log.warn("⚠️ Transferencia duplicada ignorada: {}", referencia);
                                return ResponseEntity.ok(new SwitchWebhookResponse(
                                                "ACK",
                                                "Transferencia ya procesada previamente",
                                                referencia));
                        }

                        // 2. Acreditar la cuenta destino
                        cuentaClient.acreditar(cuentaDestino, monto);

                        // 3. Registrar la transacción entrante
                        Transaccion tx = new Transaccion();
                        tx.setInstructionId(referencia);
                        tx.setReferencia(referencia);
                        tx.setCuentaOrigen(cuentaOrigen);
                        tx.setCuentaDestino(cuentaDestino);
                        tx.setMonto(monto);
                        tx.setDescripcion(concepto != null ? concepto
                                        : "Transferencia recibida de " + bancoOrigen);
                        tx.setEstado("COMPLETED");
                        tx.setRolTransaccion("CREDITO");
                        tx.setFechaEjecucion(LocalDateTime.now());
                        // Guardamos el banco origen también si la entidad lo soporta (opcional)
                        // tx.setIdBancoOrigen(...);

                        repository.save(tx);

                        log.info("✅ Transferencia acreditada exitosamente en cuenta {}", cuentaDestino);

                        return ResponseEntity.ok(new SwitchWebhookResponse(
                                        "ACK",
                                        "Transferencia procesada exitosamente",
                                        referencia));

                } catch (Exception e) {
                        log.error("❌ Error procesando webhook: {}", e.getMessage());
                        return ResponseEntity.status(422).body(new SwitchWebhookResponse(
                                        "NACK",
                                        "Error: " + e.getMessage(),
                                        referencia));
                }
        }

        /**
         * Obtiene la lista de bancos disponibles en el ecosistema DIGICONECU.
         * El frontend usa esto para mostrar el combo de bancos destino.
         */
        @Operation(summary = "Obtener lista de bancos del ecosistema DIGICONECU")
        @GetMapping("/bancos")
        public ResponseEntity<List<BancoDTO>> obtenerBancos() {
                List<BancoDTO> bancos = switchClient.obtenerBancos();
                return ResponseEntity.ok(bancos);
        }

        /**
         * Health check del servicio de transacciones.
         */
        @GetMapping("/health")
        public ResponseEntity<Map<String, String>> health() {
                return ResponseEntity.ok(Map.of(
                                "status", "UP",
                                "service", "ms-transacciones",
                                "banco", switchClient.getBancoCodigo()));
        }

        @Operation(summary = "Validar cuenta en banco destino (Account Lookup)")
        @PostMapping("/validar-cuenta")
        public ResponseEntity<com.ecusol.ms_transacciones.dto.AccountLookupResponse> validarCuentaExterna(
                        @RequestBody com.ecusol.ms_transacciones.dto.AccountLookupRequest.Body requestBody) {

                // Build full request with Header
                var fullRequest = com.ecusol.ms_transacciones.dto.AccountLookupRequest.builder()
                                .header(com.ecusol.ms_transacciones.dto.AccountLookupRequest.Header.builder()
                                                .originatingBankId(switchClient.getBancoCodigo())
                                                .build())
                                .body(requestBody)
                                .build();

                return ResponseEntity.ok(switchClient.realizarLookup(fullRequest));
        }
}