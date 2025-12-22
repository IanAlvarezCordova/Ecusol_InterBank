package com.cuentas.productos.cbs.repository;

import com.cuentas.productos.cbs.model.Cuenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CuentaRepository extends JpaRepository<Cuenta, Integer> {
    List<Cuenta> findByClienteId(Integer clienteId);

    java.util.Optional<Cuenta> findByNumeroCuenta(String numeroCuenta);

    // Buscar la última cuenta con cierto prefijo (BIN) para generar secuenciales
    java.util.Optional<Cuenta> findTopByNumeroCuentaStartingWithOrderByNumeroCuentaDesc(String prefix);
}
