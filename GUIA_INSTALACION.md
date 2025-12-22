# 🏦 ECUSOL - Guía de Instalación

## 📋 Requisitos Previos

Antes de empezar, asegúrate de tener instalado:

| Software | Versión Mínima | Link de Descarga |
|----------|----------------|------------------|
| **Docker Desktop** | 4.x | [docker.com/desktop](https://www.docker.com/products/docker-desktop/) |
| **Git** (opcional) | 2.x | [git-scm.com](https://git-scm.com/downloads) |

> ⚠️ **IMPORTANTE**: Docker Desktop debe estar **corriendo** antes de ejecutar los comandos.

---

## 🚀 Pasos para Levantar el Proyecto

### 1️⃣ Extraer el ZIP
Extrae el archivo ZIP en una carpeta de tu preferencia, por ejemplo:
```
C:\Proyectos\Microservicios_Ecusol-main
```

### 2️⃣ Abrir Terminal en la Carpeta del Proyecto
- Abre **PowerShell** o **CMD**
- Navega a la carpeta del proyecto:
```powershell
cd C:\Proyectos\Microservicios_Ecusol-main
```

### 3️⃣ Construir y Levantar los Contenedores
Ejecuta el siguiente comando (la primera vez toma varios minutos):
```powershell
docker compose up --build
```

> 💡 **Tip**: Para ejecutar en segundo plano, usa: `docker compose up --build -d`

### 4️⃣ Esperar que todo suba
Verás muchos logs. Espera hasta que veas mensajes como:
```
ecusol-gateway         | Started GatewayServerApplication...
ecusol-web-backend     | Started WebBackendApplication...
```

---

## 🌐 URLs de Acceso

Una vez que los contenedores estén corriendo:

| Módulo | URL | Descripción |
|--------|-----|-------------|
| **Frontend Web** | http://localhost:8091 | Aplicación web del cliente |
| **Frontend Ventanilla** | http://localhost:8089 | Módulo de caja/ventanilla |
| **API Gateway** | http://localhost:9091 | Gateway principal |
| **PostgreSQL** | `localhost:5434` | Base de datos (user: `postgres`, pass: `admin`) |
| **MongoDB** | `localhost:27019` | Base de datos geográfica |

---

## 📦 Servicios que se Levantan

| Contenedor | Puerto | Función |
|------------|--------|---------|
| `postgres-db-ecusol` | 5434 | Base de datos PostgreSQL |
| `mongo-db-ecusol` | 27019 | MongoDB para geografía |
| `ecusol-gateway` | 9091 | API Gateway |
| `ecusol-ms-cuentas` | - | Microservicio de Cuentas |
| `ecusol-ms-clientes` | - | Microservicio de Clientes |
| `ecusol-ms-geografia` | - | Microservicio de Geografía |
| `ecusol-ms-transacciones` | 8095 | Microservicio de Transacciones |
| `ecusol-web-backend` | - | Backend Web |
| `ecusol-front-web` | 8091 | Frontend Web |
| `ecusol-ventanilla-backend` | 8088 | Backend Ventanilla |
| `ecusol-front-ventanilla` | 8089 | Frontend Ventanilla |

---

## 🔧 Comandos Útiles

```powershell
# Ver contenedores corriendo
docker ps

# Ver logs de un servicio específico
docker logs ecusol-web-backend -f

# Detener todos los servicios
docker compose down

# Detener y eliminar volúmenes (BORRA LA BASE DE DATOS)
docker compose down -v

# Reconstruir un servicio específico
docker compose up --build ecusol-web-backend
```

---

## ⚠️ Problemas Comunes

### Error: "port is already allocated"
Algún puerto ya está en uso. Cierra la aplicación que lo usa o cambia el puerto en `docker-compose.yml`.

### Error: "Cannot connect to the Docker daemon"
Docker Desktop no está corriendo. Ábrelo y espera a que inicie.

### Los contenedores se reinician constantemente
Revisa los logs: `docker logs ecusol-web-backend` para ver el error.

### La base de datos está vacía
Los esquemas se crean automáticamente. Si necesitas datos de prueba, pregunta por el archivo de backup SQL.

---

## 💸 Transferencias Interbancarias

Para habilitar transferencias con otros bancos (**Nexus, ArcBank, Bantec**), sigue la guía especializada:

👉 **[GUIA_SWITCH.md](file:///c:/Users/stephani.rivera/Documents/Microservicios_Ecusol-main/GUIA_SWITCH.md)**

---

## 👤 Credenciales de Prueba

Después de crear un usuario en la app web:
- **Usuario**: (el que registres)
- **Contraseña**: (la que pongas al registrarte)

Para ventanilla, primero debes crear usuarios desde el backend.

---

## 📞 ¿Dudas?

Contacta a Stephani Rivera para soporte adicional.
