-- =====================================================
-- SCRIPT: Limpiar cuentas con BIN incorrecto en ECUSOL
-- =====================================================
-- Las cuentas de ECUSOL deben empezar con 280900
-- Este script elimina/actualiza cuentas con BINs incorrectos

-- 1. Ver cuentas actuales (ejecutar primero para revisar)
SELECT cuenta_id, numero_cuenta, cliente_id, saldo, estado 
FROM ecusol_cuentas.cuenta 
ORDER BY cuenta_id;

-- 2. Eliminar la cuenta específica mencionada (si tiene saldo 0)
-- La cuenta 6360715848 tiene BIN incorrecto (636...)
DELETE FROM ecusol_cuentas.cuenta 
WHERE numero_cuenta = '6360715848' 
AND saldo = 0;

-- 3. Si la cuenta tiene saldo, primero transferir a otra cuenta válida
-- O marcarla como INACTIVA para no usarla más:
UPDATE ecusol_cuentas.cuenta 
SET estado = 'INACTIVA' 
WHERE numero_cuenta = '6360715848';

-- 4. Ver transacciones asociadas a cuentas incorrectas
SELECT * FROM ecusol_transacciones.transaccion 
WHERE cuenta_origen LIKE '636%' OR cuenta_destino LIKE '636%';

-- 5. Listar todas las cuentas que NO tienen el BIN correcto 280900
SELECT cuenta_id, numero_cuenta, cliente_id, saldo, estado 
FROM ecusol_cuentas.cuenta 
WHERE numero_cuenta NOT LIKE '280900%';

-- =====================================================
-- NOTA: Después de ejecutar este script, reconstruir CBS:
-- docker-compose up --build ecusol-ms-cuentas -d
-- =====================================================
