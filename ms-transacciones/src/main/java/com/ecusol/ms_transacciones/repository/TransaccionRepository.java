package com.ecusol.ms_transacciones.repository;

import com.ecusol.ms_transacciones.model.Transaccion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransaccionRepository extends JpaRepository<Transaccion, Integer> {
    boolean existsByInstructionId(String instructionId);
}
