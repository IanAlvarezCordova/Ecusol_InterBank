package com.ecusol.ms_transacciones.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ecusol.ms_transacciones.model.Transaccion;

import java.util.List;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Integer> {
    boolean existsByInstructionId(String instructionId);

    List<Transaccion> findAllByCuentaOrigenOrCuentaDestinoOrderByFechaEjecucionDesc(String cuentaOrigen,
            String cuentaDestino);
}