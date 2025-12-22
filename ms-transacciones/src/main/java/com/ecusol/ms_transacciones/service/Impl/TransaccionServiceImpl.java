package com.ecusol.ms_transacciones.service.Impl;

import com.ecusol.ms_transacciones.client.CuentaClient;
import com.ecusol.ms_transacciones.client.SwitchClient;
import com.ecusol.ms_transacciones.dto.*;
import com.ecusol.ms_transacciones.mapper.TransaccionMapper;
import com.ecusol.ms_transacciones.model.Transaccion;
import com.ecusol.ms_transacciones.repository.TransaccionRepository;
import com.ecusol.ms_transacciones.service.TransaccionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransaccionServiceImpl implements TransaccionService {

    private final TransaccionRepository repository;
    private final CuentaClient cuentaClient;
    private final SwitchClient switchClient;
    private final TransaccionMapper mapper;

    @Override
    @Transactional
    public RespuestaTransferenciaDTO realizarTransferencia(SolicitudTransferenciaDTO solicitud) {

        String instructionId = UUID.randomUUID().toString();

        // Determinar si es transferencia interna o externa
        String bancoDestino = solicitud.getBancoDestino();
        String bancoPropioCode = switchClient.getBancoCodigo(); // "ECUSOL"

        boolean esInterna = bancoDestino == null ||
                bancoDestino.isEmpty() ||
                bancoDestino.equalsIgnoreCase(bancoPropioCode);

        log.info("📤 Iniciando transferencia: {} -> {} (Banco: {}) - {}",
                solicitud.getCuentaOrigen(),
                solicitud.getCuentaDestino(),
                esInterna ? "INTERNA" : bancoDestino,
                instructionId);

        // 1. Guardar Estado Inicial (PENDING)
        Transaccion tx = mapper.solicitudToEntity(solicitud);
        tx.setInstructionId(instructionId);
        tx.setReferencia(instructionId);
        tx.setEstado("PENDING");
        tx = repository.save(tx);

        try {
            // 2. PASO SAGA 1: Debitar la cuenta origen
            cuentaClient.debitar(tx.getCuentaOrigen(), tx.getMonto());
            log.info("✅ Débito exitoso en cuenta {}", tx.getCuentaOrigen());

            if (esInterna) {
                // --- TRANSFERENCIA INTERNA ---
                cuentaClient.acreditar(tx.getCuentaDestino(), tx.getMonto());
                log.info("✅ Crédito interno exitoso en cuenta {}", tx.getCuentaDestino());
                tx.setDescripcion("Transferencia interna exitosa");

            } else {
                // --- TRANSFERENCIA EXTERNA (INTERBANCARIA) ---
                log.info("🌐 Enviando al Switch DIGICONECU -> Banco: {}", bancoDestino);

                SwitchTransferRequest switchRequest = SwitchTransferRequest.builder()
                        .instructionId(instructionId)
                        .bancoOrigen(bancoPropioCode) // "ECUSOL"
                        .bancoDestino(bancoDestino) // "NEXUS", "ARCBANK", etc.
                        .cuentaOrigen(solicitud.getCuentaOrigen())
                        .cuentaDestino(solicitud.getCuentaDestino())
                        .monto(solicitud.getMonto())
                        .moneda("USD")
                        .concepto(solicitud.getDescripcion() != null ? solicitud.getDescripcion()
                                : "Transferencia interbancaria")
                        .build();

                SwitchTransferResponse response = switchClient.enviarTransferencia(switchRequest);

                if (response == null || !response.isSuccess()) {
                    // Switch rechazó - COMPENSAR
                    String errorMsg = response != null ? response.getError() : "Sin respuesta del Switch";
                    log.error("❌ Switch rechazó transferencia: {}", errorMsg);

                    cuentaClient.compensar(tx.getCuentaOrigen(), tx.getMonto());
                    log.info("🔄 Compensación aplicada - dinero devuelto a {}", tx.getCuentaOrigen());

                    tx.setEstado("FAILED");
                    tx.setDescripcion("Rechazado por Switch: " + errorMsg);
                    tx.setMensajeError(errorMsg);
                    return mapper.entityToRespuestaDto(repository.save(tx));
                }

                log.info("✅ Switch aceptó transferencia: {}", response.getInstructionId());
                tx.setDescripcion("Transferencia interbancaria a " + bancoDestino + " exitosa");
            }

            // Éxito
            tx.setEstado("COMPLETED");
            tx.setRolTransaccion("DEBITO");
            tx.setFechaEjecucion(LocalDateTime.now());

        } catch (Exception e) {
            log.error("❌ SAGA FALLO: {}", e.getMessage());

            // COMPENSACIÓN (Deshacer débito si ya se hizo)
            if ("PENDING".equals(tx.getEstado())) {
                try {
                    // Solo compensamos si el error no fue por saldo insuficiente
                    if (!e.getMessage().contains("Fondos insuficientes")) {
                        cuentaClient.compensar(tx.getCuentaOrigen(), tx.getMonto());
                        log.info("🔄 Compensación aplicada");
                    }
                } catch (Exception exComp) {
                    log.error("⚠️ ERROR GRAVE: Fallo compensación manual: {}", exComp.getMessage());
                }
                tx.setEstado("FAILED");
                tx.setDescripcion("Error: " + e.getMessage());
                tx.setMensajeError(e.getMessage());
            }
        }

        return mapper.entityToRespuestaDto(repository.save(tx));
    }

    @Override
    @Transactional
    public void procesarPagoEntrante(SwitchTransaccionDTO dto) {
        // Este método ya no se usa directamente, el webhook maneja la lógica
        // Se mantiene por compatibilidad con la interfaz
        if (repository.existsByInstructionId(dto.getIdInstruccion())) {
            return; // Idempotencia
        }

        Transaccion tx = mapper.switchDtoToEntity(dto);
        tx.setEstado("PENDING");
        tx = repository.save(tx);

        try {
            cuentaClient.acreditar(tx.getCuentaDestino(), tx.getMonto());
            tx.setEstado("COMPLETED");
            tx.setFechaEjecucion(LocalDateTime.now());
        } catch (Exception e) {
            tx.setEstado("FAILED");
            tx.setMensajeError(e.getMessage());
            throw e;
        }
        repository.save(tx);
    }
}
