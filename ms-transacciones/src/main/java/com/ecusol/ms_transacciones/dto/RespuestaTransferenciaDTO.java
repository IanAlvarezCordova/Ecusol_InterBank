package com.ecusol.ms_transacciones.dto;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class RespuestaTransferenciaDTO {
    private Integer idTransaccion;
    private String estado;
    private String mensaje;
    private LocalDateTime fechaHora;
}
