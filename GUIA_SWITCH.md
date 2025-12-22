# 🌐 Integración con Switch DIGICONECU

Esta guía detalla los pasos para conectar tu instancia local de **ECUSOL** con el Switch interbancario en la nube.

---

## 📍 Datos del Switch (Nube)

| Parámetro | Valor |
|-----------|-------|
| **URL del Switch** | `http://34.44.123.236:9080/api/v2/transfers` |
| **IP del Servidor** | `34.44.123.236` |
| **Puerto Gateway** | `9080` |

---

## 🛠️ Paso 1: Configuración en `docker-compose.yml`

Para que el microservicio de transacciones apunte al Switch correcto, edita el archivo `docker-compose.yml`:

```yaml
  ecusol-ms-transacciones:
    # ...
    environment:
      - APP_SWITCH_URL=http://34.44.123.236:9080
      - BANCO_CODIGO=ECUSOL
```

---

## 🚇 Paso 2: Exponer el Webhook local (ngrok)

Como tu banco corre en `localhost`, el Switch en la nube no podrá enviarte dinero (webhook) a menos que uses un túnel.

1. Descarga e instala [ngrok](https://ngrok.com/).
2. Ejecuta el túnel apuntando al puerto del **Gateway** (9091):
   ```bash
   ngrok http 9091
   ```
3. Copia la URL generada (ej: `https://abcd-123.ngrok-free.app`).
4. Tu endpoint de webhook será: `https://abcd-123.ngrok-free.app/api/transacciones/webhook`.

---

## 📝 Paso 3: Registro en el Switch

Debes registrar tanto a tu banco (**ECUSOL**) como a los demás bancos en la base de datos del Switch para que el enrutamiento funcione.

### 1. Registrar Bancos
```sql
-- Registrar ECUSOL (Local)
INSERT INTO "Bancos" ("Id", "Codigo", "Nombre", "Endpoint", "Estado", "EstadoCircuito")
VALUES (gen_random_uuid(), 'ECUSOL', 'ECUSOL Bank Local', 'https://TU_URL_NGROK/api/transacciones/webhook', 'Activo', 'CLOSED')
ON CONFLICT ("Codigo") DO UPDATE SET "Endpoint" = EXCLUDED."Endpoint";

-- Registrar NEXUS (Externo)
INSERT INTO "Bancos" ("Id", "Codigo", "Nombre", "Endpoint", "Estado", "EstadoCircuito")
VALUES (gen_random_uuid(), 'NEXUS', 'Nexus Bank', 'http://34.44.123.236:9080/api/transacciones/webhook', 'Activo', 'CLOSED')
ON CONFLICT ("Codigo") DO NOTHING;

-- Registrar BANTEC (Externo)
INSERT INTO "Bancos" ("Id", "Codigo", "Nombre", "Endpoint", "Estado", "EstadoCircuito")
VALUES (gen_random_uuid(), 'BANTEC', 'Bantec', 'http://34.44.123.236:9080/api/transacciones/webhook', 'Activo', 'CLOSED')
ON CONFLICT ("Codigo") DO NOTHING;
```

### 2. Registrar BINs (Enrutamiento)
```sql
-- BIN ECUSOL (280900)
INSERT INTO "Enrutamiento" ("Id", "BancoId", "BinInicio", "BinFin", "Activo")
SELECT gen_random_uuid(), "Id", '280900', '280900', true FROM "Bancos" WHERE "Codigo" = 'ECUSOL';

-- BIN NEXUS (270100)
INSERT INTO "Enrutamiento" ("Id", "BancoId", "BinInicio", "BinFin", "Activo")
SELECT gen_random_uuid(), "Id", '270100', '270100', true FROM "Bancos" WHERE "Codigo" = 'NEXUS';

-- BINs BANTEC (220100, 220000)
INSERT INTO "Enrutamiento" ("Id", "BancoId", "BinInicio", "BinFin", "Activo")
SELECT gen_random_uuid(), "Id", '220100', '220100', true FROM "Bancos" WHERE "Codigo" = 'BANTEC';
INSERT INTO "Enrutamiento" ("Id", "BancoId", "BinInicio", "BinFin", "Activo")
SELECT gen_random_uuid(), "Id", '220000', '220000', true FROM "Bancos" WHERE "Codigo" = 'BANTEC';

-- Rango ARCBANK (230000 - 230099)
INSERT INTO "Enrutamiento" ("Id", "BancoId", "BinInicio", "BinFin", "Activo")
SELECT gen_random_uuid(), "Id", '230000', '230099', true FROM "Bancos" WHERE "Codigo" = 'ARCBANK';
```

---

## 🔍 Checklist de Verificación

- [ ] **UUID**: El campo `instructionId` debe ser un UUID válido.
- [ ] **BIN**: Las cuentas en ECUSOL deben comenzar con `280900XXXX`.
- [ ] **Webhook**: Responder con `{ "status": "ACK", ... }` (ya implementado en el código).
- [ ] **Firewall**: El puerto `9091` debe estar accesible (ngrok maneja esto).

---

## 🔄 Flujo de una Transferencia

1. **Salida**: ECUSOL -> Switch (Nube) -> ARCBANK
2. **Entrada**: ARCBANK -> Switch (Nube) -> ngrok -> ECUSOL (Local)

---
*Documentación generada para el equipo de desarrollo de ECUSOL.*
