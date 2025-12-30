package com.ecusol.ms_transacciones.infrastructure.outbound;

import com.ecusol.ms_transacciones.dto.iso.IsoMensajeDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.ecusol.ms_transacciones.dto.BancoDTO;
import java.util.List;

@Component
public class SwitchClient {

    private final RestClient restClient;

    @Value("${app.switch.apikey}")
    private String apiKey;

    @Value("${app.switch.url}")
    private String switchUrl;

    @Value("${banco.codigo}")
    private String bancoCodigo;

    public SwitchClient(RestClient.Builder builder) {
        this.restClient = builder.build();
    }

    public void enviarTransferencia(IsoMensajeDTO request) {
        System.out.println(
                ">>> ENVIANDO AL SWITCH [" + switchUrl + "] APIKEY_LEN=" + (apiKey != null ? apiKey.length() : "NULL"));

        try {
            String response = restClient.post()
                    .uri(switchUrl)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            System.out.println(">>> RESPUESTA SWITCH: " + response);

        } catch (Exception e) {
            System.err.println(">>> ERROR SWITCH: " + e.getMessage());
            throw new RuntimeException("Error comunicando con el Switch: " + e.getMessage());
        }
    }

    public List<BancoDTO> obtenerBancos() {
        return List.of(
                new BancoDTO("NEXUS_BK", "Banco Nexus"),
                new BancoDTO("ARCBANK_BK", "ArcBank"),
                new BancoDTO("BANTEC_BK", "BanTec"));
    }

    public String getBancoCodigo() {
        return bancoCodigo;
    }
}
