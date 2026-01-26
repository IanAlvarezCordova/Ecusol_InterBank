export interface AuthResponse {
  token: string;
  usuario: string;
}

export interface CuentaDTO {
  cuentaId: number;
  numeroCuenta: string;
  saldo: number;
  estado: string;
  tipoCuentaId: number;
}

export interface MovimientoDTO {
  fecha: string;
  tipo: 'C' | 'D';
  monto: number;
  saldoNuevo: number;
  descripcion: string;
  operacion?: string; // DEPOSITO | RETIRO | TRANSFERENCIA
  instructionId?: string;
  referencia?: string;
  rolTransaccion?: string; // EMISOR | RECEPTOR
  cuentaOrigen?: string;
  cuentaDestino?: string;
}

export interface DestinatarioDTO {
  numeroCuenta: string;
  nombreTitular: string;
  cedulaParcial: string;
  tipoCuenta?: string;
}

export interface TransferenciaRequest {
  cuentaOrigen: string;
  cuentaDestino: string;
  monto: number;
  descripcion: string;
  bancoDestinoCodigo?: string; // NEXUS, ECUSOL, ARCBANK, etc.
}


export interface Beneficiario {
  id?: number;
  numeroCuenta: string;
  nombreTitular: string;
  alias: string;
  email?: string;
  tipoCuenta?: string;
}