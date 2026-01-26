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

    @Value("${banco.webhook.url}")
    private String webhookUrl;

    @Override
    @Transactional
    public RespuestaTransferenciaDTO realizarTransferencia(SolicitudTransferenciaDTO solicitud) {

        // 1. Guardar Estado Inicial (PENDING)
        Transaccion tx = mapper.solicitudToEntity(solicitud);
        tx.setInstructionId(UUID.randomUUID().toString());
        if (tx.getReferencia() == null)
            tx.setReferencia(tx.getInstructionId());

        // FIX: Usar "idBancoDestino" que es el nombre correcto en la Entidad
        // Transaccion
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
                // Acreditar directamente en local
                cuentaClient.acreditar(tx.getCuentaDestino(), tx.getMonto());
                log.info("✅ Transferencia INTERNA completada: {} -> {}",
                        tx.getCuentaOrigen(), tx.getCuentaDestino());
            } else {
                // --- TRANSFERENCIA EXTERNA ---
                log.info(">>>> INICIANDO TRANSFERENCIA EXTERNA a {}", solicitud.getBancoDestinoCodigo());

                // 1. Preparar Header
                IsoHeader header = new IsoHeader(
                        tx.getInstructionId(),
                        LocalDateTime.now().toString(),
                        switchClient.getBancoCodigo(),
                        webhookUrl);

                // 2. Preparar Body
                IsoBody body = new IsoBody();
                body.setInstructionId(tx.getInstructionId());
                body.setEndToEndId(tx.getReferencia());
                body.setAmount(new IsoAmount("USD", tx.getMonto()));

                IsoDebtor debtor = new IsoDebtor();
                debtor.setName("Cliente EcuSol"); // Idealmente sacar nombre real de ms-clientes
                debtor.setAccountId(tx.getCuentaOrigen());
                debtor.setAccountType("SAVINGS"); // Standard ISO: SAVINGS or CHECKING
                body.setDebtor(debtor);

                IsoCreditor creditor = new IsoCreditor();
                creditor.setName("Beneficiario Externo");
                creditor.setAccountId(tx.getCuentaDestino());
                creditor.setAccountType("SAVINGS"); // Por defecto mandamos SAVINGS
                creditor.setTargetBankId(solicitud.getBancoDestinoCodigo());
                body.setCreditor(creditor);

                body.setRemittanceInformation(tx.getDescripcion());

                IsoMensajeDTO isoMensaje = new IsoMensajeDTO(header, body);

                // 3. Enviar al Switch
                try {
                    switchClient.enviarTransferencia(isoMensaje);
                    log.info("✅ Transferencia EXTERNA enviada al Switch exitosamente");
                } catch (org.springframework.web.client.HttpStatusCodeException e) {
                    log.error("❌ Error enviando al Switch: Status {} - Body {}", e.getStatusCode(),
                            e.getResponseBodyAsString());
                    throw new RuntimeException("Switch rechazó la transacción: " + e.getStatusCode());
                }
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
                tx.setDescripcion("Error: " + e.getMessage());
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
        // (quien recibió la plata)
        if (!txLocal.getCuentaDestino().equals(numeroCuentaPropietaria)) {
            // Check si es el origen (caso: solicitud de cancelación por error propio?)
            // El documento dice: "Flujo 3: Procesamiento de Devoluciones (Returns) ... Si
            // una transferencia exitosa (COMPLETED) debe ser revertida ... el Banco Destino
            // debe iniciar un pacs.004."
            // Por tanto, el dueño de la cuenta destino (Beneficiario) es quien "Devuelve"
            // (Return).
            throw new RuntimeException(
                    "No tiene permiso para devolver esta transacción. Solo el beneficiario puede iniciar el retorno.");
        }

        // Regla: 48 horas
        long horasTranscurridas = java.time.temporal.ChronoUnit.HOURS.between(txLocal.getFechaEjecucion(),
                LocalDateTime.now());
        if (horasTranscurridas > 48) {
            log.warn(">>> Intento de devolución fuera del plazo. TX ID: {} ({}h transcurridas)", originalInstructionId,
                    horasTranscurridas);
            throw new RuntimeException("La transacción excede el plazo de 48 horas para devolución.");
        }

        if ("RETURNING".equals(txLocal.getEstado()) || "REVERSED".equals(txLocal.getEstado())) {
            throw new RuntimeException("La transacción ya está en proceso de devolución o fue reversada.");
        }

        ReturnRequestDTO req = ReturnRequestDTO.builder()
                .header(ReturnRequestDTO.Header.builder()
                        .messageId("RET-" + UUID.randomUUID())
                        .creationDateTime(LocalDateTime.now().toString())
                        .originatingBankId("ECUSOLBK")
                        .build())
                .body(ReturnRequestDTO.Body.builder()
                        .returnInstructionId(UUID.randomUUID().toString())
                        .originalInstructionId(originalInstructionId)
                        .returnReason(motivo != null ? motivo : "AC04") // Default AC04 or user provided
                        .returnAmount(ReturnRequestDTO.Amount.builder()
                                .currency("USD")
                                .value(txLocal.getMonto())
                                .build())
                        .build())
                .build();

        log.info("Enviando ReturnRequest al Switch...");
        cuentaClient.enviarDevolucion(req);

        txLocal.setEstado("RETURNING");
        repository.save(txLocal);
    }
}