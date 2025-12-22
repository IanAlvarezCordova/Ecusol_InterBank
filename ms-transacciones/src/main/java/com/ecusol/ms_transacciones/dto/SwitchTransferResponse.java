package com.ecusol.ms_transacciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta del Switch DIGICONECU cuando enviamos una transferencia.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwitchTransferResponse {

    @JsonProperty("success")
    private boolean success;

    @JsonProperty("message")
    private String message;

    @JsonProperty("instructionId")
    private String instructionId;

    @JsonProperty("error")
    private String error;
}
