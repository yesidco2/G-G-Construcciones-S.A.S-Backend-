ALTER TABLE public.administradores_autorizados ALTER COLUMN rol DROP DEFAULT;
ALTER TABLE public.administradores_autorizados ALTER COLUMN rol DROP NOT NULL;
UPDATE public.administradores_autorizados SET rol = NULL WHERE rol = 'USUARIO';
ALTER TABLE public.administradores_autorizados DROP CONSTRAINT cuentas_rol_check;
ALTER TABLE public.administradores_autorizados ADD CONSTRAINT cuentas_rol_check
    CHECK (rol IN ('CLIENTE', 'INGENIERO', 'ADMIN'));
