package com.ecusol.ms_transacciones.service.Impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ecusol.ms_transacciones.client.CuentaClient;
import com.ecusol.ms_transacciones.infrastructure.outbound.SwitchClient;
import com.ecusol.ms_transacciones.dto.*;
import com.ecusol.ms_transacciones.dto.iso.IsoMensajeDTO;
import com.ecusol.ms_transacciones.dto.iso.IsoMensajeDTO.*;
import com.ecusol.ms_transacciones.mapper.TransaccionMapper;
import com.ecusol.ms_transacciones.model.Transaccion;
import com.ecusol.ms_transacciones.repository.TransaccionRepository;
import com.ecusol.ms_transacciones.service.TransaccionService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransaccionServiceImpl implements TransaccionService {

    private final TransaccionRepository repository;
    private final CuentaClient cuentaClient;
    private final SwitchClient switchClient;
    private final TransaccionMapper mapper;
    private final jakarta.persistence.EntityManager entityManager;

    @Override
    @Transactional
    public RespuestaTransferenciaDTO realizarTransferencia(SolicitudTransferenciaDTO solicitud) {

        // 1. Guardar Estado Inicial (PENDING)
        Transaccion tx = mapper.solicitudToEntity(solicitud);
        tx.setInstructionId(UUID.randomUUID().toString());
        if (tx.getReferencia() == null)
            tx.setReferencia(tx.getInstructionId());

        if (solicitud.getBancoDestinoId() != null) {
            tx.setIdBancoDestino(solicitud.getBancoDestinoId());
        } else {
            tx.setIdBancoDestino(1);
        }

        tx = repository.save(tx);

        try {
            // 2. PASO SAGA 1: Debito Local
            cuentaClient.debitar(tx.getCuentaOrigen(), tx.getMonto());

            // 3. DECISIÓN: ¿Interna o Externa?
            boolean esInterna = solicitud.esTransferenciaInterna();

            if (esInterna) {
                // --- TRANSFERENCIA INTERNA ---
                cuentaClient.acreditar(tx.getCuentaDestino(), tx.getMonto());
                log.info("✅ Transferencia INTERNA completada: {} -> {}",
                        tx.getCuentaOrigen(), tx.getCuentaDestino());
            } else {
                // --- TRANSFERENCIA EXTERNA ---
                log.info(">>>> INICIANDO TRANSFERENCIA EXTERNA a {}", solicitud.getBancoDestinoCodigo());

                // 1. Preparar Header
                IsoHeader header = new IsoHeader(
                        tx.getInstructionId(),
                        LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString(),
                        switchClient.getBancoCodigo());

                // 2. Preparar Body
                IsoBody body = new IsoBody();
                body.setInstructionId(tx.getInstructionId());
                body.setEndToEndId(tx.getReferencia());
                body.setAmount(new IsoAmount("USD", tx.getMonto()));

                IsoDebtor debtor = new IsoDebtor();
                debtor.setName("Cliente EcuSol");
                debtor.setAccountId(tx.getCuentaOrigen());
                debtor.setAccountType("CACC");
                body.setDebtor(debtor);

                IsoCreditor creditor = new IsoCreditor();
                creditor.setName("Beneficiario Externo");
                creditor.setAccountId(tx.getCuentaDestino());
                creditor.setAccountType("CACC");
                creditor.setTargetBankId(solicitud.getBancoDestinoCodigo());
                body.setCreditor(creditor);

                body.setRemittanceInformation(tx.getDescripcion());

                IsoMensajeDTO isoMensaje = new IsoMensajeDTO(header, body);

                // 3. Enviar al Switch
                switchClient.enviarTransferencia(isoMensaje);
                log.info("✅ Transferencia EXTERNA enviada al Switch exitosamente");
            }

            // Éxito
            tx.setEstado("COMPLETED");
            tx.setDescripcion("Transferencia Exitosa");
            tx.setFechaEjecucion(LocalDateTime.now());

        } catch (Exception e) {
            log.error(">>> SAGA FALLO GRAVE: {}", e.getMessage(), e);

            // 4. COMPENSACIÓN (Deshacer)
            if ("PENDING".equals(tx.getEstado())) {
                try {
                    // Solo compensamos si el dinero salió (si el error no fue SaldoInsuficiente)
                    if (!e.getMessage().contains("Fondos insuficientes")) {
                        log.info(">>> INICIANDO COMPENSACION para cuenta {}", tx.getCuentaOrigen());
                        cuentaClient.compensar(tx.getCuentaOrigen(), tx.getMonto());
                    }
                } catch (Exception exComp) {
                    log.error(">>> ERROR GRAVE: Fallo compensación manual", exComp);
                }
                tx.setEstado("FAILED");
                tx.setFechaEjecucion(LocalDateTime.now()); // Set timestamp for failed transactions too
                String errorMsg = "Error: " + e.getMessage();
                if (errorMsg.length() > 250)
                    errorMsg = errorMsg.substring(0, 250);
                tx.setDescripcion(errorMsg);
            }
        }
        return mapper.entityToRespuestaDto(repository.save(tx));
    }

    @Override
    @Transactional
    public void procesarPagoEntrante(SwitchTransaccionDTO dto) {
        if (repository.existsByInstructionId(dto.getIdInstruccion())) {
            return; // Idempotencia: Ya la procesamos
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
            throw e; // Lanzamos error para que el Switch sepa que falló
        }
        repository.save(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimientoDTO> obtenerMovimientosPorCuenta(String numeroCuenta) {
        log.info("Consultando movimientos para cuenta: {}", numeroCuenta);
        List<Transaccion> transacciones = repository
                .findAllByCuentaOrigenOrCuentaDestinoOrderByFechaEjecucionDesc(numeroCuenta, numeroCuenta);

        return transacciones.stream().map(tx -> {
            MovimientoDTO dto = mapper.entityToMovimientoDto(tx);
            // Determinar rol en función del tipo de operación y origen/destino
            String tipo = tx.getTipo() != null ? tx.getTipo() : "TRANSFERENCIA";
            if ("DEPOSITO".equalsIgnoreCase(tipo)) {
                // Depósito: crédito para la cuenta origen
                dto.setRolTransaccion("RECEPTOR");
            } else if ("RETIRO".equalsIgnoreCase(tipo)) {
                // Retiro: débito para la cuenta origen
                dto.setRolTransaccion("EMISOR");
            } else {
                // Transferencia: depende si la cuenta es origen (débito) o destino (crédito)
                if (numeroCuenta.equals(tx.getCuentaOrigen())) {
                    dto.setRolTransaccion("EMISOR");
                } else {
                    dto.setRolTransaccion("RECEPTOR");
                }
            }
            return dto;
        }).toList();
    }

    @Override
    @Transactional
    public void solicitarDevolucion(String originalInstructionId, String motivo, String numeroCuentaPropietaria) {
        log.info(">>> Solicitud de devolución recibida para TX: {}", originalInstructionId);

        Transaccion txLocal = repository.findByInstructionId(originalInstructionId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada"));

        // Seguridad: Verificar que quien solicita es el dueño de la cuenta destino
        if (!txLocal.getCuentaDestino().equals(numeroCuentaPropietaria)) {
            throw new RuntimeException("No permiso. Solo el beneficiario puede iniciar el retorno.");
        }

        if (!"COMPLETED".equals(txLocal.getEstado())) {
            throw new RuntimeException("Solo se pueden devolver transacciones exitosas (COMPLETED).");
        }

        // Handle null fechaEjecucion (legacy transactions) - use current time as
        // fallback
        LocalDateTime fechaReferencia = txLocal.getFechaEjecucion();
        if (fechaReferencia == null) {
            log.warn("Transacción {} tiene fechaEjecucion null. Usando fecha actual para validación.",
                    txLocal.getInstructionId());
            // For legacy transactions without execution date, allow refund (assume within
            // 48h)
            fechaReferencia = LocalDateTime.now().minusHours(1); // Assume it happened 1 hour ago
        }

        long horasTranscurridas = java.time.temporal.ChronoUnit.HOURS.between(fechaReferencia,
                LocalDateTime.now());
        if (horasTranscurridas > 48) {
            throw new RuntimeException("La transacción excede el plazo de 48 horas para devolución.");
        }

        // Construir DTO de Devolución
        ReturnRequestDTO req = ReturnRequestDTO.builder()
                .header(ReturnRequestDTO.Header.builder()
                        .messageId("RET-" + UUID.randomUUID())
                        .creationDateTime(LocalDateTime.now().toString())
                        .originatingBankId(switchClient.getBancoCodigo())
                        .build())
                .body(ReturnRequestDTO.Body.builder()
                        .returnInstructionId(UUID.randomUUID().toString())
                        .originalInstructionId(originalInstructionId)
                        .returnReason(motivo != null ? motivo : "AC04")
                        .returnAmount(ReturnRequestDTO.Amount.builder()
                                .currency("USD")
                                .value(txLocal.getMonto())
                                .build())
                        .build())
                .build();

        log.info("Enviando ReturnRequest al Switch V2...");

        // Llamada al Switch
        try {
            switchClient.enviarDevolucion(req);
        } catch (Exception e) {
            log.error("Error al enviar devolución al switch: {}", e.getMessage());
            throw new RuntimeException("Error contactando al Switch para devolución: " + e.getMessage());
        }

        // CONCURRENCY FIX: El webhook puede haber llegado mientras enviábamos
        try {
            entityManager.refresh(txLocal); // Forzar lectura real de DB para ver si Webhook ya actualizó

            if ("REFUNDED".equals(txLocal.getEstado()) || "RETURNED".equals(txLocal.getEstado())) {
                log.info("Race condition: Webhook actualizó estado antes que nosotros. Todo OK.");
                return;
            }

            txLocal.setEstado("RETURN_REQUESTED");
            repository.saveAndFlush(txLocal);

        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
            log.info("Optimistic Lock: Webhook ganó. Todo OK.");
        }
    }

    @Override
    @Transactional
    public void solicitarDevolucionPorId(Integer transaccionId, BigDecimal monto, String motivo,
            String numeroCuentaPropietaria) {
        log.info(">>> Solicitud de devolución por ID recibida para TX ID: {}, Monto: {}", transaccionId, monto);

        Transaccion txLocal = repository.findById(transaccionId)
                .orElseThrow(() -> new RuntimeException("Transacción no encontrada con ID: " + transaccionId));

        // Validar que el monto coincida (seguridad adicional)
        if (txLocal.getMonto().compareTo(monto) != 0) {
            throw new RuntimeException("El monto no coincide con la transacción especificada.");
        }

        // Verificar que tenga instructionId para proceder
        if (txLocal.getInstructionId() == null || txLocal.getInstructionId().isEmpty()) {
            throw new RuntimeException("La transacción no tiene un ID de instrucción válido para devolución.");
        }

        // Delegar al método principal
        solicitarDevolucion(txLocal.getInstructionId(), motivo, numeroCuentaPropietaria);
    }

    // Método para manejar Webhook de Retorno (Entrante)
    @Transactional
    public void procesarDevolucionEntrante(ReturnRequestDTO dto) {
        String originalId = dto.getBody().getOriginalInstructionId();
        log.info(">>> Procesando Devolución Entrante para Original ID: {}", originalId);

        Transaccion tx = repository.findByInstructionId(originalId)
                .orElseThrow(() -> new RuntimeException("Transacción original no encontrada para devolver"));

        if ("REFUNDED".equals(tx.getEstado()) || "RETURNED".equals(tx.getEstado())) {
            log.info("Idempotencia: Transacción ya marcada como devuelta.");
            return;
        }

        // Determinar si soy el Origen (Recibo dinero) o Destino (Me quitan)
        // Como este banco "EcuSol" puede actuar en ambos roles, verificamos:

        // Caso 1: Yo envié el dinero originalmente (Tx Saliente). Ahora me lo
        // devuelven.
        // Mi cliente es cuentaOrigen. Debo Acreditarle.
        // Nota: En Tx Saliente, 'idBancoDestino' es != 1 (o mi ID interno).
        // O mejor chequeo if cuentaOrigen es de este banco? (Si está en la Tx, es
        // cliente local, excepto si es pasarela)
        // Asumimos modelo simple: cuentaOrigen es local.

        boolean soyOrigenOriginal = true; // Por defecto si tengo la Tx completa

        if (soyOrigenOriginal) {
            log.info("Reintegro: Devolviendo {} a cuenta local {}", tx.getMonto(), tx.getCuentaOrigen());
            cuentaClient.acreditar(tx.getCuentaOrigen(), tx.getMonto());
            tx.setEstado("REFUNDED");
            tx.setDescripcion("Devolución Exitosa (Reembolso)");
        }

        repository.save(tx);
        log.info("Devolución procesada exitosamente. Estado final: REFUNDED");
    }
}