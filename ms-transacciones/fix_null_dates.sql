-- Migration script to fix null fechaEjecucion for existing transactions
-- Run this against the ecusol_transacciones database

-- Update COMPLETED transactions without fechaEjecucion
-- Set to current timestamp (best effort for legacy data)
UPDATE transaccion 
SET fecha_ejecucion = CURRENT_TIMESTAMP 
WHERE fecha_ejecucion IS NULL 
  AND estado = 'COMPLETED';

-- Update FAILED transactions without fechaEjecucion
UPDATE transaccion 
SET fecha_ejecucion = CURRENT_TIMESTAMP 
WHERE fecha_ejecucion IS NULL 
  AND estado = 'FAILED';

-- Update PENDING transactions without fechaEjecucion (if any)
UPDATE transaccion 
SET fecha_ejecucion = CURRENT_TIMESTAMP 
WHERE fecha_ejecucion IS NULL 
  AND estado = 'PENDING';

-- Verify the update
SELECT estado, COUNT(*) as count, 
       SUM(CASE WHEN fecha_ejecucion IS NULL THEN 1 ELSE 0 END) as null_count
FROM transaccion
GROUP BY estado;
