package com.ecusol.ms_transacciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

/**
 * Request para ENVIAR transferencias al Switch DIGICONECU.
 * Endpoint: POST /api/v2/transfers
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwitchTransferRequest {

    @JsonProperty("instructionId")
    private String instructionId; // UUID único

    @JsonProperty("bancoOrigen")
    private String bancoOrigen; // "ECUSOL"

    @JsonProperty("bancoDestino")
    private String bancoDestino; // "NEXUS", "ARCBANK", etc.

    @JsonProperty("cuentaOrigen")
    private String cuentaOrigen; // Cuenta de ECUSOL (700...)

    @JsonProperty("cuentaDestino")
    private String cuentaDestino; // Cuenta del banco destino

    @JsonProperty("monto")
    private BigDecimal monto;

    @JsonProperty("moneda")
    private String moneda; // "USD"

    @JsonProperty("concepto")
    private String concepto; // Descripción
}
