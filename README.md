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
   - `APP_RATE_LIMIT_SEGUIMIENTO_PER_MINUTE`, `APP_RATE_LIMIT_SEGUIMIENTO_ATTEMPTS_PER_15_MIN` y `APP_RATE_LIMIT_SEGUIMIENTO_FAILED_PER_15_MIN`: límites de seguimiento público por IP. Los contadores se comparten entre instancias mediante PostgreSQL.
   - `APP_MAX_REQUEST_BODY_SIZE`: máximo para cuerpos HTTP no multipart (`1MB` por defecto).
   - `APP_MULTIPART_MAX_FILE_SIZE` y `APP_MULTIPART_MAX_REQUEST_SIZE`: límites de subida multipart (`5MB` por archivo y `25MB` por petición, por defecto).
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

Antes de usarlo, revisa `run-dev.ps1` y ajusta `FIREBASE_PROJECT_ID`, `FIREBASE_CREDENTIALS_PATH`, `APP_ADMIN_EMAILS`, `DB_URL` y `DB_USER` a tu entorno. El script comprueba que exista el archivo de credenciales de Firebase y solicita la contraseña de PostgreSQL al ejecutarse; no la guarda en el archivo. Si falta `APP_CAPTCHA_SECRET`, usa una clave de prueba de Turnstile únicamente en perfil `dev`; el frontend local debe usar la sitekey de prueba correspondiente. No copies esas claves al entorno productivo. Configura SMTP en el entorno si necesitas probar el envío real. La base de datos indicada en `DB_URL` debe existir.
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
| **GET** | `/api/servicios` | Retorna los servicios ofrecidos organizados por bloque (`CIVIL` o `SST`). |
| **GET** | `/api/datos-legales` | Devuelve el NIT y la dirección física de la empresa. |
| **GET** | `/api/proyectos` | Retorna el portafolio de proyectos. Opcionalmente filtrable por query param `?categoria=CIVIL` o `?categoria=SST`. |
| **POST** | `/api/obras/seguimiento` | Recibe `{ "codigo": "..." }` y retorna únicamente datos de seguimiento permitidos. |
| **POST** | `/api/obras/seguimiento/fotos/{fotoId}` | Retorna una foto privada como `image/png`; requiere `{ "codigo": "..." }` en el cuerpo y límites por IP. |
| **POST** | `/api/contacto` | Recibe `{ nombre, empresa, correo, telefono, tipoServicio, mensaje, aceptaPolitica, politicaVersion, sitioWeb }`. Requiere nombre, mensaje, consentimiento y al menos correo o teléfono; empresa, correo, teléfono y versión de política aceptan cadena vacía como ausente. El honeypot `sitioWeb` debe enviarse vacío. Cuando CAPTCHA está activo, requiere `X-Captcha-Token`. |
| **POST** | `/api/newsletter` | Registra una suscripción con `{ correo, aceptaPolitica }`; usa `X-Captcha-Token` cuando CAPTCHA está activo. |
| **POST** | `/api/auth/password-reset` | Solicita un enlace oficial de Firebase y lo envía por SMTP; público, limitado por IP y sin requisito CAPTCHA. Responde de forma genérica para no revelar si el correo está autorizado o registrado. |
| **POST** | `/api/auth/email-verification` | Requiere Firebase ID token, pero no sesión CAPTCHA verificada ni body. El destinatario se resuelve desde Firebase; envía un enlace oficial si la cuenta está activa y aún no verificada. |
| **POST** | `/api/chat` | Recibe `{ pregunta }`, procesa una respuesta automática basada en palabras clave de normativas SST, la registra en base de datos y la retorna. |
| **GET** | `/api/admin/consultas-chat?page=0&size=20` | Lista registros de chat paginados; requiere usuario administrador con sesión verificada. |
| **GET** | `/api/riesgos/labores` | Retorna la lista de actividades/labores críticas de seguridad laboral. |
| **GET** | `/api/riesgos/{laborId}` | Retorna la matriz de riesgos detallada y los EPP sugeridos para una labor en particular. |
| **GET** | `/api/politicas-ambientales` | Devuelve la lista de políticas y compromisos de mitigación ambiental de la empresa. |

### Contrato del panel de obras

Todos los endpoints `/api/panel/**` requieren Firebase Authentication, sesión verificada y rol `ADMIN` o `INGENIERO`; las operaciones de clientes y rotación de códigos son exclusivas de `ADMIN`. El servidor comprueba además la asignación del ingeniero a cada obra.

| Método | Endpoint | Acceso y respuesta |
|---|---|---|
| `GET` | `/api/panel/obras` | ADMIN: todas las obras activas; INGENIERO: solo las asignadas. |
| `GET` | `/api/panel/obras/{id}` | Obra y fotos; comprueba el rol y la asignación. |
| `GET` | `/api/panel/obras/clientes` | Solo ADMIN; devuelve usuarios activos con rol `CLIENTE` (`id`, `nombre`, `email`). |
| `GET` | `/api/panel/obras/estados` | Estados válidos: `PLANIFICADA`, `EN_PROGRESO`, `PAUSADA`, `FINALIZADA`. |
| `POST` | `/api/panel/obras` | Solo ADMIN; crea y devuelve `201 Created`. |
| `PUT` | `/api/panel/obras/{id}` | Reemplazo completo; requiere todos los campos principales. Los hitos se reemplazan; campos opcionales omitidos o nulos se limpian. |
| `PATCH` | `/api/panel/obras/{id}` | Actualización parcial: solo cambia los campos enviados; los omitidos se conservan. `null` limpia `descripcion` o fechas. |
| `PATCH` | `/api/panel/obras/{id}/estado` | Cambia estado respetando transiciones válidas. |
| `PATCH` | `/api/panel/obras/{id}/progreso` | Recibe `{ "progreso": 0..100 }`. |
| `DELETE` | `/api/panel/obras/{id}` | Eliminación lógica; responde `204 No Content`. |
| `POST` | `/api/panel/obras/{id}/codigo-seguimiento` | Solo ADMIN; rota el código y lo devuelve una única vez. |
| `POST` | `/api/panel/obras/{id}/fotos` | Multipart: `archivo` y `descripcion` opcional. Responde la obra actualizada y su lista de fotos. |
| `GET` | `/api/panel/obras/{id}/fotos/{fotoId}` | Descarga inline; requiere bearer token y permiso sobre la obra. |
| `DELETE` | `/api/panel/obras/{id}/fotos/{fotoId}` | Elimina la foto; responde `204 No Content`. |

El cuerpo de creación y reemplazo requiere `nombre`, `clienteId`, `categoria`, `estado` y `progreso`; `hitos` es opcional. `descripcion` (máximo 2000 caracteres), `fechaInicio` y `fechaFinEstimada` son opcionales, con fechas ISO `yyyy-MM-dd`; cuando ambas existen, la fecha fin no puede ser anterior a la de inicio. `categoria` admite `CIVIL` o `SST`. Ejemplo:

```json
{
  "nombre": "Reforzamiento estructural",
  "clienteId": 42,
  "categoria": "CIVIL",
  "estado": "PLANIFICADA",
  "progreso": 0,
  "descripcion": "Intervención de la estructura existente",
  "fechaInicio": "2026-08-01",
  "fechaFinEstimada": "2026-12-30",
  "hitos": []
}
```

`clienteId` debe identificar un usuario activo con rol `CLIENTE`; el texto `cliente` que algunos formularios antiguos envían no se usa para asignar el cliente. ADMIN asigna el cliente al crear o puede cambiarlo; INGENIERO no puede crear ni cambiar la asignación. Un administrador habilita una cuenta existente como cliente desde `PATCH /api/admin/usuarios/{id}` con `{ "rol": "CLIENTE" }`. Ningún DTO acepta que el cliente establezca `id`, roles, hash, borrado lógico o código de seguimiento.

Las transiciones permitidas son `PLANIFICADA → EN_PROGRESO|PAUSADA`, `EN_PROGRESO → PAUSADA|FINALIZADA` y `PAUSADA → EN_PROGRESO|FINALIZADA`; `FINALIZADA` es terminal. La respuesta de obra incluye `id`, `nombre`, `cliente`, `clienteId`, `categoria`, `estado`, `progreso`, `descripcion`, fechas, `hitos` y `fotos`. Cada foto contiene `id`, `url`, `fecha`, `descripcion` y, solo en el panel autorizado, `subidoPor`.

El frontend debe dejar de enviar `codigo` y seleccionar el cliente mediante `clienteId` obtenido de `/api/panel/obras/clientes`. Puede conservar `cliente` como etiqueta solo de lectura, pero el servidor lo ignora en escritura. Para editar sin perder campos no incluidos en el formulario debe usar `PATCH`; fechas y fechas límite se envían como cadenas `yyyy-MM-dd`. Las transiciones inválidas responden `409 Conflict` y los datos inválidos `400 Bad Request`.

Para mostrar fotos del panel, el frontend debe descargar el endpoint protegido con `Authorization: Bearer <Firebase ID token>` y asignar la respuesta `Blob` a la vista; no debe usar una etiqueta `<img>` sin token. Para una consulta pública, `url` apunta a `/api/obras/seguimiento/fotos/{fotoId}`: el frontend debe hacer `POST` con `{ "codigo": "<código de seguimiento>" }` y mostrar la imagen Blob. El código no se incluye en la URL.

### Códigos de seguimiento y transición de datos existentes

Los códigos nuevos tienen 32 caracteres aleatorios generados por `SecureRandom`; PostgreSQL almacena solo HMAC-SHA256 con `APP_TRACKING_CODE_PEPPER`. `codigoSeguimiento` aparece únicamente en la respuesta de creación o rotación y debe tratarse como secreto de un solo acceso. Nunca se aceptan códigos enviados al crear obras ni se retornan hashes.

La migración `V6` marca como legacy las obras que ya existían, sin recuperar ni revelar sus códigos originales. Mientras `APP_TRACKING_CODE_LEGACY_ENABLED=true`, esas obras solo se consultan comparando el código presentado contra su HMAC; las consultas tienen límites persistidos por IP y los errores no distinguen obras inexistentes. Para retirar el formato antiguo: entrega primero un código nuevo rotando cada obra existente mediante el endpoint ADMIN de rotación; comunica el código por un canal seguro; después configura `APP_TRACKING_CODE_LEGACY_ENABLED=false` y reinicia. Los códigos anteriores dejarán de funcionar; sus HMAC no necesitan ni deben revertirse. Las obras nuevas y las obras rotadas siempre usan el formato seguro de 32 caracteres.

### Consentimiento y retención de datos

Las solicitudes de contacto y las suscripciones al newsletter deben incluir `aceptaPolitica: true`. La versión vigente se configura con `APP_PRIVACY_POLICY_VERSION` y se guarda junto con la fecha del consentimiento. La verificación CAPTCHA de contacto y newsletter se controla con `APP_CAPTCHA_ENABLED`, `APP_CAPTCHA_PROVIDER` y `APP_CAPTCHA_SECRET`; si está habilitada, el token del proveedor se envía en el encabezado `X-Captcha-Token`.

El campo `tipoServicio` de contacto admite los valores del selector del frontend: `Obra Civil`, `Consultoría SST`, `Interventoría Técnica` y `Capacitación Integral`.

Los registros del chat se eliminan automáticamente según `APP_CHAT_LOG_RETENTION_MONTHS` (12 meses por defecto), en el horario definido por `APP_CHAT_LOG_RETENTION_CRON`.

### Datos de desarrollo

En el perfil `dev`, `DataLoader` solo precarga obras cuando ya existe un usuario activo con rol `CLIENTE`; genera códigos aleatorios y guarda únicamente su HMAC. No hay códigos de demostración fijos. Las obras nuevas creadas desde el panel devuelven el código una vez. Al migrar códigos almacenados en texto plano, Flyway requiere `APP_TRACKING_CODE_PEPPER` y los sustituye por HMAC; nunca intentes reconstruir el valor original desde el hash.

### CAPTCHA, sesiones y correo Firebase

Turnstile usa `POST /api/auth/captcha/verify` con `{ "captchaToken": "<token>" }`. El frontend debe crear el widget con la acción `login` para ese endpoint; los formularios públicos usan `contacto` y `newsletter`. El backend verifica Siteverify por POST `application/x-www-form-urlencoded`, exige `success`, hostname permitido y acción coincidente. En producción, CAPTCHA habilitado exige secret y hostnames explícitos; el backend rechaza las claves públicas de prueba conocidas de Turnstile. Errores de proveedor/configuración devuelven `503` con un `code` interno; token inválido/expirado devuelve `400`; el límite devuelve `429` y `Retry-After`.

Los hashes de tokens CAPTCHA usados, tickets de un solo uso y sesiones CAPTCHA verificadas se comparten mediante PostgreSQL (migración aditiva `V8`). Los tickets expiran según `APP_CAPTCHA_TICKET_TTL_SECONDS` (120 segundos por defecto) y se ligan a la IP que Spring obtiene del proxy confiable y al `User-Agent`. La IP se toma de `request.getRemoteAddr()` tanto para Siteverify como para rate limit y tickets. No se confía directamente en `X-Forwarded-For` ni en `CF-Connecting-IP`: limita el acceso al origen al proxy, configura `APP_TRUSTED_PROXIES` con sus rangos controlados y elige `APP_REMOTE_IP_HEADER`. En Cloudflare se puede usar `CF-Connecting-IP` solo si el origen no acepta conexiones directas y Cloudflare sobrescribe ese encabezado. Mantén actualizados los rangos del proxy según tu proveedor; no uses `.*`.

El restablecimiento conserva `POST /api/auth/password-reset` con `{ "email": "..." }`. Firebase Admin SDK crea el enlace oficial mediante `generatePasswordResetLink()` y el servidor lo envía con la plantilla HTML corporativa. La respuesta aceptada es igual para correo no autorizado/no existente y para errores por cuenta o entrega, que se registran sin dirección, enlace ni credenciales; la falta global de configuración devuelve `503`. No se envían contraseñas ni se inventan códigos o URLs de acción.

`POST /api/auth/email-verification` no acepta correo ni otro body: requiere autenticación Firebase, pero no requiere ticket CAPTCHA ni sesión verificada, para que una cuenta aún no verificada pueda pedir el mensaje. El backend usa el UID autenticado para consultar la cuenta de Firebase, rechaza cuentas inactivas/deshabilitadas, no reenvía si ya está verificada y limita las solicitudes por IP y cuenta. Responde `202 { "status": "sent" }` o `200 { "status": "already_verified" }`. Firebase genera el enlace con `generateEmailVerificationLink()`; los enlaces mantienen el handler oficial de Firebase.

Para ambos correos configura `APP_MAIL_FROM`, `SMTP_HOST`, `SMTP_PORT`, `SMTP_USERNAME` y `SMTP_PASSWORD`. `SMTP_AUTH`, `SMTP_STARTTLS_ENABLE`, `SMTP_STARTTLS_REQUIRED` y los timeouts permiten ajustar el relay; no guardes credenciales SMTP en el repositorio. La cuenta debe actualizar su token de Firebase tras completar la verificación para que los claims y el rol reflejen el nuevo estado.

---

## Ejecución de Pruebas Unitarias y de Integración

Los tests de integración del controlador utilizan una configuración especial con base de datos H2 en memoria (`src/test/resources/application-test.properties`). No requieren que tengas PostgreSQL corriendo para compilar o probar.

Para ejecutar los tests, corre:
```powershell
.\mvnw.cmd test
```
