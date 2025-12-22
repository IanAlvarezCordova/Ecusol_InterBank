-- Update ECUSOL webhook endpoint
UPDATE "Bancos" 
SET "EndPoint" = 'http://ecusol-gateway:8080/api/transacciones/webhook' 
WHERE "Codigo" = 'ECUSOL';

-- Verify the update
SELECT "Id", "Codigo", "Nombre", "EndPoint", "Estado" FROM "Bancos";
