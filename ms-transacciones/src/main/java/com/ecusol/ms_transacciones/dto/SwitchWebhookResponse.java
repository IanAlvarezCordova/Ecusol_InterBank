package com.ecusol.ms_transacciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Respuesta al webhook del Switch DIGICONECU.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwitchWebhookResponse {
    private String status;
    private String message;
    private String instructionId;
}
