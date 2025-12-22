package com.ecusol.ms_transacciones.client;

import com.ecusol.ms_transacciones.exception.SaldoInsuficienteException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import java.math.BigDecimal;

@Component
@Slf4j
public class CuentaClient {

    private final RestTemplate restTemplate;
    private final String urlCompleta;

    // Eliminamos @RequiredArgsConstructor para usar este constructor manual
    // @Value inyectará la URL desde application.properties o variables de entorno
    // de AWS
    public CuentaClient(RestTemplate restTemplate, @Value("${api.cuentas.url}") String urlBase) {
        this.restTemplate = restTemplate;
        this.urlCompleta = urlBase + "/api/cuentas";
    }

    public void debitar(String cuenta, BigDecimal monto) {
        try {
            log.info("Llamando a débito en: {} para cuenta: {}", urlCompleta, cuenta);
            record Req(String cuenta, BigDecimal monto) {
            }
            restTemplate.postForEntity(urlCompleta + "/debito", new Req(cuenta, monto), Void.class);
        } catch (HttpClientErrorException.Conflict | HttpClientErrorException.BadRequest e) {
            log.error("Fallo debito por saldo: {}", e.getMessage());
            throw new SaldoInsuficienteException("Fondos insuficientes en la cuenta origen");
        } catch (Exception e) {
            log.error("Error de conexión con MS-CUENTAS: {}", e.getMessage());
            throw new RuntimeException("Servicio de cuentas no disponible");
        }
    }

    public void acreditar(String cuenta, BigDecimal monto) {
        try {
            log.info("Llamando a crédito en: {} para cuenta: {}", urlCompleta, cuenta);
            record Req(String cuenta, BigDecimal monto) {
            }
            restTemplate.postForEntity(urlCompleta + "/credito", new Req(cuenta, monto), Void.class);
        } catch (Exception e) {
            log.error("Fallo credito en SAGA: {}", e.getMessage());
            throw new RuntimeException("Error crítico al acreditar fondos");
        }
    }

    public void compensar(String cuenta, BigDecimal monto) {
        log.warn("SAGA COMPENSANDO: Revirtiendo débito para cuenta {}", cuenta);
        acreditar(cuenta, monto);
    }

    public Object buscarPorNumero(String numeroCuenta) {
        try {
            String url = urlCompleta + "/por-numero/" + numeroCuenta;
            log.info("Consultando cuenta en MS-CUENTAS: {}", url);
            return restTemplate.getForObject(url, Object.class);
        } catch (Exception e) {
            log.warn("Cuenta no encontrada o error en MS-CUENTAS: {}", e.getMessage());
            return null;
        }
    }
}
