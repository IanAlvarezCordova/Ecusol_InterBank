package com.ecusol.ms_transacciones.controller;

import com.ecusol.ms_transacciones.dto.iso.IsoMensajeDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/core/transacciones")
public class WebhookController {

    @PostMapping("/recepcion")
    public ResponseEntity<?> recibirTransferencia(@RequestBody IsoMensajeDTO mensaje) {
        System.out.println(">>> WEBHOOK: Recibida transferencia ISO 20022");
        System.out.println("    InstructionID: " + mensaje.getBody().getInstructionId());
        System.out.println("    Creditor Account: " + mensaje.getBody().getCreditor().getAccountId());
        System.out.println("    Monto: " + mensaje.getBody().getAmount().getValue());

        // AQUÍ idealmente llamaríamos a un servicio interno para acreditar la cuenta.
        // Por ahora, simulamos éxito para cumplir con el contrato del Switch.

        // TODO: Implementar lógica real de acreditación (Llamar a MS-Cuentas o
        // depositar localmente si fuera monolitico)
        // Como MS-Cuentas es otro microservicio, aquí deberíamos llamarlo via
        // Feign/HTTP.
        // Simulamos éxito:

        return ResponseEntity.ok().body("{\"status\":\"RECEIVED\"}");
    }
}
