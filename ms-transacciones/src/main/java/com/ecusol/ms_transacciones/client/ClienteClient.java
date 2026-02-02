package com.ecusol.ms_transacciones.client;

import com.ecusol.ms_transacciones.dto.PersonaDTO; // We need to check if this DTO exists in ms-transacciones or create it
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClienteClient {

    private final RestTemplate restTemplate;

    @Value("${url.ms.clientes:http://ecusol-ms-clientes:8084}")
    private String clientesUrl;

    public PersonaDTO obtenerCliente(Integer clienteId) {
        String url = clientesUrl + "/api/v1/clientes/" + clienteId;
        try {
            return restTemplate.getForObject(url, PersonaDTO.class);
        } catch (Exception e) {
            log.error("Error obteniendo cliente {}: {}", clienteId, e.getMessage());
            return null;
        }
    }

    public PersonaDTO buscarPorCuenta(String numeroCuenta) {
        // Since we don't have a direct "search by account" in ms-clientes mostly likely
        // (based on previous file exploration),
        // we might need to rely on what we have.
        // Wait, ms-cuentas likely links account -> client.
        // But the requirement says "Buscar en SU base de datos local".
        // In Ecusol architecture:
        // ms-transacciones -> ms-cuentas (to validate account) -> Account has clientId.
        // ms-transacciones -> ms-clientes (to get name using clientId).

        // So this method implementation depends on how we obtain the name.
        // We probably need to:
        // 1. Call ms-cuentas to get account info (which includes clientID).
        // 2. Call ms-clientes to get client info (name).

        // Ideally checking ms-cuentas endpoint response from previous steps:
        // /por-numero/{numeroCuenta} returns CuentaResponse which has clienteId.

        return null; // Implementation will be in the next steps after creating the DTOs
    }

    public String obtenerNombreCliente(Integer clienteId) {
        String url = clientesUrl + "/api/v1/clientes/" + clienteId;
        try {
            return restTemplate.getForObject(url, PersonaDTO.class).getNombres() + " "
                    + restTemplate.getForObject(url, PersonaDTO.class).getApellidos();
        } catch (Exception e) {
            log.error("Error obteniendo cliente {}: {}", clienteId, e.getMessage());
            return "Desconocido";
        }
    }
}
