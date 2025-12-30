# Arquitectura del Sistema EcuSol

Este documento describe la arquitectura técnica, el flujo de comunicación y la estructura de servicios del proyecto EcuSol.

## Visión General

EcuSol es un sistema bancario basado en microservicios que opera bajo un ecosistema de Docker. El sistema simula las operaciones de un banco real, incluyendo gestión de clientes, cuentas, transacciones y geografía, con interfaces tanto para clientes web como para ventanillas de sucursal.

### Diagrama de Principales Componentes

```mermaid
graph TD
    UserWeb[Cliente Web] -->|HTTP:8090| FrontWeb[Frontend Web]
    UserVentanilla[Cajero Ventanilla] -->|HTTP:82| FrontVent[Frontend Ventanilla]

    FrontWeb -->|API Call| Gateway[API Gateway :9180]
    FrontVent -->|API Call| Gateway

    subgraph "Docker Network: ecusol-net"
        Gateway -->|/api/web| WebBack[Web Backend :8082]
        Gateway -->|/api/ventanilla| VentBack[Ventanilla Backend :8083]
        
        Gateway -->|/api/transacciones| MSTrans[MS Transacciones :8082]
        Gateway -->|/api/cuentas| MSCuentas[MS Cuentas :8083]
        Gateway -->|/api/clientes| MSClientes[MS Clientes :8084]
        Gateway -->|/api/geografia| MSGeo[MS Geografía :8081]

        WebBack --> Gateway
        VentBack --> Gateway
        MSTrans -->|HTTP| MSCuentas
        MSTrans -->|HTTP| MSClientes
        MSTrans -->|HTTP| MSGeo

        MSTrans -->|JDBC| DBPostgres[PostgreSQL :5432]
        MSCuentas -->|JDBC| DBPostgres
        MSClientes -->|JDBC| DBPostgres
        WebBack -->|JDBC| DBPostgres
        VentBack -->|JDBC| DBPostgres
        
        MSGeo -->|MongoDriver| DBMongo[MongoDB :27017]
    end

    Gateway -.-> SwitchNube[Switch Transaccional (Nube)]
```

## Estructura de Servicios

### 1. Frontend (Capas Visuales)
- **Frontend Web (`front-web-ecusol`)**:
  - **Puerto Externo**: `8090`
  - **Tecnología**: Angular (asumido por estructura).
  - **Función**: Banca en línea para clientes finales.
- **Frontend Ventanilla (`front-ventanilla-ecusol`)**:
  - **Puerto Externo**: `82`
  - **Tecnología**: Angular.
  - **Función**: Interfaz para cajeros y personal del banco.

### 2. Capa de Entrada (Gateway)
- **API Gateway (`ecusol-gateway`)**:
  - **Puerto Externo**: `9180` (Interno `8080`)
  - **Función**: Punto único de entrada. Enruta peticiónes al servicio correspondiente basándose en la URL.
  - **Rutas Clave**:
    - `/api/web/**` -> `web-backend-ecusol`
    - `/api/ventanilla/**` -> `ventanilla-backend-ecusol`
    - `/api/v1/transacciones/**` -> `ecusol-ms-transacciones`
    - `/api/cuentas/**` -> `ecusol-ms-cuentas`

### 3. Backends de Negocio (BFF - Backend for Frontend)
Estos servicios actúan como intermediarios para las interfaces específicas, manejando lógica de presentación y orquestación simple.
- **Web Backend (`web-backend-ecusol`)**: Puerto interno `8082`.
- **Ventanilla Backend (`ventanilla-backend-ecusol`)**: Puerto Externo `8185` (Interno `8083`).

### 4. Core Bancario (Microservicios)
Estos son los servicios puros de dominio que manejan la lógica dura del banco.
- **MS Transacciones (`ecusol-ms-transacciones`)**:
  - **Puerto Externo**: `8182` (Interno `8082`).
  - **Responsabilidad**: Transferencias, depósitos, retiros. Se comunica con el Switch para transferencias interbancarias.
- **MS Cuentas (`ecusol-ms-cuentas`)**:
  - **Responsabilidad**: Saldos, creación de cuentas, tasas de interés.
- **MS Clientes (`ecusol-ms-clientes`)**:
  - **Responsabilidad**: Datos personales, KYC.
- **MS Geografía (`ecusol-ms-geografia`)**:
  - **Responsabilidad**: Ubicaciones, sucursales. Usa MongoDB.

### 5. Bases de Datos
- **PostgreSQL (`postgres-db-ecusol`)**:
  - **Puerto Externo**: `5434`.
  - **Uso**: Base de datos relacional principal. Microservicios usan esquemas separados (`ecusol_transacciones`, `ecusol_cuentas`, etc.) para mantener aislamiento lógico.
- **MongoDB (`mongo-db-ecusol`)**:
  - **Puerto Externo**: `27019`.
  - **Uso**: Datos no relacionales para geografía.

## Mapa de Puertos (Resumen)

| Servicio | Puerto Docker (Interno) | Puerto Host (Para acceder desde Windows) |
| :--- | :--- | :--- |
| **API Gateway** | 8080 | **9180** (Punto principal de API) |
| Frontend Web | 80 | **8090** (http://localhost:8090) |
| Frontend Ventanilla | 80 | **82** (http://localhost:82) |
| MS Transacciones | 8082 | **8182** |
| Backend Ventanilla | 8083 | **8185** |
| PostgreSQL | 5432 | **5434** |
| MongoDB | 27017 | **27019** |

## Flujo de una Transacción Típica

1. **Usuario** inicia sesión en `http://localhost:8090` (Frontend Web).
2. **Frontend** envía credenciales a `http://localhost:9180/api/auth/login`.
3. **Gateway** recibe la petición en puerto `8080` y la enruta a `web-backend-ecusol`.
4. **Web Backend** valida usuario en PostgreSQL.
5. Usuario realiza una transferencia:
   - Frontend envía POST a `http://localhost:9180/api/v1/transacciones`.
   - Gateway enruta a `ecusol-ms-transacciones`.
   - **MS Transacciones**:
     - Verifica saldo llamando a `ecusol-ms-cuentas`.
     - Si es a otro banco, llama al **Switch** (Nube).
     - Registra la transacción en PostgreSQL.
   - Retorna éxito al Frontend.
