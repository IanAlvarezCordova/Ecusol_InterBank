package com.ecusol.ms_transacciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Payload que recibimos cuando el Switch DIGICONECU nos notifica una
 * transferencia entrante.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwitchWebhookPayload {

    @JsonProperty("bancoOrigen")
    private String bancoOrigen; // "NEXUS", "ARCBANK", etc.

    @JsonProperty("cuentaOrigen")
    private String cuentaOrigen;

    @JsonProperty("cuentaDestino")
    private String cuentaDestino; // Cuenta de ECUSOL (debe empezar con 280900)

    @JsonProperty("monto")
    private BigDecimal monto;

    @JsonProperty("instructionId")
    private String instructionId; // UUID - usar para idempotencia

    @JsonProperty("concepto")
    private String concepto;
}
