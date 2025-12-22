package com.ecusol.ms_transacciones.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(name = "SolicitudTransferencia", description = "Cuerpo de la petición para iniciar un pago")
public class SolicitudTransferenciaDTO {

    @NotBlank
    @Schema(description = "Cuenta del cliente que envía los fondos", example = "2200112233")
    private String cuentaOrigen;

    @NotBlank
    @Schema(description = "Cuenta destino en el banco receptor", example = "4050607080")
    private String cuentaDestino;

    @NotNull
    @Positive
    @Schema(description = "Monto total de la transacción", example = "150.75")
    private BigDecimal monto;

    @NotNull
    @Schema(description = "Código del banco destino (1: Otros Bancos, 2: Ecuasol)", example = "1")
    private Integer bancoDestinoId;

    @Schema(description = "Comentario o motivo del pago", example = "Pago de arriendo - Diciembre")
    private String descripcion;

    @Schema(description = "Código del banco destino para transferencias interbancarias (ECUSOL, NEXUS, ARCBANK)", example = "NEXUS")
    private String bancoDestino;
}
