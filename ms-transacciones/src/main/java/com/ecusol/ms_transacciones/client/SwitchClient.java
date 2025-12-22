package com.ecusol.ms_transacciones.client;

import com.ecusol.ms_transacciones.dto.BancoDTO;
import com.ecusol.ms_transacciones.dto.SwitchTransferRequest;
import com.ecusol.ms_transacciones.dto.SwitchTransferResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

/**
 * Cliente HTTP para comunicarse con el Switch DIGICONECU.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SwitchClient {

    private final RestTemplate restTemplate;

    @Value("${api.switch.url}")
    private String switchUrl;

    @Value("${api.switch.network.url:${api.switch.url}}")
    private String switchNetworkUrl;

    @Value("${banco.codigo:ECUSOL}")
    private String bancoCodigo;

    /**
     * ENVIAR transferencia a otro banco via Switch.
     * Endpoint: POST /api/v2/transfers
     */
    public SwitchTransferResponse enviarTransferencia(SwitchTransferRequest request) {
        String url = switchUrl + "/api/v2/transfers";
        log.info("📤 Enviando transferencia al Switch: {} -> {} por ${}",
                request.getCuentaOrigen(), request.getCuentaDestino(), request.getMonto());

        try {
            ResponseEntity<SwitchTransferResponse> response = restTemplate.postForEntity(url, request,
                    SwitchTransferResponse.class);

            SwitchTransferResponse body = response.getBody();
            if (body != null && body.isSuccess()) {
                log.info("✅ Transferencia enviada exitosamente: {}", body.getInstructionId());
            } else {
                log.warn("⚠️ Switch rechazó transferencia: {}", body != null ? body.getError() : "Sin respuesta");
            }
            return body;
        } catch (Exception e) {
            log.error("❌ Error comunicándose con el Switch: {}", e.getMessage());
            SwitchTransferResponse errorResponse = new SwitchTransferResponse();
            errorResponse.setSuccess(false);
            errorResponse.setError("Error de conexión con el Switch: " + e.getMessage());
            return errorResponse;
        }
    }

    /**
     * CONSULTAR lista de bancos disponibles.
     * Endpoint: GET /api/v1/red/bancos
     */
    public List<BancoDTO> obtenerBancos() {
        String url = switchNetworkUrl + "/api/v1/red/bancos";
        log.info("📋 Consultando bancos disponibles en: {}", url);

        try {
            ResponseEntity<List<BancoDTO>> response = restTemplate.exchange(
                    url, HttpMethod.GET, null,
                    new ParameterizedTypeReference<List<BancoDTO>>() {
                    });
            List<BancoDTO> bancos = response.getBody();
            log.info("✅ {} bancos encontrados", bancos != null ? bancos.size() : 0);
            return bancos != null ? bancos : Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ Error obteniendo lista de bancos: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Obtener el código del banco configurado.
     */
    public String getBancoCodigo() {
        return bancoCodigo;
    }
}
