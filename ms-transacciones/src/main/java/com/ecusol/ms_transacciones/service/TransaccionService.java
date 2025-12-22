package com.ecusol.ms_transacciones.service;

import com.ecusol.ms_transacciones.dto.*;

public interface TransaccionService {
    RespuestaTransferenciaDTO realizarTransferencia(SolicitudTransferenciaDTO solicitud);
    void procesarPagoEntrante(SwitchTransaccionDTO dto);
}
