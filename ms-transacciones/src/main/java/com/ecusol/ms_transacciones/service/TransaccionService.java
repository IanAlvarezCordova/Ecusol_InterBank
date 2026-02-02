package com.ecusol.ms_transacciones.service;

import java.util.List;

import com.ecusol.ms_transacciones.dto.*;

public interface TransaccionService {
    RespuestaTransferenciaDTO realizarTransferencia(SolicitudTransferenciaDTO solicitud);

    void procesarPagoEntrante(SwitchTransaccionDTO dto);

    List<MovimientoDTO> obtenerMovimientosPorCuenta(String numeroCuenta);

    void solicitarDevolucion(String originalInstructionId, String motivo, String numeroCuentaPropietaria);

    // Overload: Fallback using transaccionId + monto when instructionId is
    // unavailable
    void solicitarDevolucionPorId(Integer transaccionId, java.math.BigDecimal monto, String motivo,
            String numeroCuentaPropietaria);

    void procesarDevolucionEntrante(ReturnRequestDTO dto);
}