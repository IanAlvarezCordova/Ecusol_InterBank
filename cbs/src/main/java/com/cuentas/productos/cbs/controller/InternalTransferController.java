package com.cuentas.productos.cbs.controller;

import com.cuentas.productos.cbs.service.CuentaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/cuentas")
@RequiredArgsConstructor
public class InternalTransferController {

    private final CuentaService cuentaService;

    public record OperacionCuenta(String cuenta, BigDecimal monto) {
    }

    @PostMapping("/debito")
    public ResponseEntity<Void> debito(@RequestBody OperacionCuenta req) {
        cuentaService.debitar(req.cuenta(), req.monto());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/credito")
    public ResponseEntity<Void> credito(@RequestBody OperacionCuenta req) {
        cuentaService.acreditar(req.cuenta(), req.monto());
        return ResponseEntity.ok().build();
    }
}
