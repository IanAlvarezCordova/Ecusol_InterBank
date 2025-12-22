-- Update ECUSOL webhook endpoint
UPDATE "Bancos" 
SET "Endpoint" = 'http://ecusol-gateway:8080/api/transacciones/webhook' 
WHERE "Codigo" = 'ECUSOL';

-- Verify the update
SELECT "Codigo", "Nombre", "Endpoint", "Estado" FROM "Bancos";
