package com.cuentas.productos.cbs.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cuenta")
public class Cuenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cuentaid")
    private Integer id;

    @Column(name = "clienteid", nullable = false)
    private Integer clienteId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipocuentaid", nullable = true)
    @org.hibernate.annotations.NotFound(action = org.hibernate.annotations.NotFoundAction.IGNORE)
    private TipoCuenta tipoCuenta;

    @Column(name = "sucursalidapertura", nullable = false)
    private Integer sucursalIdApertura;

    @Column(name = "numerocuenta", nullable = false, unique = true, length = 20)
    private String numeroCuenta;

    @Column(name = "saldo", nullable = false)
    private BigDecimal saldo;

    @Column(name = "fechaapertura", nullable = false)
    private LocalDate fechaApertura;

    @Column(name = "estado", nullable = false, length = 15)
    private String estado;

    public Cuenta() {
    }

    public Cuenta(Integer id) {
        this.id = id;
    }

    // --- GETTERS Y SETTERS MANUALES ---

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getClienteId() {
        return clienteId;
    }

    public void setClienteId(Integer clienteId) {
        this.clienteId = clienteId;
    }

    public TipoCuenta getTipoCuenta() {
        return tipoCuenta;
    }

    public void setTipoCuenta(TipoCuenta tipoCuenta) {
        this.tipoCuenta = tipoCuenta;
    }

    public Integer getSucursalIdApertura() {
        return sucursalIdApertura;
    }

    public void setSucursalIdApertura(Integer sucursalIdApertura) {
        this.sucursalIdApertura = sucursalIdApertura;
    }

    public String getNumeroCuenta() {
        return numeroCuenta;
    }

    public void setNumeroCuenta(String numeroCuenta) {
        this.numeroCuenta = numeroCuenta;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo;
    }

    public LocalDate getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(LocalDate fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Cuenta cuenta = (Cuenta) o;
        return id != null && id.equals(cuenta.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Cuenta{" +
                "id=" + id +
                ", clienteId=" + clienteId +
                ", tipoCuenta=" + (tipoCuenta != null ? tipoCuenta.getId() : null) +
                ", sucursalIdApertura=" + sucursalIdApertura +
                ", numeroCuenta='" + numeroCuenta + '\'' +
                ", saldo=" + saldo +
                ", fechaApertura=" + fechaApertura +
                ", estado='" + estado + '\'' +
                '}';
    }
}
