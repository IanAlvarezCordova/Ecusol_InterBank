//ubi: src/main/java/com/ecusol/web/dto/MovimientoWebDTO.java
package com.ecusol.web.dto;

import java.time.ZonedDateTime;

public record MovimientoWebDTO(
        ZonedDateTime fecha,
        String tipo,
        Double monto,
        Double saldoNuevo,
        String descripcion,
        String operacion) {
}
