
import { apiClient } from "./apiClient";
import { CuentaDTO, DestinatarioDTO, MovimientoDTO, TransferenciaRequest } from "@/types";


export interface Beneficiario {
  tipoCuenta: any;
  id?: number;
  numeroCuenta: string;
  nombreTitular: string;
  alias: string;
  email?: string;
}

export interface Sucursal {
  id: number;
  nombre: string;
  direccion: string;
  telefono: string;
  lat: number;
  lng: number;
}

export const bancaService = {
  getMisCuentas: async () => {
    return await apiClient<CuentaDTO[]>('/web/cuentas');
  },

  getMovimientos: async (numeroCuenta: string) => {
    return await apiClient<MovimientoDTO[]>(`/web/movimientos/${numeroCuenta}`);
  },

  validarDestinatario: async (numeroCuenta: string, banco?: string) => {
    const url = banco
      ? `/web/validar-destinatario/${numeroCuenta}?banco=${banco}`
      : `/web/validar-destinatario/${numeroCuenta}`;
    return await apiClient<DestinatarioDTO>(url);
  },

  transferir: async (data: TransferenciaRequest) => {
    return await apiClient<string>('/web/transferir', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  },

  solicitarCuenta: async (tipoCuentaId: number) => {
    return await apiClient<string>(`/web/solicitar-cuenta?tipoCuentaId=${tipoCuentaId}`, {
      method: 'POST'
    });
  },


  getBeneficiarios: async () => {
    return await apiClient<Beneficiario[]>('/web/beneficiarios');
  },

  guardarBeneficiario: async (data: Beneficiario) => {
    return await apiClient<string>('/web/beneficiarios', {
      method: 'POST',
      body: JSON.stringify(data)
    });
  },

  getSucursales: async () => {

    return await apiClient<Sucursal[]>('/web/sucursales');
  },

  solicitarDevolucion: async (
    originalInstructionId: string,
    motivo: string,
    numeroCuentaPropietaria: string
  ) => {
    // Endpoint apunta a TransaccionClienteController POST /api/v1/transacciones/devoluciones
    // PERO ojo, apiClient suele apuntar a Gateway o microservicio Específico.
    // Revisado apiClient: apunta a /api/v1 (generalmente) o gestiona prefijos.
    // En WebBackendController hay endpoints /web/... que proxyan.
    // Sin embargo, configuramos TransaccionClienteController en ms-transacciones.
    // El WebBackend debería tener un endpoint espejo o el Gateway rutear directo.
    // Para no complicar con WebBackend, usaremos la ruta directa del Gateway si es posible /api/v1/transacciones/...

    // Asumiendo que apiClient maneja base URL del gateway:
    return await apiClient<{ message: string }>('/transacciones/devoluciones', {
      method: 'POST',
      body: JSON.stringify({
        originalInstructionId,
        motivo,
        numeroCuentaPropietaria
      })
    });
  }
};