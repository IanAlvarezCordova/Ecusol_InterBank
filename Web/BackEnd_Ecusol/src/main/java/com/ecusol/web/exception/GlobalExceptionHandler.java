//ubi: src/main/java/com/ecusol/web/exception/GlobalExceptionHandler.java
package com.ecusol.web.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Captura errores de lógica (Credenciales, Validaciones)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> handleRuntimeException(RuntimeException ex) {
        Map<String, String> error = new HashMap<>();
        error.put("error", ex.getMessage()); // Mensaje claro para el front

        // Si es error de credenciales, devolvemos 401, si no 400
        if (ex.getMessage().contains("Credenciales") || ex.getMessage().contains("bloqueado")) {
            return new ResponseEntity<>(error, HttpStatus.UNAUTHORIZED);
        }

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    // Captura errores de llamadas HTTP a otros microservicios (WebClient)
    @ExceptionHandler(org.springframework.web.reactive.function.client.WebClientResponseException.class)
    public ResponseEntity<Map<String, Object>> handleWebClientException(
            org.springframework.web.reactive.function.client.WebClientResponseException ex) {
        Map<String, Object> error = new HashMap<>();
        // Intentar parsear el body si es JSON, sino enviarlo como string
        String errorBody = ex.getResponseBodyAsString();
        error.put("error", "Error del Core Bancario");
        error.put("detalle", errorBody);
        error.put("mensaje", ex.getMessage());

        // Propagar el código de estado original (ej: 422, 404, 500)
        return new ResponseEntity<>(error, ex.getStatusCode());
    }

    // Captura errores inesperados
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneralException(Exception ex) {
        ex.printStackTrace(); // Imprimir en consola para que tú lo veas
        Map<String, String> error = new HashMap<>();
        error.put("error", "Error interno del servidor: " + ex.getMessage());
        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}