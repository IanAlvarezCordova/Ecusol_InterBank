package com.ecusol.ms_transacciones.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO para representar un banco del Switch DIGICONECU.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BancoDTO {
    private String codigo; // "NEXUS", "ARCBANK", "ECUSOL"
    private String nombre; // "Banco Nexus", "ArcBank S.A."
    private String estado; // "Activo", "Inactivo"
}
