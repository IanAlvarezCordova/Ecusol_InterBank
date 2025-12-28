package com.ecusol.ms_transacciones.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.ecusol.ms_transacciones.dto.BancoDTO;
import com.ecusol.ms_transacciones.dto.SwitchTransferRequest;
import com.ecusol.ms_transacciones.dto.SwitchTransferResponse;

import java.util.Collections;
import java.util.List;

/**
 * Cliente para comunicarse con el Switch DIGICONECU.
 * Soporta envío de transferencias y consulta de bancos.
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

    @Value("${banco.codigo:NEXUS}")
    private String bancoCodigo;

    /**
     * Envía una transferencia interbancaria al Switch DIGICONECU.
     * Endpoint: POST /api/v2/transfers
     */
    public SwitchTransferResponse enviarTransferencia(SwitchTransferRequest request) {
        String url = switchUrl + "/api/v2/transfers";
        log.info("📤 Enviando transferencia al Switch: {} -> {}",
                request.getCuentaOrigen(), request.getCuentaDestino());

        try {
            ResponseEntity<SwitchTransferResponse> response = restTemplate.postForEntity(url, request,
                    SwitchTransferResponse.class);

            log.info("✅ Respuesta del Switch: {}", response.getBody());
            return response.getBody();
        } catch (Exception e) {
            log.error("❌ Error enviando al Switch: {}", e.getMessage());
            throw new RuntimeException("Error comunicándose con el Switch: " + e.getMessage());
        }
    }

    /**
     * Obtiene la lista de bancos disponibles en el ecosistema DIGICONECU.
     * Endpoint: GET /api/v1/red/bancos (Network Management - puerto 9082)
     */
    public List<BancoDTO> obtenerBancos() {
        String url = switchNetworkUrl + "/api/v1/red/bancos";
        log.info("📡 Consultando bancos disponibles en el Switch: {}", url);

        try {
            ResponseEntity<List<BancoDTO>> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<List<BancoDTO>>() {
                    });

            List<BancoDTO> bancos = response.getBody();
            log.info("✅ Bancos obtenidos: {}", bancos != null ? bancos.size() : 0);
            return bancos != null ? bancos : Collections.emptyList();
        } catch (Exception e) {
            log.error("❌ Error obteniendo bancos: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Obtiene el código del banco configurado (NEXUS).
     */
    public String getBancoCodigo() {
        return bancoCodigo;
    }
}