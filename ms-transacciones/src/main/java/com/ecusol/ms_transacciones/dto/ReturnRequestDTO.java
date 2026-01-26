package com.ecusol.ms_transacciones.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnRequestDTO {
    
    @JsonProperty("idInstruccionOriginal")
    private String idInstruccionOriginal;
    
    @JsonProperty("bancoOrigen")
    private String bancoOrigen;
    
    @JsonProperty("bancoDestino")
    private String bancoDestino;
    
    @JsonProperty("cuentaOrigen")
    private String cuentaOrigen;
    
    @JsonProperty("cuentaDestino")
    private String cuentaDestino;
    
    @JsonProperty("monto")
    private BigDecimal monto;
    
    @JsonProperty("razonDevolucion")
    private String razonDevolucion;
    
    @JsonProperty("referencia")
    private String referencia;
    
    @JsonProperty("timestamp")
    private String timestamp;
}