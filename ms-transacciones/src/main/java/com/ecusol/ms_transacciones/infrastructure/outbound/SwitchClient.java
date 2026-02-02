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
        System.out.println(">>> ENVIANDO AL SWITCH V2 [" + switchUrl + "] APIKEY_LEN="
                + (apiKey != null ? apiKey.length() : "NULL"));

        try {
            String url = switchUrl + "/api/v2/switch/transfers";
            if (switchUrl.endsWith("/"))
                url = switchUrl + "api/v2/switch/transfers";

            String response = restClient.post()
                    .uri(url)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            System.out.println(">>> RESPUESTA SWITCH: " + response);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println(">>> ERROR SWITCH 4xx: " + e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString()); // Propagar mensaje JSON del switch
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.err.println(">>> ERROR SWITCH 5xx: " + e.getResponseBodyAsString());
            throw new RuntimeException("Error técnico en el Switch (5xx)");
        } catch (Exception e) {
            throw new RuntimeException("Error de conexión con el Switch: " + e.getMessage());
        }
    }

    public void enviarDevolucion(com.ecusol.ms_transacciones.dto.ReturnRequestDTO request) {
        System.out.println(">>> ENVIANDO DEVOLUCION V2 [" + switchUrl + "]");
        try {
            String url = switchUrl + "/api/v2/switch/transfers/return";
            if (switchUrl.endsWith("/"))
                url = switchUrl + "api/v2/switch/transfers/return";

            String response = restClient.post()
                    .uri(url)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            System.out.println(">>> RESPUESTA RETURN SWITCH: " + response);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println(">>> ERROR SWITCH RETURN 4xx: " + e.getResponseBodyAsString());
            throw new RuntimeException(e.getResponseBodyAsString());
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            System.err.println(">>> ERROR SWITCH RETURN 5xx: " + e.getResponseBodyAsString());
            throw new RuntimeException("Error técnico en Switch (5xx) al devolver");
        }
    }

    public List<BancoDTO> obtenerBancos() {
        return List.of(
                new BancoDTO("NEXUS_BANK", "Banco Nexus"),
                new BancoDTO("ARCBANK", "ArcBank"),
                new BancoDTO("BANTEC", "BanTec"));
    }

    public com.ecusol.ms_transacciones.dto.AccountLookupResponse realizarLookup(
            com.ecusol.ms_transacciones.dto.AccountLookupRequest request) {
        System.out.println(">>> LOOKUP CUENTA AL SWITCH [" + switchUrl + "]");
        try {
            String url = switchUrl + "/api/v2/switch/accounts/lookup";
            if (switchUrl.endsWith("/"))
                url = switchUrl + "api/v2/switch/accounts/lookup";

            String responseStr = restClient.post()
                    .uri(url)
                    .header("apikey", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(String.class);

            System.out.println(">>> RESPUESTA SWITCH LOOKUP: " + responseStr);

            // Parse manually or useObjectMapper if available, but for now simple check or
            // re-parse
            // Since I injected RestClient builder, I don't have ObjectMapper handy easily
            // without injection.
            // But I can use the same RestClient to convert... no, I already consumed the
            // stream?
            // RestClient doesn't work like that.
            // Better: Just inject ObjectMapper or use a hack.
            // Or change to return String and parse in controller? No, that breaks
            // interface.

            // Let's rely on Spring's ObjectMapper availability in the context if we can, or
            // just instantiate one for debug?
            // No, better to reference `com.fasterxml.jackson.databind.ObjectMapper`.
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(responseStr, com.ecusol.ms_transacciones.dto.AccountLookupResponse.class);

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println(">>> ERROR SWITCH LOOKUP 4xx: " + e.getResponseBodyAsString());
            // Return failed response instead of throwing if possible, or rethrow
            throw new RuntimeException("Error en validación: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            System.err.println(">>> ERROR SWITCH LOOKUP: " + e.getMessage());
            throw new RuntimeException("Error técnico validando cuenta externa: " + e.getMessage());
        }
    }

    public String getBancoCodigo() {
        return bancoCodigo;
    }
}
