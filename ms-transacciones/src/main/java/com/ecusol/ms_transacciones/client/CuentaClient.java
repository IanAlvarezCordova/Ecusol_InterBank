package com.ecusol.ms_transacciones.client;

import com.ecusol.ms_transacciones.dto.ReturnRequestDTO;
import com.ecusol.ms_transacciones.exception.SaldoInsuficienteException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
@Slf4j
public class CuentaClient {

    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    @Value("${api.cuentas.url}")
    private String cuentasBaseUrl;

    @Value("${api.switch.url}")
    private String switchUrl;

    @Value("${api.switch.network.url:${api.switch.url}}")
    private String switchNetworkUrl;

    @Value("${api.switch.apikey}")
    private String apiKey;

    @Value("${api.switch.returns.url:${api.switch.url}}")
    private String switchReturnsUrl;

    private String cuentasUrl() {
        return cuentasBaseUrl + "/api/v1/cuentas";
    }

    public void debitar(String cuenta, BigDecimal monto) {
        String url = cuentasUrl() + "/debito";
        log.info("Iniciando DÉBITO en: {} para cuenta: {}", url, cuenta);

        try {
            record Req(String cuenta, BigDecimal monto) {}
            restTemplate.postForEntity(url, new Req(cuenta, monto), Void.class);
            log.info("DÉBITO exitoso");
        } catch (HttpClientErrorException.Conflict | HttpClientErrorException.BadRequest e) {
            log.error("FALLO DÉBITO (Saldo): {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new SaldoInsuficienteException("Fondos insuficientes en la cuenta origen");
        } catch (Exception e) {
            log.error("Error contactando MS-CUENTAS (DÉBITO): {}", e.getMessage(), e);
            throw new RuntimeException("Servicio de cuentas no disponible: " + e.getMessage());
        }
    }

    public void acreditar(String cuenta, BigDecimal monto) {
        String url = cuentasUrl() + "/credito";
        log.info("Iniciando CRÉDITO en: {} para cuenta: {}", url, cuenta);

        try {
            record Req(String cuenta, BigDecimal monto) {}
            restTemplate.postForEntity(url, new Req(cuenta, monto), Void.class);
            log.info("CRÉDITO exitoso");
        } catch (Exception e) {
            log.error("Error contactando MS-CUENTAS (CRÉDITO): {}", e.getMessage(), e);
            throw new RuntimeException("Error crítico al acreditar fondos: " + e.getMessage());
        }
    }

    public void compensar(String cuenta, BigDecimal monto) {
        log.warn("SAGA COMPENSANDO: Revirtiendo débito para cuenta {}", cuenta);
        acreditar(cuenta, monto);
    }

    public void enviarDevolucion(ReturnRequestDTO request) {
        String url = switchReturnsUrl + "/api/v1/transactions/returns";

        log.info("Enviando devolución al Switch: Original ID {}", request.getIdInstruccionOriginal());

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", apiKey);

            HttpEntity<ReturnRequestDTO> entity = new HttpEntity<>(request, headers);
            restTemplate.postForEntity(url, entity, Void.class);

            log.info("Devolución enviada correctamente");
        } catch (Exception e) {
            log.error("Error enviando devolución: {}", e.getMessage(), e);
            throw new RuntimeException("El Switch rechazó la devolución: " + e.getMessage());
        }
    }

    public void enviarDevolucionAsincrona(ReturnRequestDTO request) {
        log.info("Enviando devolución ASÍNCRONA a RabbitMQ: Original ID {}", request.getIdInstruccionOriginal());
        rabbitTemplate.convertAndSend("switch.exchange", "switch.returns.in", request);
    }

    public String getSwitchNetworkUrl() {
        return switchNetworkUrl;
    }
}