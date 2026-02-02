package com.ecusol.ms_transacciones.dto;

import lombok.Data;

@Data
public class PersonaDTO {
    private Integer clienteId;
    private String nombres;
    private String apellidos;
    private String numeroIdentificacion;
    private String email;
    private String estado;
}
