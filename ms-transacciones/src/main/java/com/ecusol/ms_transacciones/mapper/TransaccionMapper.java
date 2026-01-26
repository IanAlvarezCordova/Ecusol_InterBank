package com.ecusol.ms_transacciones.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.ecusol.ms_transacciones.dto.*;
import com.ecusol.ms_transacciones.model.Transaccion;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TransaccionMapper {

    // Cliente -> Entidad
    @Mapping(target = "transaccionId", ignore = true)
    @Mapping(target = "estado", constant = "PENDING")
    @Mapping(target = "rolTransaccion", constant = "DEBITO")
    @Mapping(target = "tipo", constant = "TRANSFERENCIA")
    @Mapping(target = "idBancoOrigen", constant = "2") // Somos Banco 2 (Nexus)
    @Mapping(target = "idBancoDestino", source = "bancoDestinoId")
    @Mapping(target = "instructionId", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "fechaEjecucion", ignore = true)
    @Mapping(target = "referencia", ignore = true)
    @Mapping(target = "codigoBicDestino", source = "bancoDestinoCodigo")
    @Mapping(target = "mensajeError", ignore = true)
    Transaccion solicitudToEntity(SolicitudTransferenciaDTO dto);

    // Entidad -> Switch JSON
    @Mapping(target = "idInstruccion", source = "instructionId")
    @Mapping(target = "endToEnd", source = "referencia")
    @Mapping(target = "estadoActual", source = "estado")
    @Mapping(target = "mensaje", source = "descripcion")
    SwitchTransaccionDTO entityToSwitchDto(Transaccion tx);

    MovimientoDTO entityToMovimientoDto(Transaccion tx);

    List<MovimientoDTO> entityListToMovimientoDtoList(List<Transaccion> txs);

    // Switch JSON -> Entidad (Entrada)
    @Mapping(target = "transaccionId", ignore = true)
    @Mapping(target = "instructionId", source = "idInstruccion")
    @Mapping(target = "referencia", source = "endToEnd")
    @Mapping(target = "estado", source = "estadoActual")
    @Mapping(target = "descripcion", source = "mensaje")
    @Mapping(target = "tipo", constant = "TRANSFERENCIA")
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "fechaEjecucion", ignore = true)
    // El switch nos manda ID banco en la trama? No siempre explicitamente, asumimos
    // lógica externa
    @Mapping(target = "idBancoOrigen", ignore = true)
    @Mapping(target = "idBancoDestino", ignore = true)
    @Mapping(target = "codigoBicDestino", ignore = true)
    @Mapping(target = "mensajeError", ignore = true)
    @Mapping(target = "rolTransaccion", constant = "CREDITO")
    Transaccion switchDtoToEntity(SwitchTransaccionDTO dto);

    // Entidad -> Respuesta Cliente
    @Mapping(target = "idTransaccion", source = "transaccionId")
    @Mapping(target = "fechaHora", source = "fechaEjecucion")
    @Mapping(target = "mensaje", source = "descripcion")
    @Mapping(target = "estado", source = "estado")
    RespuestaTransferenciaDTO entityToRespuestaDto(Transaccion transaccion);
}