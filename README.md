# GYG_Backend - G&G Construcciones (Spring Boot)

Este es el backend desarrollado en Java con Spring Boot para la empresa de ingeniería civil y consultoría en SST **G&G Construcciones**.

El backend sirve de API REST para un frontend desarrollado en React.

## Tecnologías Utilizadas
- **Java 17**
- **Spring Boot 3.5.16**
- **Maven**
- **PostgreSQL** (Entorno de desarrollo/producción)
- **H2 Database** (Entorno de pruebas/tests de integración)
- **SpringDoc OpenAPI (Swagger UI)**

---

## Requisitos Previos

1. **PostgreSQL** instalado y corriendo localmente.
2. Crear una base de datos en PostgreSQL llamada:
   ```sql
   CREATE DATABASE gygdatabase;
   ```
3. Configura estas variables de entorno para conectar la base de datos:
   - `DB_URL`: URL JDBC de PostgreSQL, por ejemplo `jdbc:postgresql://localhost:5432/gygdatabase`
   - `DB_USER`: usuario de PostgreSQL
   - `DB_PASSWORD`: contraseña de PostgreSQL
   - `APP_CORS_ALLOWED_ORIGINS`: orígenes de frontend separados por comas; en producción no se permiten orígenes locales por defecto.
   - `APP_TRACKING_CODE_PEPPER`: valor aleatorio de al menos 32 bytes, necesario para generar y verificar códigos de seguimiento. Guárdalo en el gestor de secretos; no lo compartas ni lo cambies después de migrar la base de datos.
   - `APP_TRACKING_CODE_LEGACY_ENABLED`: mantiene temporalmente la validación de códigos antiguos migrados a HMAC (`true` por defecto). Rota primero los códigos de las obras existentes y luego establece `false`.
   - `APP_TRUSTED_PROXIES`: expresión regular de las direcciones de los proxies inversos que realmente controlas. Tomcat solo acepta el encabezado IP configurado desde esos proxies; por defecto no se confía en ningún proxy.
   - `APP_REMOTE_IP_HEADER`: encabezado IP del proxy confiable (`X-Forwarded-For` por defecto; `CF-Connecting-IP` solo si Cloudflare está delante y protege el acceso directo al origen).
   - `APP_CAPTCHA_ENABLED`, `APP_CAPTCHA_PROVIDER` y `APP_CAPTCHA_SECRET`: configuración de CAPTCHA para los formularios; la clave secreta de Turnstile es obligatoria siempre que CAPTCHA esté habilitado.
   - `APP_CAPTCHA_EXPECTED_HOSTNAMES` y `APP_CAPTCHA_TICKET_TTL_SECONDS`: hostnames permitidos y duración de los tickets CAPTCHA.
   - `APP_MAIL_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME`, `SMTP_PASSWORD`, `SMTP_AUTH`, `SMTP_STARTTLS_ENABLE`, `SMTP_STARTTLS_REQUIRED` y `SMTP_*_TIMEOUT_MS`: remitente y configuración SMTP; STARTTLS y sus timeouts están habilitados por defecto.
   - `APP_RATE_LIMIT_EMAIL_VERIFICATION_PER_HOUR`: máximo de solicitudes de verificación/reenvío por IP y cuenta (3 por defecto).
   - `APP_RATE_LIMIT_PASSWORD_RESET_PER_HOUR`: máximo de solicitudes de restablecimiento por IP por hora (5 por defecto).
   - `APP_RATE_LIMIT_CONTACTO_PER_HOUR`: máximo de solicitudes de contacto por IP por hora (5 por defecto).
   - `APP_RATE_LIMIT_SEGUIMIENTO_PER_MINUTE`, `APP_RATE_LIMIT_SEGUIMIENTO_ATTEMPTS_PER_15_MIN` y `APP_RATE_LIMIT_SEGUIMIENTO_FAILED_PER_15_MIN`: límites de seguimiento público por IP. Los contadores se comparten entre instancias mediante PostgreSQL.
   - `APP_MAX_REQUEST_BODY_SIZE`: tamaño máximo de los cuerpos HTTP (`1MB` por defecto).
   - `APP_RATE_LIMIT_NEWSLETTER_PER_HOUR`: máximo de suscripciones al newsletter por IP por hora (5 por defecto).
   - `APP_PRIVACY_POLICY_VERSION`, `APP_CHAT_LOG_RETENTION_MONTHS` y `APP_CHAT_LOG_RETENTION_CRON`: versión de consentimiento y política de retención de consultas del chat.
   - `SPRING_PROFILES_ACTIVE=dev` solo para desarrollo local; añade orígenes localhost y carga datos demostrativos.

   Para producción en GitHub Pages, agrega el origen del sitio, por ejemplo `https://<usuario>.github.io`, a `APP_CORS_ALLOWED_ORIGINS`.

4. Para ejecutar con Firebase habilitado, descarga el archivo JSON de credenciales de la cuenta de servicio desde Firebase Console. No lo agregues al repositorio. La ruta del archivo y el ID del proyecto deben estar configurados en el entorno de ejecución.

> No publiques credenciales de base de datos. `application.properties.example` documenta la configuración sin incluir secretos.
> Copia `.env.example` como `.env` para preparar variables localmente y completa valores solo en ese archivo ignorado por Git o en el gestor de secretos del despliegue. En producción, Hibernate usa `ddl-auto=validate`; Flyway crea el esquema en instalaciones nuevas y toma como baseline version 1 los esquemas existentes no vacíos, sin recrear sus tablas ni borrar sus datos.

---

## Instrucciones para Ejecución

### En Windows:
Desde PowerShell, ejecuta el script de desarrollo en la raíz del proyecto:
```powershell
.\run-dev.ps1
```

Configura `FIREBASE_PROJECT_ID`, `FIREBASE_CREDENTIALS_PATH`, `APP_ADMIN_EMAILS`, `DB_URL` y `DB_USER` para tu entorno. Para PostgreSQL, el script usa `DB_PASSWORD` del entorno o de `.env`. Si falta, la solicita de forma interactiva. Flyway administra las migraciones y Hibernate valida el esquema durante el arranque. Si falta `APP_CAPTCHA_SECRET`, el perfil `dev` usa una clave de prueba de Turnstile y el frontend local necesita la sitekey de prueba correspondiente. Configura SMTP en el entorno si necesitas probar el envío real. La base de datos indicada en `DB_URL` debe existir.
Activa el perfil `dev` en la terminal antes de iniciarlo para habilitar los datos de demostración y los orígenes localhost:
```powershell
$env:SPRING_PROFILES_ACTIVE = "dev"
```

Como alternativa, puedes iniciar Spring Boot directamente con el wrapper Maven, siempre que hayas configurado las variables de entorno requeridas (`DB_URL`, `DB_USER`, `DB_PASSWORD` y las de Firebase cuando corresponda):
```powershell
.\mvnw.cmd spring-boot:run
```

### En Linux/macOS:
Configura las variables de entorno requeridas (`DB_URL`, `DB_USER`, `DB_PASSWORD` y las de Firebase cuando corresponda), concede permisos de ejecución al script `mvnw` y ejecútalo desde la raíz del proyecto:
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

El servidor web arrancará por defecto en el puerto **`8080`**: `http://localhost:8080`.

---

## Documentación de la API (Swagger UI)

Una vez el servidor esté corriendo, puedes visualizar e interactuar con toda la API REST y probar los endpoints desde el navegador web ingresando a:
- [Swagger UI - G&G API](http://localhost:8080/swagger-ui/index.html)
- [OpenAPI Spec JSON](http://localhost:8080/v3/api-docs)

---

## Conexión con el Frontend (React)

Para que el frontend consuma este backend, configura las variables de entorno de tu aplicación React (generalmente en un archivo `.env` en la raíz de tu frontend React):
```properties
VITE_API_URL=http://localhost:8080
```
En el perfil `dev`, CORS permite `http://localhost:5173` (puerto por defecto de Vite). En otros entornos, agrega explícitamente el origen del frontend a `APP_CORS_ALLOWED_ORIGINS`.

---

## Estructura de Endpoints de la API

| Método | Endpoint | Descripción |
|---|---|---|
| **GET** | `/api/institucional` | Devuelve la misión, visión y valores de la empresa. |
| **GET** | `/api/datos-legales` | Devuelve el NIT y la dirección física de la empresa. |
| **POST** | `/api/obras/seguimiento` | Recibe `{ "codigo": "..." }` y retorna únicamente datos de seguimiento permitidos. |
| **POST** | `/api/contacto` | Registra una solicitud en `solicitudes_contacto`; requiere consentimiento y CAPTCHA cuando está activo. |
| **POST** | `/api/newsletter` | Registra una suscripción con `{ correo, aceptaPolitica }`; usa `X-Captcha-Token` cuando CAPTCHA está activo. |
| **POST** | `/api/auth/password-reset` | Solicita un enlace oficial de Firebase y lo envía por SMTP; público, limitado por IP y sin requisito CAPTCHA. Responde de forma genérica para no revelar si el correo está autorizado o registrado. |
| **POST** | `/api/auth/email-verification` | Requiere Firebase ID token, pero no sesión CAPTCHA verificada ni body. El destinatario se resuelve desde Firebase; envía un enlace oficial si la cuenta está activa y aún no verificada. |
| **POST** | `/api/chat` | Recibe `{ pregunta }`, procesa una respuesta automática basada en palabras clave de normativas SST, la registra en base de datos y la retorna. |
| **GET** | `/api/admin/consultas-chat?page=0&size=20` | Lista registros de chat paginados; requiere usuario administrador con sesión verificada. |

### Contrato del panel de obras

Todos los endpoints `/api/panel/**` requieren Firebase Authentication, sesión verificada y rol `ADMIN` o `INGENIERO`. El listado de cuentas de clientes y su vinculación a una obra requieren permisos de administrador. El servidor comprueba la asignación del ingeniero a cada obra para editarla o renovar su código.

| Método | Endpoint | Acceso y respuesta |
|---|---|---|
| `GET` | `/api/panel/obras` | ADMIN: todas las obras activas; INGENIERO: solo las asignadas. |
| `GET` | `/api/panel/obras/{id}` | Obra y hitos; comprueba el rol y la asignación. |
| `GET` | `/api/panel/obras/clientes` | Solo ADMIN; devuelve usuarios activos con rol `CLIENTE` (`id`, `nombre`, `email`). |
| `GET` | `/api/panel/obras/estados` | Estados válidos: `PLANIFICADA`, `EN_PROGRESO`, `PAUSADA`, `FINALIZADA`. |
| `POST` | `/api/panel/obras` | Solo ADMIN; crea y devuelve `201 Created`. |
| `PUT` | `/api/panel/obras/{id}` | Reemplazo completo; requiere todos los campos principales. Los hitos se reemplazan; campos opcionales omitidos o nulos se limpian. |
| `PATCH` | `/api/panel/obras/{id}` | Actualización parcial: solo cambia los campos enviados; los omitidos se conservan. `null` limpia `descripcion` o fechas. |
| `PATCH` | `/api/panel/obras/{id}/estado` | Cambia estado respetando transiciones válidas. |
| `PATCH` | `/api/panel/obras/{id}/progreso` | Recibe `{ "progreso": 0..100 }`. |
| `DELETE` | `/api/panel/obras/{id}` | Eliminación lógica; responde `204 No Content`. |
| `POST` | `/api/panel/obras/{id}/codigo-seguimiento` | Solo ADMIN; rota el código y lo devuelve una única vez. |

La creación requiere `nombre`, `cliente`, `categoria`, `estado` y `progreso`. `hitos`, `descripcion` (máximo 2000 caracteres), `fechaInicio` y `fechaFinEstimada` son opcionales. Usa fechas ISO `yyyy-MM-dd` y una fecha final igual o posterior a la inicial. `categoria` admite `CIVIL` o `SST`. Ejemplo:

```json
{
  "nombre": "Reforzamiento estructural",
  "cliente": "Empresa del cliente",
  "categoria": "CIVIL",
  "estado": "PLANIFICADA",
  "progreso": 0,
  "descripcion": "Intervención de la estructura existente",
  "fechaInicio": "2026-08-01",
  "fechaFinEstimada": "2026-12-30",
  "hitos": []
}
```

Un ADMIN o INGENIERO registra una obra con el nombre del cliente. Cuando la registra un INGENIERO, queda asignado automáticamente. ADMIN también puede enviar `clienteId` para vincular una cuenta activa con rol `CLIENTE` y cambiar esa vinculación desde el panel. El nombre de un cliente sin cuenta queda guardado en `apoyo.obras_ingenieros.cliente`.

Las transiciones permitidas son `PLANIFICADA → EN_PROGRESO|PAUSADA`, `EN_PROGRESO → PAUSADA|FINALIZADA` y `PAUSADA → EN_PROGRESO|FINALIZADA`; `FINALIZADA` es terminal. La respuesta de obra incluye `id`, `nombre`, `cliente`, `clienteId`, `categoria`, `estado`, `progreso`, `descripcion`, fechas y `hitos`.

El frontend envía `cliente` como texto al registrar una obra sin cuenta. Para editar campos de forma parcial usa `PATCH` y envía las fechas como cadenas `yyyy-MM-dd`. Las transiciones inválidas responden `409 Conflict` y los datos inválidos `400 Bad Request`.

Un administrador o un ingeniero asignado puede editar el nombre visible del cliente con `PATCH /api/panel/obras/{id}` y el campo `cliente`. Para cambiar la cuenta vinculada se usa `clienteId` con permisos de administrador.

El código de seguimiento de una obra activa no caduca. El administrador o un ingeniero asignado puede reemplazarlo mediante `POST /api/panel/obras/{id}/codigo-seguimiento`. El código anterior se invalida al guardar el nuevo. El panel autorizado recupera el código vigente al recargar. Mantén el mismo `APP_TRACKING_CODE_PEPPER` entre reinicios y despliegues para recuperar y validar los códigos existentes.

El endpoint `/api/contacto` guarda las solicitudes en `solicitudes_contacto`. El contrato de fotografías está documentado más abajo.

### Códigos de seguimiento y transición de datos existentes

Los códigos nuevos tienen 32 caracteres aleatorios generados por `SecureRandom`. PostgreSQL guarda el HMAC-SHA256 para validar el seguimiento y una copia cifrada para mostrar el código en el panel autorizado. V17 agrega la columna de cifrado. Si un código anterior solo tenía HMAC, una consulta pública exitosa con ese código guarda su copia cifrada y el panel puede mostrarlo sin regenerarlo. La respuesta pública omite el código.

La migración `V6` marcó como legacy las obras que existían en ese momento. Mientras `APP_TRACKING_CODE_LEGACY_ENABLED=true`, el backend acepta el formato anterior únicamente en las obras marcadas como legacy. Para retirar el formato antiguo, renueva los códigos de esas obras y entrega el reemplazo por un canal privado antes de configurar `APP_TRACKING_CODE_LEGACY_ENABLED=false`. Las consultas tienen límites persistidos por IP.

### Consentimiento y retención de datos

Las solicitudes de contacto y las suscripciones al newsletter deben incluir `aceptaPolitica: true`. La versión vigente se configura con `APP_PRIVACY_POLICY_VERSION` y se guarda junto con la fecha del consentimiento. La verificación CAPTCHA de contacto y newsletter se controla con `APP_CAPTCHA_ENABLED`, `APP_CAPTCHA_PROVIDER` y `APP_CAPTCHA_SECRET`; si está habilitada, el token del proveedor se envía en el encabezado `X-Captcha-Token`.

El campo `tipoServicio` de contacto admite `Obra Civil`, `Consultoría SST`, `Interventoría Técnica` y `Capacitación Integral`.

Los registros del chat se eliminan automáticamente según `APP_CHAT_LOG_RETENTION_MONTHS` (12 meses por defecto), en el horario definido por `APP_CHAT_LOG_RETENTION_CRON`.

### Datos de desarrollo

En el perfil `dev`, `DataLoader` precarga obras cuando ya existe un usuario activo con rol `CLIENTE` y genera códigos aleatorios con el mismo almacenamiento que el panel.

### CAPTCHA, sesiones y correo Firebase

Turnstile usa `POST /api/auth/captcha/verify` con `{ "captchaToken": "<token>" }`. El frontend debe crear el widget con la acción `login` para ese endpoint; los formularios públicos usan `contacto` y `newsletter`. El backend verifica Siteverify por POST `application/x-www-form-urlencoded`, exige `success`, hostname permitido y acción coincidente. En producción, CAPTCHA habilitado exige secret y hostnames explícitos; el backend rechaza las claves públicas de prueba conocidas de Turnstile. Errores de proveedor/configuración devuelven `503` con un `code` interno; token inválido/expirado devuelve `400`; el límite devuelve `429` y `Retry-After`.

Los hashes de tokens CAPTCHA usados, tickets de un solo uso y sesiones CAPTCHA verificadas se comparten mediante PostgreSQL (migración aditiva `V8`). Los tickets expiran según `APP_CAPTCHA_TICKET_TTL_SECONDS` (120 segundos por defecto) y se ligan a la IP que Spring obtiene del proxy confiable y al `User-Agent`. La IP se toma de `request.getRemoteAddr()` tanto para Siteverify como para rate limit y tickets. No se confía directamente en `X-Forwarded-For` ni en `CF-Connecting-IP`: limita el acceso al origen al proxy, configura `APP_TRUSTED_PROXIES` con sus rangos controlados y elige `APP_REMOTE_IP_HEADER`. En Cloudflare se puede usar `CF-Connecting-IP` solo si el origen no acepta conexiones directas y Cloudflare sobrescribe ese encabezado. Mantén actualizados los rangos del proxy según tu proveedor; no uses `.*`.

El restablecimiento conserva `POST /api/auth/password-reset` con `{ "email": "..." }`. Firebase Admin SDK crea el enlace oficial mediante `generatePasswordResetLink()` y el servidor lo envía con la plantilla HTML corporativa. La respuesta `202` es genérica para correos no autorizados/no existentes y para Firebase o SMTP no disponibles; los errores se registran sin dirección, enlace ni credenciales. No se envían contraseñas ni se inventan códigos o URLs de acción.

`POST /api/auth/email-verification` no acepta correo ni otro body: requiere autenticación Firebase, pero no requiere ticket CAPTCHA ni sesión verificada, para que una cuenta aún no verificada pueda pedir el mensaje. El backend usa el UID autenticado para consultar la cuenta de Firebase, rechaza cuentas inactivas/deshabilitadas, no reenvía si ya está verificada y limita las solicitudes por IP y cuenta. Responde `202 { "status": "sent" }` o `200 { "status": "already_verified" }`. Firebase genera el enlace con `generateEmailVerificationLink()`; los enlaces mantienen el handler oficial de Firebase.

Para ambos correos configura `APP_MAIL_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME` y `SMTP_PASSWORD`. Gmail requiere `SMTP_HOST=smtp.gmail.com`, puerto `587`, `SMTP_AUTH=true`, `SMTP_STARTTLS_ENABLE=true` y `SMTP_STARTTLS_REQUIRED=true`. Las contraseñas de aplicación de Gmail se muestran agrupadas para facilitar su lectura; el backend elimina espacios de formato únicamente cuando el host es `smtp.gmail.com`. No guardes credenciales SMTP en el repositorio. La cuenta debe actualizar su token de Firebase tras completar la verificación para que los claims y el rol reflejen el nuevo estado.

El envío registra la categoría, la cadena de clases de excepción y, cuando se puede extraer sin incluir el mensaje del servidor, el código SMTP. No registra contraseñas, destinatarios ni enlaces. Para probar conexión y autenticación sin enviar correo, configura `APP_MAIL_TEST_CONNECTION=true` en perfil `dev`; al iniciar, el backend ejecuta `JavaMailSenderImpl.testConnection()` y registra el resultado. La prueba está desactivada por defecto. También se puede ejecutar `.\mvnw.cmd "-Dtest=SmtpConnectionDiagnosticTest" test` con `APP_SMTP_DIAGNOSTIC=true` y las variables SMTP cargadas en el entorno de PowerShell.

---

## Ejecución de Pruebas Unitarias y de Integración

Los tests de integración del controlador utilizan una configuración especial con base de datos H2 en memoria (`src/test/resources/application-test.properties`). No requieren que tengas PostgreSQL corriendo para compilar o probar.

Para ejecutar los tests, corre:
```powershell
.\mvnw.cmd test
```

### Cuentas y autorizaciones unificadas

La migracion V12 concentra cuentas y autorizaciones en `administradores_autorizados`. Conserva los IDs de usuarios y las referencias de `obras_ingenieros`. El ID anterior de cada autorizacion se guarda en `autorizacion_id_anterior`.

`rol` y `activo` corresponden a la cuenta. `autorizacion_email`, `rol_autorizado`, `autorizacion_activa`, `autorizado_por` y `autorizado_en` conservan la autorizacion administrativa por correo. Una autorizacion pendiente tiene `firebase_uid` nulo y se vincula al primer token con ese correo verificado. Revocar la autorizacion conserva la cuenta.

Los endpoints `/api/admin/usuarios` y `/api/admin/autorizados` mantienen sus rutas y campos de respuesta. Tras migrar, el frontend debe recargar la lista de autorizaciones y usar el `id` devuelto por GET para DELETE `/api/admin/autorizados/{id}`, ya que ahora corresponde al ID de la cuenta unificada. Las autorizaciones pendientes aparecen en la lista de autorizados. La lista de usuarios muestra cuentas vinculadas a Firebase.

V12 comprueba duplicados de correo antes de modificar el esquema, copia y verifica las autorizaciones, elimina su tabla original y renombra `usuarios` a `administradores_autorizados`. PostgreSQL ejecuta la migracion dentro de una transaccion. El backend desplegado debe incluir el nuevo mapeo JPA al aplicar V12.

### Esquema tecnico de PostgreSQL

Las tablas `api_rate_limit_locks`, `api_rate_limit_windows`, `captcha_session_tickets`, `captcha_used_tokens` y `verified_auth_sessions` se almacenan en `seguridad` desde V13. El historial `flyway_schema_history` se traslada antes de ejecutar Flyway, conservando sus registros. La configuracion detecta historiales duplicados y detiene el arranque para evitar elegir uno arbitrariamente.

Las conexiones de la aplicacion utilizan `search_path = public, seguridad`. Flyway usa una conexion independiente con su historial en `seguridad` y ejecuta las migraciones en `public`, manteniendo compatibles las migraciones anteriores. El perfil de pruebas H2 conserva sus tablas tecnicas en `PUBLIC`. La separacion de esquemas PostgreSQL se verifico tambien contra la base real, incluyendo una segunda ejecucion sin migraciones pendientes.

V14 trasladó hitos, asignaciones, suscripciones y auditoría al esquema `apoyo`. V15 concentra las obras, los hitos, las asignaciones y las fotos en `apoyo.obras_ingenieros`. V15 descarta las obras anteriores por instrucción del propietario.

### Registro y fotografías de obra

React registra la obra con `POST /api/panel/obras`. El JSON usa `nombre`, `cliente`, `categoria`, `estado`, `progreso` y `descripcion`. El registro devuelve `id` y `codigoSeguimiento`. El panel autorizado puede volver a consultar el código vigente.

- Envía cada foto con `POST /api/panel/obras/{id}/fotos` mediante `multipart/form-data`. Usa el campo `archivo` y, opcionalmente, `descripcion`. Se admiten JPEG, PNG y WebP, hasta 5 MB y 4 megapíxeles. Cada obra admite hasta 20 fotos y el servidor las guarda como PNG.
- Lista las fotos del panel con `GET /api/panel/obras/{id}/fotos` o lee `fotos` en la respuesta de la obra. Cada elemento incluye `id`, `nombre`, `url`, `fecha` y `descripcion`. Descarga con `GET /api/panel/obras/{id}/fotos/{fotoId}` y elimina con `DELETE` en esa misma ruta.
- El seguimiento público descarga con `POST /api/obras/seguimiento/fotos/{fotoId}`, mandando `{"codigo":"..."}` en JSON. Usa la respuesta PNG como Blob para mostrarla en la interfaz.
