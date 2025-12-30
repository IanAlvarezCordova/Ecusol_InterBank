package com.ecusol.ms_transacciones.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transaccion")
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "transaccion_id")
    private Integer transaccionId;

    // Negocio
    @Column(name = "cuenta_origen")
    private String cuentaOrigen;

    @Column(name = "cuenta_destino")
    private String cuentaDestino;

    @Column(name = "monto")
    private BigDecimal monto;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "estado")
    private String estado; // PENDING, COMPLETED, FAILED

    @Column(name = "rol_transaccion")
    private String rolTransaccion; // DEBITO, CREDITO

    // Tipo funcional: DEPOSITO, RETIRO, TRANSFERENCIA
    @Column(name = "tipo")
    private String tipo;

    @Column(name = "fecha_ejecucion")
    private LocalDateTime fechaEjecucion;

    // Switch (Técnico)
    @Column(name = "instruction_id", unique = true)
    private String instructionId;

    @Column(name = "referencia")
    private String referencia;

    @Column(name = "id_banco_origen")
    private Integer idBancoOrigen;

    @Column(name = "id_banco_destino")
    private Integer idBancoDestino;

    @Column(name = "codigo_bic_destino")
    private String codigoBicDestino; // NEW: Para saber a qué banco fue

    @Column(name = "mensaje_error")
    private String mensajeError;

    // Concurrencia
    @Version
    private Long version;

    // --- CONSTRUCTORES ---
    public Transaccion() {
    }

    public Transaccion(Integer transaccionId, String cuentaOrigen, String cuentaDestino, BigDecimal monto,
            String estado, LocalDateTime fechaEjecucion) {
        this.transaccionId = transaccionId;
        this.cuentaOrigen = cuentaOrigen;
        this.cuentaDestino = cuentaDestino;
        this.monto = monto;
        this.estado = estado;
        this.fechaEjecucion = fechaEjecucion;
    }

    // --- GETTERS Y SETTERS MANUALES (SIN LOMBOK) ---

    public Integer getTransaccionId() {
        return transaccionId;
    }

    public void setTransaccionId(Integer transaccionId) {
        this.transaccionId = transaccionId;
    }

    public String getCuentaOrigen() {
        return cuentaOrigen;
    }

    public void setCuentaOrigen(String cuentaOrigen) {
        this.cuentaOrigen = cuentaOrigen;
    }

    public String getCuentaDestino() {
        return cuentaDestino;
    }

    public void setCuentaDestino(String cuentaDestino) {
        this.cuentaDestino = cuentaDestino;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public void setMonto(BigDecimal monto) {
        this.monto = monto;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getRolTransaccion() {
        return rolTransaccion;
    }

    public void setRolTransaccion(String rolTransaccion) {
        this.rolTransaccion = rolTransaccion;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public LocalDateTime getFechaEjecucion() {
        return fechaEjecucion;
    }

    public void setFechaEjecucion(LocalDateTime fechaEjecucion) {
        this.fechaEjecucion = fechaEjecucion;
    }

    public String getInstructionId() {
        return instructionId;
    }

    public void setInstructionId(String instructionId) {
        this.instructionId = instructionId;
    }

    public String getReferencia() {
        return referencia;
    }

    public void setReferencia(String referencia) {
        this.referencia = referencia;
    }

    public Integer getIdBancoOrigen() {
        return idBancoOrigen;
    }

    public void setIdBancoOrigen(Integer idBancoOrigen) {
        this.idBancoOrigen = idBancoOrigen;
    }

    public Integer getIdBancoDestino() {
        return idBancoDestino;
    }

    public void setIdBancoDestino(Integer idBancoDestino) {
        this.idBancoDestino = idBancoDestino;
    }

    public String getCodigoBicDestino() {
        return codigoBicDestino;
    }

    public void setCodigoBicDestino(String codigoBicDestino) {
        this.codigoBicDestino = codigoBicDestino;
    }

    public String getMensajeError() {
        return mensajeError;
    }

    public void setMensajeError(String mensajeError) {
        this.mensajeError = mensajeError;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
    // --- EQUALS, HASHCODE, TOSTRING MANUALES ---

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Transaccion that = (Transaccion) o;

        return transaccionId != null ? transaccionId.equals(that.transaccionId) : that.transaccionId == null;
    }

    @Override
    public int hashCode() {
        return transaccionId != null ? transaccionId.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Transaccion{" +
                "transaccionId=" + transaccionId +
                ", cuentaOrigen='" + cuentaOrigen + '\'' +
                ", cuentaDestino='" + cuentaDestino + '\'' +
                ", monto=" + monto +
                ", descripcion='" + descripcion + '\'' +
                ", estado='" + estado + '\'' +
                ", rolTransaccion='" + rolTransaccion + '\'' +
                ", tipo='" + tipo + '\'' +
                ", fechaEjecucion=" + fechaEjecucion +
                ", instructionId='" + instructionId + '\'' +
                ", referencia='" + referencia + '\'' +
                ", idBancoOrigen=" + idBancoOrigen +
                ", idBancoDestino=" + idBancoDestino +
                ", codigoBicDestino='" + codigoBicDestino + '\'' +
                ", mensajeError='" + mensajeError + '\'' +
                '}';
    }
}
