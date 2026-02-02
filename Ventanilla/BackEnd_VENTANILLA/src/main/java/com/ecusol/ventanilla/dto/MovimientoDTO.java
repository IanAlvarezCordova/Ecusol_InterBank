package com.ecusol.ventanilla.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class MovimientoDTO {
    private Integer transaccionId;
    private String referencia;
    private String rolTransaccion;
    private BigDecimal monto;
    private String descripcion;
    private LocalDateTime fechaEjecucion;
    private String tipo; // DEPOSITO, RETIRO, TRANSFERENCIA
    private String instructionId;
    private String cuentaDestino;
    private String estado; // Mapping generic field if available
}
