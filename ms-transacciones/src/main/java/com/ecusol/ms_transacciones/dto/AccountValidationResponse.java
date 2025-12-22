package com.ecusol.ms_transacciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountValidationResponse {
    private boolean success;
    private String message;
    private String nombreTitular;
    private boolean cuentaValida;
}
