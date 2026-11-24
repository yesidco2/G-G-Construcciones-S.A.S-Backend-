# Inducciones SST por obra

La migración Flyway `V21__inducciones_sst.sql` agrega el catálogo, las versiones, las asignaciones, la cuadrilla, los verificadores de PIN, las respuestas y el historial de entregas con sus fotos y revisiones. El material inicial de Trabajo en alturas está marcado como ejemplo pendiente de revisión. El responsable SST puede preparar material para su obra y publicar nuevas versiones desde el panel. Cada asignación conserva la versión elegida.

## Configuración y permisos

- Configura `APP_SST_PIN_SECRET` con un secreto aleatorio de al menos 32 bytes y conserva el mismo valor entre réplicas y despliegues. Cambiarlo invalida los PIN emitidos. Se pueden regenerar individualmente desde el panel.
- El panel usa Supabase Auth y la sesión verificada con Turnstile. Pueden gestionar las inducciones los administradores activos y los ingenieros activos asignados a la obra. El backend comprueba ese permiso incluso al consultar una foto.
- El enlace usa un UUID generado en el backend. Se genera un solo QR en el navegador, sin servicios externos. Los números de lista distinguen homónimos.
- Los PIN de seis dígitos se generan con `SecureRandom` y se guardan como HMAC-SHA-256 con un secreto del servidor. Cada valor emitido se reserva dentro de su asignación. Se permiten hasta cinco fallos por trabajador en 15 minutos.
- El token opaco dura cuatro horas como máximo y solo permite consultar o modificar la entrega de ese trabajador. Un nuevo acceso, la regeneración del PIN y el cierre revocan el acceso previo. Las operaciones públicas también comprueban que la obra continúa activa.
- Las fotos se decodifican y reprocesan para eliminar metadatos. El PNG queda en una columna privada de PostgreSQL. Se devuelve como binario con `Cache-Control: no-store` tras comprobar el permiso. Protege los respaldos de la base con los mismos controles aplicados a la evidencia y define su plazo de conservación antes de usar el flujo en una obra real.
- La revisión de una entrega y la autorización para comenzar una tarea son decisiones separadas.

## API del panel

Prefijo `/api/panel/obras/{obra}/inducciones`. Envía los encabezados de sesión del panel con `apiFetch`. Los cuerpos son JSON.

| Método y ruta relativa | Solicitud y respuesta |
|---|---|
| GET `/catalogo` | Lista las versiones visibles con `id, titulo, version, ejemplo`. |
| POST `/catalogo` | Recibe `material` opcional, `titulo` y `contenido`. Devuelve `id, version`. Si se indica `material`, crea una versión para ese material de la obra. |
| GET `/asignaciones` | Lista `id, titulo, version, ejemplo, creada_en, cerrada_en`. |
| POST `/asignaciones` | Recibe `material, version, nombres` (1 a 500). Devuelve `id` y `pins[]` con `id, nombre, numero, pin`. |
| POST `/asignaciones/{id}/cerrar` | Cierra la asignación y revoca los tokens. |
| GET `/asignaciones/{id}/trabajadores` | Devuelve `id, nombre, numero, estado` para cada trabajador. |
| GET `/asignaciones/{id}/trabajadores/{persona}` | Devuelve trabajador, `material`, `respuestas`, última `entrega` e `historial` de envíos y revisiones. |
| POST `/asignaciones/{id}/trabajadores/{persona}/pin` | Emite el nuevo `pin` y revoca el acceso anterior. |
| POST `/asignaciones/{id}/trabajadores/{persona}/foto` | Recibe `entrega` y devuelve la foto binaria de ese envío. |
| POST `/asignaciones/{id}/trabajadores/{persona}/revision` | Recibe `entrega, validar, observacion`. Si `validar` es falso, exige observación. Rechaza una revisión desactualizada con 409. |

El contenido tiene la forma `{secciones: [{titulo, texto}], preguntas: [{texto, opciones, correcta, seccion}]}`. Los índices comienzan en cero. Las respuestas registradas contienen `pregunta, opcion, correcta, intentada_en`. Los envíos contienen `id, enviada_en, revisada_en, revisor_uid, decision, observacion` en el panel.

## API del trabajador

La URL del portal termina en `#induccion/{id}`. El prefijo de API es `/api/inducciones/{id}`. El navegador mantiene el token en memoria y lo envía en `X-Induccion-Token` desde que valida el PIN.

| Método y ruta relativa | Solicitud y respuesta |
|---|---|
| GET `/nombres?buscar=…&pagina=0` | Busca en toda la cuadrilla, sin distinguir tildes ni mayúsculas. Devuelve `total` y hasta 20 `personas` con `id, nombre, numero`. |
| POST `/acceso` | Recibe `trabajador, pin` como JSON, con el PIN como cadena. Devuelve `token`, 401 si el PIN es incorrecto y 429 al alcanzar el límite. |
| GET `/entrega` | Consulta la propia entrega, sección por sección, con preguntas sin clave de respuesta, progreso y observación. |
| POST `/respuestas` | Recibe `pregunta, opcion` como JSON. Devuelve el resultado guardado para ese punto. |
| POST `/entrega` | Recibe multipart con `archivo` y una `referencia` UUID. Requiere todos los aciertos. Reutiliza la referencia si se reintenta el mismo archivo. |
| GET `/foto` | Devuelve el PNG privado de la última entrega propia. |

Un token inválido devuelve 403. Una asignación inexistente o cerrada devuelve 404. Los estados de API son `PENDIENTE`, `POR_REVISAR`, `VALIDADA` y `CORRECCION`. Un nuevo envío tras corrección conserva las respuestas y agrega una foto al historial.

## Pruebas

`mvnw test` en backend y `npm test` en frontend. Las pruebas del backend usan el perfil de pruebas del repositorio y aplican las migraciones.

## Arranque local con el Supabase del equipo

El historial vigente vive en `seguridad`. El registro de baseline que quedó en `public` se conserva cuando contiene únicamente el mismo baseline. El esquema anterior `gyg` conserva su historial por separado.

El Supabase del equipo ya aplicó `V20__perfil_y_documentos.sql`, que aún no está en este repositorio. Para ese entorno, activa `--app.flyway.allow-applied-profile-v20=true`. La excepción requiere que esa sea la única migración aplicada que falta. Los checksums y las demás migraciones se validan. Cuando el archivo original esté disponible, incorpora el archivo y retira la excepción.

Desde PowerShell, en la raíz del backend, carga el `.env` y arranca con el perfil `dev` para usar Turnstile en localhost. Hibernate valida `public` y Flyway registra las migraciones SST en `seguridad`. Las tablas de evidencias están en el esquema privado `apoyo`.

```powershell
Get-Content -LiteralPath '.env' | ForEach-Object {
    if ($_ -match '^\s*([A-Za-z_][A-Za-z0-9_]*)=(.*)$') {
        [Environment]::SetEnvironmentVariable($Matches[1], $Matches[2], 'Process')
    }
}
.\mvnw.cmd spring-boot:run '-Dspring-boot.run.profiles=dev' '-Dspring-boot.run.arguments=--server.address=127.0.0.1 --spring.jpa.hibernate.ddl-auto=validate --spring.jpa.show-sql=false --spring.jpa.properties.hibernate.default_schema=public --app.flyway.allow-applied-profile-v20=true --app.auth.admin-emails='
```
