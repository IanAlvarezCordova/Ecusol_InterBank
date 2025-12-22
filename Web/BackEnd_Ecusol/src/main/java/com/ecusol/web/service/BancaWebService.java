//ubi: src/main/java/com/ecusol/web/service/BancaWebService.java
package com.ecusol.web.service;

import com.ecusol.web.client.CoreBancarioClient;
import com.ecusol.web.dto.*;
import com.ecusol.web.model.Beneficiario;
import com.ecusol.web.model.UsuarioWeb;
import com.ecusol.web.repository.BeneficiarioRepository;
import com.ecusol.web.repository.UsuarioWebRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BancaWebService {

    @Autowired
    private CoreBancarioClient coreClient;
    @Autowired
    private BeneficiarioRepository beneficiarioRepo;
    @Autowired
    private UsuarioWebRepository usuarioWebRepo;

    public List<CuentaWebDTO> misCuentas(Integer clienteIdCore) {
        return coreClient.obtenerCuentasPorCliente(clienteIdCore).stream()
                .map(c -> new CuentaWebDTO(
                        c.getCuentaId().longValue(),
                        c.getNumeroCuenta(),
                        c.getSaldo(),
                        c.getEstado(),
                        c.getTipoCuentaId().longValue()))
                .collect(Collectors.toList());
    }

    public List<MovimientoWebDTO> misMovimientos(String cuenta) {
        var movsCore = coreClient.obtenerMovimientos(cuenta);
        return movsCore.stream()
                .map(m -> new MovimientoWebDTO(
                        m.getFechaEjecucion(),
                        m.getTipo(),
                        m.getMonto(),
                        BigDecimal.ZERO, // Saldo histórico no viene, ponemos 0 o calculamos
                        m.getDescripcion()))
                .collect(Collectors.toList());
    }

    public String transferir(TransferenciaRequest req) {
        // Map bancoDestino string to ID
        // 2 = EcuSol/Nexus (Interno), 1 = Otros Bancos
        Integer bancoDestinoId = "ECUSOL".equalsIgnoreCase(req.bancoDestino())
                ? 2
                : 1;
        return coreClient.realizarTransferencia(req, bancoDestinoId);
    }

    /**
     * Valida un destinatario. Si es banco externo, llama al Switch.
     * Si es ECUSOL (interno), busca en la base de datos local.
     */
    public TitularCuentaDTO validarDestinatarioCompleto(String numeroCuenta, String banco) {
        // Si es interno (ECUSOL o null), buscar localmente
        if (banco == null || banco.isEmpty() || "ECUSOL".equalsIgnoreCase(banco) || "ECUASOL".equalsIgnoreCase(banco)) {
            try {
                return coreClient.validarTitular(numeroCuenta);
            } catch (Exception e) {
                throw new RuntimeException("Cuenta no existe en ECUSOL");
            }
        }

        // Si es externo, devolver datos genéricos (el Switch validará en la
        // transferencia)
        // En un escenario real, llamaríamos al Switch para validar, pero el Switch
        // DIGICONECU
        // no tiene un endpoint de validación por banco, solo por BIN.
        // Por ahora, retornamos datos indicando que es una cuenta externa.
        return new TitularCuentaDTO(
                numeroCuenta,
                "Titular " + banco.toUpperCase(),
                "***",
                "Cuenta " + banco.toUpperCase());
    }

    public void solicitarCuenta(Integer clienteIdCore, Integer tipoCuentaId) {
        CrearCuentaRequest req = CrearCuentaRequest.builder()
                .clienteId(clienteIdCore)
                .tipoCuentaId(tipoCuentaId)
                .sucursalIdApertura(1) // FIX: Sucursal Matriz por defecto
                .saldoInicial(BigDecimal.ZERO)
                .build();
        coreClient.crearCuenta(req);
    }

    public List<SucursalDTO> obtenerSucursales() {
        return coreClient.obtenerSucursales();
    }

    public void guardarBeneficiario(Integer usuarioWebId, BeneficiarioDTO dto) {
        UsuarioWeb usuario = usuarioWebRepo.findById(usuarioWebId)
                .orElseThrow(() -> new RuntimeException("Usuario Web no encontrado"));

        Beneficiario b = new Beneficiario();
        b.setUsuarioWeb(usuario);
        b.setNumeroCuentaDestino(dto.numeroCuenta());
        b.setNombreTitular(dto.nombreTitular());
        b.setAlias(dto.alias());

        // Guardar Tipo de Cuenta correctamente
        b.setTipoCuenta(dto.tipoCuenta() != null ? dto.tipoCuenta() : "Desconocido");

        b.setFechaRegistro(java.time.LocalDateTime.now());
        beneficiarioRepo.save(b);
    }

    public List<BeneficiarioDTO> misBeneficiarios(Integer usuarioWebId) {
        return beneficiarioRepo.findByUsuarioWeb_UsuarioWebId(usuarioWebId).stream()
                .map(b -> new BeneficiarioDTO(
                        b.getBeneficiarioId(),
                        b.getNumeroCuentaDestino(),
                        b.getNombreTitular(),
                        b.getAlias(),
                        b.getTipoCuenta()))
                .collect(Collectors.toList());
    }
}