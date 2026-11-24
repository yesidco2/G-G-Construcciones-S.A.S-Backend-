ALTER TABLE logs_chat ADD COLUMN acepta_envio_proveedor BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE logs_chat ADD COLUMN version_politica VARCHAR(50);
ALTER TABLE logs_chat ADD COLUMN fecha_consentimiento TIMESTAMP WITHOUT TIME ZONE;
