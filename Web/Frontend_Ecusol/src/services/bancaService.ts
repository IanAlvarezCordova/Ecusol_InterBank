
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
    // Si es banco externo (no Ecusol y no vacío), usar el endpoint de Transacciones (Account Lookup)
    if (banco && banco !== 'ECUSOL_BK' && banco !== 'ECUASOL') {
      const response = await apiClient<any>('/transacciones/validar-cuenta', {
        method: 'POST',
        body: JSON.stringify({
          targetBankId: banco,
          targetAccountNumber: numeroCuenta
        })
      });

      if (response.status === 'SUCCESS' && response.data.exists) {
        return {
          nombreTitular: response.data.ownerName,
          cedulaParcial: "******", // No retornada por Switch
          tipoCuenta: "Cuenta Externa",
          numeroCuenta: numeroCuenta
        };
      } else {
        throw new Error(response.data?.mensaje || "Cuenta no encontrada en banco destino");
      }
    }

    // Lógica original para Ecusol (Web Backend)
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
    // Endpoint apunta a BFF (BancaWebController)
    return await apiClient<{ message: string }>('/web/transacciones/devolucion', {
      method: 'POST',
      body: JSON.stringify({
        idTransaccion: originalInstructionId, // BFF expects idTransaccion
        motivo,
        numeroCuenta: numeroCuentaPropietaria // BFF expects numeroCuenta
      })
    });
  }
};