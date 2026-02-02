//ubicacion: src/types/index.ts
// Auth
export interface AuthResponse {
  token: string;
  nombreSucursal: string;
  sucursalId: number;
}

// Cliente y Cuentas
export interface CuentaResumenDTO {
  numeroCuenta: string;
  tipo: string; // "Ahorros" / "Corriente"
  saldo: number;
  estado: string;
}

export interface ResumenClienteDTO {
  clienteId: number;
  nombres: string; // Nombres + Apellidos
  cedula: string;
  estado: string;
  cuentas: CuentaResumenDTO[];
}

// Info para validar destino
export interface InfoCuentaDTO {
  numeroCuenta: string;
  nombreCompleto: string; // Ojo: En Java le pusimos nombreCompleto
  tipoCuenta: string;
}

// Operaciones
export interface VentanillaOpDTO {
  numeroCuentaOrigen: string;
  numeroCuentaDestino?: string; // Solo para transferencias
  monto: number;
  descripcion: string;
}

export interface MovimientoDTO {
  transaccionId?: number;
  instructionId?: string;
  referencia: string;
  tipo: string; // "DEPOSITO", "RETIRO", etc.
  rolTransaccion: string; // "DEBITO", "CREDITO"
  monto: number;
  descripcion: string;
  fechaEjecucion: string; // ISO String or Array
  estado: string;
  cuentaDestino?: string;
  // helpers
  nombreDestino?: string;
  numeroCuenta?: string;
}