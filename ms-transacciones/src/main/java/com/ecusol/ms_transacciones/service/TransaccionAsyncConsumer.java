package com.ecusol.ms_transacciones.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.web.client.RestTemplate;
import com.ecusol.ms_transacciones.dto.iso.IsoMensajeDTO;
import com.ecusol.ms_transacciones.model.Transaccion;
import com.ecusol.ms_transacciones.repository.TransaccionRepository;
import com.ecusol.ms_transacciones.client.CuentaClient;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransaccionAsyncConsumer {

    private final TransaccionRepository repository;
    private final CuentaClient cuentaClient;
    private final RestTemplate restTemplate;

    @RabbitListener(queues = "${banco.cola.entrada}")
    public void recibirTransferenciaRabbit(IsoMensajeDTO payload, 
                                           Channel channel, 
                                           @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        String instructionId = payload.getBody().getInstructionId();
        log.info("⚡ RabbitMQ: Recibida transferencia ID: {} desde {}", 
                 instructionId, payload.getHeader().getOriginatingBankId());

        String estadoFinal = "COMPLETED";
        String mensajeFinal = "Transferencia procesada con éxito";

        try {
            if (repository.existsByInstructionId(instructionId)) {
                log.warn("⚠️ Duplicado detectado en cola: {}", instructionId);
                channel.basicAck(tag, false); 
                return; 
            }

            String cuentaDestino = payload.getBody().getCreditor().getAccountId();
            java.math.BigDecimal monto = payload.getBody().getAmount().getValue();
            
            log.info("💰 Acreditando {} a cuenta destino: {}", monto, cuentaDestino);
            cuentaClient.acreditar(cuentaDestino, monto);

            Transaccion tx = new Transaccion();
            tx.setInstructionId(instructionId);
            tx.setReferencia(payload.getBody().getEndToEndId());
            tx.setCuentaOrigen(payload.getBody().getDebtor().getAccountId());
            tx.setCuentaDestino(cuentaDestino);
            tx.setMonto(monto);
            tx.setEstado("COMPLETED");
            tx.setFechaEjecucion(LocalDateTime.now());
            tx.setRolTransaccion("CREDITO");
            
            repository.save(tx);
            log.info("✅ Transacción guardada en BD: {}", instructionId);

            channel.basicAck(tag, false);
            log.info("✅ ACK enviado a RabbitMQ para ID: {}", instructionId);

        } catch (Exception e) {
            log.error("❌ Error procesando mensaje de cola [ID: {}]: {}", instructionId, e.getMessage(), e);
            estadoFinal = "FAILED";
            mensajeFinal = "Error: " + e.getMessage();
            
            try {
                channel.basicNack(tag, false, true);
                log.warn("⚠️ NACK enviado a RabbitMQ - mensaje reencolado para reintentar");
            } catch (IOException ioException) {
                log.error("❌ Error fatal: No se pudo enviar NACK a RabbitMQ: {}", ioException.getMessage());
            }
        }

        try {
            enviarWebhookCallback(
                payload.getHeader().getCallbackUrl(), 
                instructionId, 
                estadoFinal, 
                mensajeFinal
            );
        } catch (Exception e) {
            log.error("❌ Error enviando webhook callback: {}", e.getMessage());
        }
    }

    private void enviarWebhookCallback(String urlCallback, String txId, String estado, String mensaje) {
        if (urlCallback == null || urlCallback.isEmpty()) {
            log.warn("⚠️ No hay URL de Callback para notificar la transacción {}", txId);
            return;
        }

        log.info("📤 Enviando Webhook a: {} [Estado: {}]", urlCallback, estado);

        Map<String, Object> response = Map.of(
            "transaccionId", txId,
            "estado", estado, 
            "mensaje", mensaje,
            "fechaProcesamiento", LocalDateTime.now().toString()
        );

        try {
            restTemplate.postForEntity(urlCallback, response, Void.class);
            log.info("✅ Webhook entregado correctamente al banco origen");
        } catch (Exception e) {
            log.error("❌ Fallo al entregar Webhook al banco origen: {}", e.getMessage());
        }
    }
}