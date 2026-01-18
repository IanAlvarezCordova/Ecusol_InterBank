package com.ecusol.ms_transacciones.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.UUID;

@Data
public class ReturnRequestDTO {
    private UUID id; 
    private UUID idInstruccionOriginal; 
    private String codigoMotivo; 
    private String estado;
}