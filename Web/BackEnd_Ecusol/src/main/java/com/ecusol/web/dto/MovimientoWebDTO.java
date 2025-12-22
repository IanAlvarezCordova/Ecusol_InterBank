//ubi: src/main/java/com/ecusol/web/dto/MovimientoWebDTO.java
package com.ecusol.web.dto;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record MovimientoWebDTO(
    LocalDateTime fecha, String tipo, BigDecimal monto, BigDecimal saldoNuevo, String descripcion
) {}